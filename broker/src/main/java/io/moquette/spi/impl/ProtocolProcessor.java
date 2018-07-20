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

package io.moquette.spi.impl;

import io.moquette.connections.IConnectionsManager;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptAcknowledgedMessage;
import io.moquette.server.ConnectionDescriptor;
import io.moquette.server.ConnectionDescriptorStore;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.ClientSession;
import io.moquette.spi.EnqueuedMessage;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.IMessagesStore.StoredMessage;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.subscriptions.ISubscriptionsDirectory;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizator;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static io.moquette.server.ConnectionDescriptor.ConnectionState.*;
import static io.moquette.spi.impl.InternalRepublisher.createPublishForQos;
import static io.moquette.spi.impl.Utils.messageId;
import static io.moquette.spi.impl.Utils.readBytesAndRewind;
import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;
import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;
import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.*;

/**
 * Class responsible to handle the logic of MQTT protocol it's the director of the protocol
 * execution.
 *
 * Used by the front facing class ProtocolProcessorBootstrapper.
 */
public class ProtocolProcessor {

    private enum SubscriptionState {
        STORED, VERIFIED
    }

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolProcessor.class);

    private IConnectionsManager connectionDescriptors;
    private ConcurrentMap<RunningSubscription, SubscriptionState> subscriptionInCourse;

    private ISubscriptionsDirectory subscriptions;
    private IAuthorizator m_authorizator;

    private IMessagesStore m_messagesStore;

    private ISessionsStore m_sessionsStore;

    private BrokerInterceptor m_interceptor;

    private Qos0PublishHandler qos0PublishHandler;
    private Qos1PublishHandler qos1PublishHandler;
    private Qos2PublishHandler qos2PublishHandler;
    private MessagesPublisher messagesPublisher;
    private InternalRepublisher internalRepublisher;
    SessionsRepository sessionsRepository;

    private ConnectHandler connectHandler;
    private DisconnectHandler disconnectHandler;

    ProtocolProcessor() {
    }

    public void init(ISubscriptionsDirectory subscriptions, IMessagesStore storageService, ISessionsStore sessionsStore,
                     IAuthenticator authenticator, boolean allowAnonymous, IAuthorizator authorizator,
                     BrokerInterceptor interceptor, SessionsRepository sessionsRepository,
                     boolean reauthorizeSubscriptionsOnConnect) {
        init(subscriptions, storageService, sessionsStore, authenticator, allowAnonymous, false,
             authorizator, interceptor, sessionsRepository, reauthorizeSubscriptionsOnConnect);
    }

    public void init(ISubscriptionsDirectory subscriptions, IMessagesStore storageService, ISessionsStore sessionsStore,
                     IAuthenticator authenticator, boolean allowAnonymous, boolean allowZeroByteClientId,
                     IAuthorizator authorizator, BrokerInterceptor interceptor, SessionsRepository sessionsRepository,
                     boolean reauthorizeSubscriptionsOnConnect) {
        init(new ConnectionDescriptorStore(), subscriptions, storageService, sessionsStore,
             authenticator, allowAnonymous, allowZeroByteClientId, authorizator, interceptor, sessionsRepository,
             reauthorizeSubscriptionsOnConnect);
    }

    /**
     * @param subscriptions
     *            the subscription store where are stored all the existing clients subscriptions.
     * @param storageService
     *            the persistent store to use for save/load of messages for QoS1 and QoS2 handling.
     * @param sessionsStore
     *            the clients sessions store, used to persist subscriptions.
     * @param authenticator
     *            the authenticator used in connect messages.
     * @param allowAnonymous
     *            true connection to clients without credentials.
     * @param allowZeroByteClientId
     *            true to allow clients connect without a clientid
     * @param authorizator
     *            used to apply ACL policies to publishes and subscriptions.
     * @param interceptor
     *            to notify events to an intercept handler
     */
    void init(IConnectionsManager connectionDescriptors, ISubscriptionsDirectory subscriptions,
              IMessagesStore storageService, ISessionsStore sessionsStore, IAuthenticator authenticator,
              boolean allowAnonymous, boolean allowZeroByteClientId, IAuthorizator authorizator,
              BrokerInterceptor interceptor, SessionsRepository sessionsRepository,
              boolean reauthorizeSubscriptionsOnConnect) {
        LOG.info("Initializing MQTT protocol processor..");
        this.connectionDescriptors = connectionDescriptors;
        this.subscriptionInCourse = new ConcurrentHashMap<>();
        this.m_interceptor = interceptor;
        this.subscriptions = subscriptions;
        m_authorizator = authorizator;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initial subscriptions tree={}", subscriptions.dumpTree());
        }
        m_messagesStore = storageService;
        m_sessionsStore = sessionsStore;

        this.sessionsRepository = sessionsRepository;

        LOG.debug("Initializing messages publisher...");
        final PersistentQueueMessageSender messageSender = new PersistentQueueMessageSender(this.connectionDescriptors);
        this.messagesPublisher = new MessagesPublisher(connectionDescriptors, messageSender,
            subscriptions, this.sessionsRepository);

        LOG.debug("Initializing QoS publish handlers...");
        this.qos0PublishHandler = new Qos0PublishHandler(m_authorizator, m_messagesStore, m_interceptor,
                this.messagesPublisher);
        this.qos1PublishHandler = new Qos1PublishHandler(m_authorizator, m_messagesStore, m_interceptor,
                this.connectionDescriptors, this.messagesPublisher);
        this.qos2PublishHandler = new Qos2PublishHandler(m_authorizator, subscriptions, m_messagesStore, m_interceptor,
                this.connectionDescriptors, this.messagesPublisher, this.sessionsRepository);

        LOG.debug("Initializing internal republisher...");
        this.internalRepublisher = new InternalRepublisher(messageSender);

        connectHandler = new ConnectHandler(connectionDescriptors, m_interceptor, sessionsRepository, m_sessionsStore,
            authenticator, m_authorizator, subscriptions,
            allowAnonymous, allowZeroByteClientId, reauthorizeSubscriptionsOnConnect, internalRepublisher);
        disconnectHandler = new DisconnectHandler(connectionDescriptors, sessionsRepository, subscriptions, m_interceptor);
        LOG.info("Initialized");
    }

    public void processConnect(Channel channel, MqttConnectMessage connectMessage) {
        this.connectHandler.processConnect(channel, connectMessage);
    }

    public void processPubAck(Channel channel, MqttPubAckMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        int messageID = msg.variableHeader().messageId();
        String username = NettyUtils.userName(channel);
        LOG.trace("retrieving inflight for messageID <{}>", messageID);

        ClientSession targetSession = this.sessionsRepository.sessionForClient(clientID);
        StoredMessage inflightMsg = targetSession.inFlightAcknowledged(messageID);

        String topic = inflightMsg.getTopic();
        InterceptAcknowledgedMessage wrapped = new InterceptAcknowledgedMessage(inflightMsg, topic, username,
                                                                                messageID);
        m_interceptor.notifyMessageAcknowledged(wrapped);
    }

    public static IMessagesStore.StoredMessage asStoredMessage(MqttPublishMessage msg) {
        // TODO ugly, too much array copy
        ByteBuf payload = msg.payload();
        byte[] payloadContent = readBytesAndRewind(payload);

        IMessagesStore.StoredMessage stored = new IMessagesStore.StoredMessage(payloadContent,
                msg.fixedHeader().qosLevel(), msg.variableHeader().topicName());
        stored.setRetained(msg.fixedHeader().isRetain());
        return stored;
    }

    private static IMessagesStore.StoredMessage asStoredMessage(WillMessage will) {
        IMessagesStore.StoredMessage pub = new IMessagesStore.StoredMessage(will.getPayload().array(), will.getQos(),
                will.getTopic());
        pub.setRetained(will.isRetained());
        return pub;
    }

    public void processPublish(Channel channel, MqttPublishMessage msg) {
        final MqttQoS qos = msg.fixedHeader().qosLevel();
        final String clientId = NettyUtils.clientID(channel);
        LOG.info("Processing PUBLISH message. CId={}, topic={}, messageId={}, qos={}", clientId,
                 msg.variableHeader().topicName(), msg.variableHeader().messageId(), qos);
        switch (qos) {
            case AT_MOST_ONCE:
                this.qos0PublishHandler.receivedPublishQos0(channel, msg);
                break;
            case AT_LEAST_ONCE:
                this.qos1PublishHandler.receivedPublishQos1(channel, msg);
                break;
            case EXACTLY_ONCE:
                this.qos2PublishHandler.receivedPublishQos2(channel, msg);
                break;
            default:
                LOG.error("Unknown QoS-Type:{}", qos);
                break;
        }
    }

    /**
     * Intended usage is only for embedded versions of the broker, where the hosting application
     * want to use the broker to send a publish message. Inspired by {@link #processPublish} but
     * with some changes to avoid security check, and the handshake phases for Qos1 and Qos2. It
     * also doesn't notifyTopicPublished because using internally the owner should already know
     * where it's publishing.
     *
     * @param msg
     *            the message to publish.
     * @param clientId
     *            the clientID
     */
    public void internalPublish(MqttPublishMessage msg, final String clientId) {
        final MqttQoS qos = msg.fixedHeader().qosLevel();
        final Topic topic = new Topic(msg.variableHeader().topicName());
        LOG.info("Sending PUBLISH message. Topic={}, qos={}", topic, qos);

        IMessagesStore.StoredMessage toStoreMsg = asStoredMessage(msg);
        if (clientId == null || clientId.isEmpty()) {
            toStoreMsg.setClientID("BROKER_SELF");
        } else {
            toStoreMsg.setClientID(clientId);
        }
        this.messagesPublisher.publish2Subscribers(toStoreMsg, topic);

        if (!msg.fixedHeader().isRetain()) {
            return;
        }
        if (qos == AT_MOST_ONCE || msg.payload().readableBytes() == 0) {
            // QoS == 0 && retain => clean old retained
            m_messagesStore.cleanRetained(topic);
            return;
        }
        m_messagesStore.storeRetained(topic, toStoreMsg);
    }

    /**
     * Specialized version to publish will testament message.
     */
    private void forwardPublishWill(WillMessage will, String clientID) {
        LOG.info("Publishing will message. CId={}, topic={}", clientID, will.getTopic());
        // it has just to publish the message downstream to the subscribers
        // NB it's a will publish, it needs a PacketIdentifier for this conn, default to 1
        IMessagesStore.StoredMessage tobeStored = asStoredMessage(will);
        tobeStored.setClientID(clientID);
        Topic topic = new Topic(tobeStored.getTopic());
        this.messagesPublisher.publish2Subscribers(tobeStored, topic);

        //Stores retained message to the topic
        if (will.isRetained()) {
            m_messagesStore.storeRetained(topic, tobeStored);
        }
     }

    static MqttQoS lowerQosToTheSubscriptionDesired(Subscription sub, MqttQoS qos) {
        if (qos.value() > sub.getRequestedQos().value()) {
            qos = sub.getRequestedQos();
        }
        return qos;
    }

    /**
     * Second phase of a publish QoS2 protocol, sent by publisher to the broker. Search the stored
     * message and publish to all interested subscribers.
     *
     * @param channel
     *            the channel of the incoming message.
     * @param msg
     *            the decoded pubrel message.
     */
    public void processPubRel(Channel channel, MqttMessage msg) {
        this.qos2PublishHandler.processPubRel(channel, msg);
    }

    public void processPubRec(Channel channel, MqttMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        int messageID = messageId(msg);
        LOG.debug("Processing PUBREC message. CId={}, messageId={}", clientID, messageID);
        ClientSession targetSession = this.sessionsRepository.sessionForClient(clientID);
        // remove from the inflight and move to the QoS2 second phase queue
        StoredMessage ackedMsg = targetSession.inFlightAcknowledged(messageID);
        targetSession.moveInFlightToSecondPhaseAckWaiting(messageID, ackedMsg);
        // once received a PUBREC reply with a PUBREL(messageID)
        LOG.debug("Processing PUBREC message. CId={}, messageId={}", clientID, messageID);

        MqttFixedHeader pubRelHeader = new MqttFixedHeader(MqttMessageType.PUBREL, false, AT_LEAST_ONCE, false, 0);
        MqttMessage pubRelMessage = new MqttMessage(pubRelHeader, from(messageID));
        channel.writeAndFlush(pubRelMessage).addListener(FIRE_EXCEPTION_ON_FAILURE);
    }

    public void processPubComp(Channel channel, MqttMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        int messageID = messageId(msg);
        LOG.debug("Processing PUBCOMP message. CId={}, messageId={}", clientID, messageID);
        // once received the PUBCOMP then remove the message from the temp memory
        ClientSession targetSession = this.sessionsRepository.sessionForClient(clientID);
        StoredMessage inflightMsg = targetSession.completeReleasedPublish(messageID);
        String username = NettyUtils.userName(channel);
        String topic = inflightMsg.getTopic();
        final InterceptAcknowledgedMessage interceptAckMsg = new InterceptAcknowledgedMessage(inflightMsg, topic,
            username, messageID);
        m_interceptor.notifyMessageAcknowledged(interceptAckMsg);
    }

    public void processDisconnect(Channel channel) {
        final String clientID = NettyUtils.clientID(channel);
        LOG.debug("Processing DISCONNECT message. CId={}", clientID);
        channel.flush();
        disconnectHandler.processDisconnect(channel, clientID);
    }

    public void processConnectionLost(String clientID, Channel channel) {
        LOG.info("Lost connection with client <{}>", clientID);
        ConnectionDescriptor oldConnDescr = new ConnectionDescriptor(clientID, channel, true);
        connectionDescriptors.removeConnection(oldConnDescr);
        // publish the Will message (if any) for the clientID
        final ClientSession clientSession = this.sessionsRepository.sessionForClient(clientID);
        WillMessage will = clientSession.willMessage();
        if (will != null) {
            forwardPublishWill(will, clientID);
            clientSession.removeWill();
        }

        String username = NettyUtils.userName(channel);
        m_interceptor.notifyClientConnectionLost(clientID, username);
    }

    /**
     * Remove the clientID from topic subscription, if not previously subscribed, doesn't reply any
     * error.
     *
     * @param channel
     *            the channel of the incoming message.
     * @param msg
     *            the decoded unsubscribe message.
     */
    public void processUnsubscribe(Channel channel, MqttUnsubscribeMessage msg) {
        List<String> topics = msg.payload().topics();
        String clientID = NettyUtils.clientID(channel);

        LOG.debug("Processing UNSUBSCRIBE message. CId={}, topics={}", clientID, topics);

        ClientSession clientSession = this.sessionsRepository.sessionForClient(clientID);
        for (String t : topics) {
            Topic topic = new Topic(t);
            boolean validTopic = topic.isValid();
            if (!validTopic) {
                // close the connection, not valid topicFilter is a protocol violation
                channel.close().addListener(CLOSE_ON_FAILURE);
                LOG.error("Topic filter is not valid. CId={}, topics={}, badTopicFilter={}", clientID, topics, topic);
                return;
            }

            LOG.trace("Removing subscription. CId={}, topic={}", clientID, topic);
            subscriptions.removeSubscription(topic, clientID);
            clientSession.unsubscribeFrom(topic);
            String username = NettyUtils.userName(channel);
            m_interceptor.notifyTopicUnsubscribed(topic.toString(), clientID, username);
        }

        // ack the client
        int messageID = msg.variableHeader().messageId();
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBACK, false, AT_MOST_ONCE,
                                                         false, 0);
        MqttUnsubAckMessage ackMessage = new MqttUnsubAckMessage(fixedHeader, from(messageID));

        LOG.debug("Sending UNSUBACK message. CId={}, topics={}, messageId={}", clientID, topics, messageID);
        channel.writeAndFlush(ackMessage).addListener(FIRE_EXCEPTION_ON_FAILURE);
        LOG.info("Client <{}> unsubscribed from topics <{}>", clientID, topics);
    }

    public void processSubscribe(Channel channel, MqttSubscribeMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        int messageID = messageId(msg);
        LOG.debug("Processing SUBSCRIBE message. CId={}, messageId={}", clientID, messageID);

        RunningSubscription executionKey = new RunningSubscription(clientID, messageID);
        SubscriptionState currentStatus = subscriptionInCourse.putIfAbsent(executionKey, SubscriptionState.VERIFIED);
        if (currentStatus != null) {
            LOG.warn("Client sent another SUBSCRIBE message while this one was being processed CId={}, messageId={}",
                clientID, messageID);
            return;
        }
        String username = NettyUtils.userName(channel);
        List<MqttTopicSubscription> ackTopics = doVerify(clientID, username, msg);
        MqttSubAckMessage ackMessage = doAckMessageFromValidateFilters(ackTopics, messageID);
        if (!this.subscriptionInCourse.replace(executionKey, SubscriptionState.VERIFIED, SubscriptionState.STORED)) {
            LOG.warn("Client sent another SUBSCRIBE message while the topic filters were being verified CId={}, " +
                "messageId={}", clientID, messageID);
            return;
        }

        LOG.debug("Creating and storing subscriptions CId={}, messageId={}, topics={}", clientID, messageID, ackTopics);

        List<Subscription> newSubscriptions = doStoreSubscription(ackTopics, clientID);

        // save session, persist subscriptions from session
        for (Subscription subscription : newSubscriptions) {
            subscriptions.add(subscription);
        }

        LOG.debug("Sending SUBACK response CId={}, messageId={}", clientID, messageID);
        channel.writeAndFlush(ackMessage).addListener(FIRE_EXCEPTION_ON_FAILURE);

        // fire the persisted messages in session
        for (Subscription subscription : newSubscriptions) {
            publishRetainedMessagesInSession(subscription, username);
        }

        boolean success = this.subscriptionInCourse.remove(executionKey, SubscriptionState.STORED);
        if (!success) {
            LOG.warn("Unable to perform the final subscription state update CId={}, messageId={}", clientID, messageID);
        } else {
            LOG.info("Client <{}> subscribed to topics", clientID);
        }
    }

    private List<Subscription> doStoreSubscription(List<MqttTopicSubscription> ackTopics, String clientID) {
        ClientSession clientSession = this.sessionsRepository.sessionForClient(clientID);

        List<Subscription> newSubscriptions = new ArrayList<>();
        for (MqttTopicSubscription req : ackTopics) {
            // TODO this is SUPER UGLY
            if (req.qualityOfService() == FAILURE) {
                continue;
            }
            final Topic topic = new Topic(req.topicName());
            Subscription newSubscription = new Subscription(clientID, topic, req.qualityOfService());

            clientSession.subscribe(newSubscription);
            newSubscriptions.add(newSubscription);
        }
        return newSubscriptions;
    }

    /**
     * @param clientID
     *            the clientID
     * @param username
     *            the username
     * @param msg
     *            the subscribe message to verify
     * @return the list of verified topics for the given subscribe message.
     */
    private List<MqttTopicSubscription> doVerify(String clientID, String username, MqttSubscribeMessage msg) {
        ClientSession clientSession = this.sessionsRepository.sessionForClient(clientID);
        List<MqttTopicSubscription> ackTopics = new ArrayList<>();

        final int messageId = messageId(msg);
        for (MqttTopicSubscription req : msg.payload().topicSubscriptions()) {
            Topic topic = new Topic(req.topicName());
            if (!m_authorizator.canRead(topic, username, clientSession.clientID)) {
                // send SUBACK with 0x80, the user hasn't credentials to read the topic
                LOG.warn("Client does not have read permissions on the topic CId={}, username={}, messageId={}, " +
                    "topic={}", clientID, username, messageId, topic);
                ackTopics.add(new MqttTopicSubscription(topic.toString(), FAILURE));
            } else {
                MqttQoS qos;
                if (topic.isValid()) {
                    LOG.debug("Client will be subscribed to the topic CId={}, username={}, messageId={}, topic={}",
                        clientID, username, messageId, topic);
                    qos = req.qualityOfService();
                } else {
                    LOG.warn("Topic filter is not valid CId={}, username={}, messageId={}, topic={}", clientID,
                        username, messageId, topic);
                    qos = FAILURE;
                }
                ackTopics.add(new MqttTopicSubscription(topic.toString(), qos));
            }
        }
        return ackTopics;
    }

    /**
     * Create the SUBACK response from a list of topicFilters
     */
    private MqttSubAckMessage doAckMessageFromValidateFilters(List<MqttTopicSubscription> topicFilters, int messageId) {
        List<Integer> grantedQoSLevels = new ArrayList<>();
        for (MqttTopicSubscription req : topicFilters) {
            grantedQoSLevels.add(req.qualityOfService().value());
        }

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, AT_MOST_ONCE, false, 0);
        MqttSubAckPayload payload = new MqttSubAckPayload(grantedQoSLevels);
        return new MqttSubAckMessage(fixedHeader, from(messageId), payload);
    }

    private void publishRetainedMessagesInSession(Subscription newSubscription, String username) {
        LOG.debug("Retrieving retained messages CId={}, topics={}", newSubscription.getClientId(),
                newSubscription.getTopicFilter());

        // scans retained messages to be published to the new subscription
        // TODO this is ugly, it does a linear scan on potential big dataset
        Collection<IMessagesStore.StoredMessage> messages = m_messagesStore
                .searchMatching(key -> key.match(newSubscription.getTopicFilter()));

        if (!messages.isEmpty()) {
            LOG.info("Publishing retained messages CId={}, topics={}, messagesNo={}",
                newSubscription.getClientId(), newSubscription.getTopicFilter(), messages.size());
        }
        ClientSession targetSession = this.sessionsRepository.sessionForClient(newSubscription.getClientId());

        // adapt the message QoS to subscriber's accepted accepted
        for (IMessagesStore.StoredMessage msg : messages) {
            int lowestQoS = Math.min(msg.getQos().value(), newSubscription.getRequestedQos().value());
            MqttQoS qosToPublish = MqttQoS.valueOf(lowestQoS);
            msg.setQos(qosToPublish);
        }

        this.internalRepublisher.publishRetained(targetSession, messages);

        // notify the Observables
        m_interceptor.notifyTopicSubscribed(newSubscription, username);
    }

    public void notifyChannelWritable(Channel channel) {
        String clientID = NettyUtils.clientID(channel);
        ClientSession clientSession = this.sessionsRepository.sessionForClient(clientID);
        boolean emptyQueue = false;
        while (channel.isWritable() && !emptyQueue) {
            EnqueuedMessage msg = clientSession.poll();
            if (msg == null) {
                emptyQueue = true;
            } else {
                // recreate a publish from stored publish in queue
                MqttPublishMessage pubMsg = createPublishForQos(msg.msg.getTopic(), msg.msg.getQos(),
                                                                msg.msg.getPayload(), msg.msg.isRetained(),
                                                                msg.messageId);
                channel.write(pubMsg).addListener(FIRE_EXCEPTION_ON_FAILURE);
            }
        }
        channel.flush();
    }

    public void addInterceptHandler(InterceptHandler interceptHandler) {
        this.m_interceptor.addInterceptHandler(interceptHandler);
    }

    public void removeInterceptHandler(InterceptHandler interceptHandler) {
        this.m_interceptor.removeInterceptHandler(interceptHandler);
    }

    public IMessagesStore getMessagesStore() {
        return m_messagesStore;
    }

    public ISessionsStore getSessionsStore() {
        return m_sessionsStore;
    }

    public void shutdown() {
        if (m_interceptor != null)
            m_interceptor.stop();
    }
}
