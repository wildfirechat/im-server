/*
 * Copyright (c) 2012-2018 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package io.moquette.broker;

import io.moquette.broker.Session.SessionStatus;
import io.moquette.broker.subscriptions.ISubscriptionsDirectory;
import io.moquette.broker.subscriptions.Subscription;
import io.moquette.broker.subscriptions.Topic;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class SessionRegistry {

    public abstract static class EnqueuedMessage {
    }

    static class PublishedMessage extends EnqueuedMessage {

        final Topic topic;
        final MqttQoS publishingQos;
        final ByteBuf payload;

        PublishedMessage(Topic topic, MqttQoS publishingQos, ByteBuf payload) {
            this.topic = topic;
            this.publishingQos = publishingQos;
            this.payload = payload;
        }
    }

    static final class PubRelMarker extends EnqueuedMessage {
    }

    private enum PostConnectAction {
        NONE, SEND_STORED_MESSAGES
    }

    private static final Logger LOG = LoggerFactory.getLogger(SessionRegistry.class);

    private final ConcurrentMap<String, Session> pool = new ConcurrentHashMap<>();
    private final ISubscriptionsDirectory subscriptionsDirectory;
    private final IQueueRepository queueRepository;
    private final Authorizator authorizator;
    private final ConcurrentMap<String, Queue<SessionRegistry.EnqueuedMessage>> queues = new ConcurrentHashMap<>();

    SessionRegistry(ISubscriptionsDirectory subscriptionsDirectory,
                    IQueueRepository queueRepository,
                    Authorizator authorizator) {
        this.subscriptionsDirectory = subscriptionsDirectory;
        this.queueRepository = queueRepository;
        this.authorizator = authorizator;
    }

    void bindToSession(MQTTConnection mqttConnection, MqttConnectMessage msg, String clientId) {
        boolean isSessionAlreadyStored = false;
        PostConnectAction postConnectAction = PostConnectAction.NONE;
        if (!pool.containsKey(clientId)) {
            // case 1
            final Session newSession = createNewSession(mqttConnection, msg, clientId);

            // publish the session
            final Session previous = pool.putIfAbsent(clientId, newSession);
            final boolean success = previous == null;

            if (success) {
                LOG.trace("case 1, not existing session with CId {}", clientId);
            } else {
                postConnectAction = bindToExistingSession(mqttConnection, msg, clientId, newSession);
                isSessionAlreadyStored = true;
            }
        } else {
            final Session newSession = createNewSession(mqttConnection, msg, clientId);
            postConnectAction = bindToExistingSession(mqttConnection, msg, clientId, newSession);
            isSessionAlreadyStored = true;
        }
        final boolean msgCleanSessionFlag = msg.variableHeader().isCleanSession();
        boolean isSessionAlreadyPresent = !msgCleanSessionFlag && isSessionAlreadyStored;
        mqttConnection.sendConnAck(isSessionAlreadyPresent);

        if (postConnectAction == PostConnectAction.SEND_STORED_MESSAGES) {
            final Session session = pool.get(clientId);
            session.sendQueuedMessagesWhileOffline();
        }
    }

    private PostConnectAction bindToExistingSession(MQTTConnection mqttConnection, MqttConnectMessage msg,
                                                    String clientId, Session newSession) {
        PostConnectAction postConnectAction = PostConnectAction.NONE;
        final boolean newIsClean = msg.variableHeader().isCleanSession();
        final Session oldSession = pool.get(clientId);
        if (newIsClean && oldSession.disconnected()) {
            // case 2
            dropQueuesForClient(clientId);
            unsubscribe(oldSession);

            // publish new session
            boolean result = oldSession.assignState(SessionStatus.DISCONNECTED, SessionStatus.CONNECTING);
            if (!result) {
                throw new SessionCorruptedException("old session was already changed state");
            }
            copySessionConfig(msg, oldSession);
            oldSession.bind(mqttConnection);

            result = oldSession.assignState(SessionStatus.CONNECTING, SessionStatus.CONNECTED);
            if (!result) {
                throw new SessionCorruptedException("old session moved in connected state by other thread");
            }
            final boolean published = pool.replace(clientId, oldSession, oldSession);
            if (!published) {
                throw new SessionCorruptedException("old session was already removed");
            }
            LOG.trace("case 2, oldSession with same CId {} disconnected", clientId);
        } else if (!newIsClean && oldSession.disconnected()) {
            // case 3
            final String username = mqttConnection.getUsername();
            reactivateSubscriptions(oldSession, username);

            // mark as connected
            final boolean connecting = oldSession.assignState(SessionStatus.DISCONNECTED, SessionStatus.CONNECTING);
            if (!connecting) {
                throw new SessionCorruptedException("old session moved in connected state by other thread");
            }
            oldSession.bind(mqttConnection);

            final boolean connected = oldSession.assignState(SessionStatus.CONNECTING, SessionStatus.CONNECTED);
            if (!connected) {
                throw new SessionCorruptedException("old session moved in other state state by other thread");
            }

            // publish new session
            final boolean published = pool.replace(clientId, oldSession, oldSession);
            if (!published) {
                throw new SessionCorruptedException("old session was already removed");
            }
            postConnectAction = PostConnectAction.SEND_STORED_MESSAGES;
            LOG.trace("case 3, oldSession with same CId {} disconnected", clientId);
        } else if (oldSession.connected()) {
            // case 4
            LOG.trace("case 4, oldSession with same CId {} still connected, force to close", clientId);
            oldSession.closeImmediately();
            //remove(clientId);
            // publish new session
            final boolean published = pool.replace(clientId, oldSession, newSession);
            if (!published) {
                throw new SessionCorruptedException("old session was already removed");
            }
        }
        // case not covered new session is clean true/false and old session not in CONNECTED/DISCONNECTED
        return postConnectAction;
    }

    private void reactivateSubscriptions(Session session, String username) {
        //verify if subscription still satisfy read ACL permissions
        for (Subscription existingSub : session.getSubscriptions()) {
            final boolean topicReadable = authorizator.canRead(existingSub.getTopicFilter(), username,
                                                               session.getClientID());
            if (!topicReadable) {
                subscriptionsDirectory.removeSubscription(existingSub.getTopicFilter(), session.getClientID());
            }
            // TODO
//            subscriptionsDirectory.reactivate(existingSub.getTopicFilter(), session.getClientID());
        }
    }

    private void unsubscribe(Session session) {
        for (Subscription existingSub : session.getSubscriptions()) {
            subscriptionsDirectory.removeSubscription(existingSub.getTopicFilter(), session.getClientID());
        }
    }

    private Session createNewSession(MQTTConnection mqttConnection, MqttConnectMessage msg, String clientId) {
        final boolean clean = msg.variableHeader().isCleanSession();
        final Queue<SessionRegistry.EnqueuedMessage> sessionQueue =
                    queues.computeIfAbsent(clientId, (String cli) -> queueRepository.createQueue(cli, clean));
        final Session newSession;
        if (msg.variableHeader().isWillFlag()) {
            final Session.Will will = createWill(msg);
            newSession = new Session(clientId, clean, will, sessionQueue);
        } else {
            newSession = new Session(clientId, clean, sessionQueue);
        }

        newSession.markConnected();
        newSession.bind(mqttConnection);

        return newSession;
    }

    private void copySessionConfig(MqttConnectMessage msg, Session session) {
        final boolean clean = msg.variableHeader().isCleanSession();
        final Session.Will will;
        if (msg.variableHeader().isWillFlag()) {
            will = createWill(msg);
        } else {
            will = null;
        }
        session.update(clean, will);
    }

    private Session.Will createWill(MqttConnectMessage msg) {
        final ByteBuf willPayload = Unpooled.copiedBuffer(msg.payload().willMessageInBytes());
        final String willTopic = msg.payload().willTopic();
        final boolean retained = msg.variableHeader().isWillRetain();
        final MqttQoS qos = MqttQoS.valueOf(msg.variableHeader().willQos());
        return new Session.Will(willTopic, willPayload, qos, retained);
    }

    Session retrieve(String clientID) {
        return pool.get(clientID);
    }

    public void remove(String clientID) {
        pool.remove(clientID);
    }

    public void disconnect(String clientID) {
        final Session session = retrieve(clientID);
        if (session == null) {
            LOG.debug("Some other thread already removed the session CId={}", clientID);
            return;
        }
        session.disconnect();
    }

    private void dropQueuesForClient(String clientId) {
        queues.remove(clientId);
    }

    Collection<ClientDescriptor> listConnectedClients() {
        return pool.values().stream()
            .filter(Session::connected)
            .map(this::createClientDescriptor)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    private Optional<ClientDescriptor> createClientDescriptor(Session s) {
        final String clientID = s.getClientID();
        final Optional<InetSocketAddress> remoteAddressOpt = s.remoteAddress();
        return remoteAddressOpt.map(r -> new ClientDescriptor(clientID, r.getHostString(), r.getPort()));
    }
}
