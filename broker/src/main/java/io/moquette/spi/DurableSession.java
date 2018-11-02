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
package io.moquette.spi;

import io.moquette.spi.impl.WillMessage;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

@Deprecated
public class DurableSession extends ClientSession {

    class OutboundFlightZone {

        /**
         * Save the binding messageID, clientID - message
         *
         * @param messageID the packet ID used in transmission
         * @param msg the message to put in flight zone
         */
        void waitingAck(int messageID, IMessagesStore.StoredMessage msg) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Adding to inflight {}, guid <{}>", messageID, msg.getGuid());
            }
            sessionsStore.inFlight(clientID, messageID, msg);
        }

        IMessagesStore.StoredMessage acknowledged(int messageID) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Acknowledging inflight, clientID <{}> messageID {}", clientID, messageID);
            }
            return sessionsStore.inFlightAck(clientID, messageID);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(DurableSession.class);

    private final ISessionsStore sessionsStore;
    private final ISubscriptionsStore subscriptionsStore;

    private final OutboundFlightZone outboundFlightZone;
    private final InboundFlightZone inboundFlightZone;
    private final ConcurrentMap<String, WillMessage> willMessages;

    public DurableSession(String clientID, ISessionsStore sessions, ISubscriptionsStore subscriptionsStore,
                          ConcurrentMap<String, WillMessage> willMessages) {
        super(clientID);
        this.subscriptionsStore = subscriptionsStore;
        this.sessionsStore = sessions;
        this.willMessages = willMessages;
        this.outboundFlightZone = new OutboundFlightZone();
        this.inboundFlightZone = new InboundFlightZone();
    }

    public void reloadAllSubscriptionsFromStore() {
        Collection<Subscription> reloadedSubscriptions = this.subscriptionsStore.listClientSubscriptions(this.clientID);
        this.subscriptions.addAll(reloadedSubscriptions);
    }

    @Override
    public boolean isCleanSession() {
        return false;
    }

    @Override
    public boolean subscribe(Subscription newSubscription) {
        LOG.info("Adding new subscription. CId={}, topics={}, qos={}", newSubscription.getClientId(),
            newSubscription.getTopicFilter(), newSubscription.getRequestedQos());
        boolean validTopic = newSubscription.getTopicFilter().isValid();
        if (!validTopic) {
            LOG.warn("The topic filter is not valid. CId={}, topics={}", newSubscription.getClientId(),
                newSubscription.getTopicFilter());
            // send SUBACK with 0x80 for this topic filter
            return false;
        }
        Subscription existingSub = subscriptionsStore.reload(newSubscription);
        // update the selected subscriptions if not present or if has a greater qos
        if (existingSub == null || existingSub.qosLessThan(newSubscription)) {
            if (existingSub != null) {
                LOG.info("Subscription already existed with a lower QoS value. It will be updated. CId={}, " +
                        "topics={}, existingQos={}, newQos={}", newSubscription.getClientId(),
                    newSubscription.getTopicFilter(), existingSub.getRequestedQos(), newSubscription.getRequestedQos());
                subscriptions.remove(newSubscription);
            }
            subscriptions.add(newSubscription);
            subscriptionsStore.addNewSubscription(newSubscription);
        }
        return true;
    }

    @Override
    public void unsubscribeFrom(Topic topicFilter) {
        LOG.info("Removing subscription. CId={}, topics={}", clientID, topicFilter);
        subscriptionsStore.removeSubscription(topicFilter, clientID);
        Set<Subscription> subscriptionsToRemove = new HashSet<>();
        for (Subscription sub : this.subscriptions) {
            if (sub.getTopicFilter().equals(topicFilter)) {
                subscriptionsToRemove.add(sub);
            }
        }
        subscriptions.removeAll(subscriptionsToRemove);
    }

    @Override
    public void cleanSession() {
        sessionsStore.removeTemporaryQoS2(this.clientID);
        LOG.info("Wiping existing subscriptions. ClientId={}", clientID);
        subscriptionsStore.wipeSubscriptions(clientID);

        LOG.info("Removing queues. ClientId={}", clientID);
        sessionsStore.dropQueue(clientID);
    }

    @Override
    public void disconnect() {
        LOG.info("Client disconnected. Removing its subscriptions. CId={}", clientID);
        // cleanup topic subscriptions
        cleanSession();
    }

    @Override
    protected int nextPacketId() {
        return this.sessionsStore.nextPacketID(this.clientID);
    }

    @Override
    public IMessagesStore.StoredMessage inFlightAcknowledged(int messageID) {
        return outboundFlightZone.acknowledged(messageID);
    }

    @Override
    public int inFlightAckWaiting(IMessagesStore.StoredMessage msg) {
        LOG.debug("Adding message to inflight zone. CId={}", clientID);
        int messageId = this.nextPacketId();
        outboundFlightZone.waitingAck(messageId, msg);
        return messageId;
    }

    @Override
    public IMessagesStore.StoredMessage inboundInflight(int messageID) {
        return inboundFlightZone.lookup(messageID);
    }

    @Override
    public void markAsInboundInflight(int messageID, IMessagesStore.StoredMessage msg) {
        inboundFlightZone.waitingRel(messageID, msg);
    }

    @Override
    public void moveInFlightToSecondPhaseAckWaiting(int messageID, IMessagesStore.StoredMessage msg) {
        this.sessionsStore.moveInFlightToSecondPhaseAckWaiting(this.clientID, messageID, msg);
    }

    @Override
    public boolean isEmptyQueue() {
        return this.sessionsStore.queue(clientID).isEmpty();
    }

    @Override
    public void dropQueue() {
        LOG.debug("Removing messages of session. CId={}", this.clientID);
        this.sessionsStore.dropQueue(this.clientID);
        LOG.debug("Messages of the session have been removed. CId={}", this.clientID);
    }

    @Override
    public EnqueuedMessage poll() {
        IMessagesStore.StoredMessage msg = this.sessionsStore.queue(clientID).poll();
        if (msg == null) {
            return null;
        }
        int messageId = this.inFlightAckWaiting(msg);
        return new EnqueuedMessage(msg, messageId);
    }

    @Override
    public void enqueue(IMessagesStore.StoredMessage message) {
        this.sessionsStore.queue(this.clientID).add(message);
    }

    @Override
    public IMessagesStore.StoredMessage completeReleasedPublish(int messageID) {
        return this.sessionsStore.completeReleasedPublish(clientID, messageID);
    }

    @Override
    public int getPendingPublishMessagesNo() {
        return this.sessionsStore.queue(clientID).size();
    }

    @Override
    public int countPubReleaseWaitingPubComplete() {
        return this.sessionsStore.countPubReleaseWaitingPubComplete(clientID);
    }

    @Override
    public int getInflightMessagesNo() {
        return this.sessionsStore.getInflightMessagesNo(clientID);
    }

    @Override
    public void wipeSubscriptions() {
        this.subscriptionsStore.wipeSubscriptions(clientID);
    }

    @Override
    public void storeWillMessage(MqttConnectMessage msg, String clientId) {
        // Handle will flag
        if (msg.variableHeader().isWillFlag()) {
            MqttQoS willQos = MqttQoS.valueOf(msg.variableHeader().willQos());
            LOG.debug("Configuring MQTT last will and testament CId={}, willQos={}, willTopic={}, willRetain={}",
                clientId, willQos, msg.payload().willTopic(), msg.variableHeader().isWillRetain());
            byte[] willPayload = msg.payload().willMessage().getBytes(StandardCharsets.UTF_8);
            ByteBuffer bb = (ByteBuffer) ByteBuffer.allocate(willPayload.length).put(willPayload).flip();
            // save the will testament in the clientID store
            WillMessage will = new WillMessage(msg.payload().willTopic(), bb, msg.variableHeader().isWillRetain(), willQos);
            willMessages.put(clientId, will);
            LOG.debug("MQTT last will and testament has been configured. CId={}", clientId);
        }
    }

    @Override
    public void removeWill() {
        willMessages.remove(clientID);
    }

    @Override
    public WillMessage willMessage() {
        return willMessages.get(clientID);
    }
}
