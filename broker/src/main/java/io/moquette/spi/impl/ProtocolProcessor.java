/*
 * Copyright (c) 2012-2017 The original author or authors
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

import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptAcknowledgedMessage;
import io.moquette.server.ConnectionDescriptor;
import io.moquette.server.ConnectionDescriptorStore;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.*;
import io.moquette.spi.IMessagesStore.StoredMessage;
import io.moquette.spi.impl.subscriptions.ISubscriptionsDirectory;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizator;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static io.moquette.server.ConnectionDescriptor.ConnectionState.*;
import static io.moquette.spi.impl.InternalRepublisher.createPublishForQos;
import static io.moquette.spi.impl.Utils.messageId;
import static io.moquette.spi.impl.Utils.readBytesAndRewind;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.*;
import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.*;

/**
 * Class responsible to handle the logic of MQTT protocol it's the director of the protocol
 * execution.
 *
 * Used by the front facing class ProtocolProcessorBootstrapper.
 */
public class ProtocolProcessor {

    static final class WillMessage {

        private final String topic;
        private final ByteBuffer payload;
        private final boolean retained;
        private final MqttQoS qos;

        WillMessage(String topic, ByteBuffer payload, boolean retained, MqttQoS qos) {
            this.topic = topic;
            this.payload = payload;
            this.retained = retained;
            this.qos = qos;
        }

        public String getTopic() {
            return topic;
        }

        public ByteBuffer getPayload() {
            return payload;
        }

        public boolean isRetained() {
            return retained;
        }

        public MqttQoS getQos() {
            return qos;
        }
    }

    private enum SubscriptionState {
        STORED, VERIFIED
    }

    private class RunningSubscription {

        final String clientID;
        final long packetId;

        RunningSubscription(String clientID, long packetId) {
            this.clientID = clientID;
            this.packetId = packetId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            RunningSubscription that = (RunningSubscription) o;

            return packetId == that.packetId
                    && (clientID != null ? clientID.equals(that.clientID) : that.clientID == null);
        }

        @Override
        public int hashCode() {
            int result = clientID != null ? clientID.hashCode() : 0;
            result = 31 * result + (int) (packetId ^ (packetId >>> 32));
            return result;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolProcessor.class);

    private ConnectionDescriptorStore connectionDescriptors;
    private ConcurrentMap<RunningSubscription, SubscriptionState> subscriptionInCourse;

    private ISubscriptionsDirectory subscriptions;
    private ISubscriptionsStore subscriptionStore;
    private boolean allowAnonymous;
    private boolean allowZeroByteClientId;
    private IAuthorizator m_authorizator;

    private IMessagesStore m_messagesStore;

    private ISessionsStore m_sessionsStore;

    private IAuthenticator m_authenticator;
    private BrokerInterceptor m_interceptor;

    private Qos0PublishHandler qos0PublishHandler;
    private Qos1PublishHandler qos1PublishHandler;
    private Qos2PublishHandler qos2PublishHandler;
    private MessagesPublisher messagesPublisher;
    private InternalRepublisher internalRepublisher;
    SessionsRepository sessionsRepository;

    // maps clientID to Will testament, if specified on CONNECT
    private ConcurrentMap<String, WillMessage> m_willStore = new ConcurrentHashMap<>();

    ProtocolProcessor() {
    }

    public void init(ISubscriptionsDirectory subscriptions, IMessagesStore storageService, ISessionsStore sessionsStore,
                     IAuthenticator authenticator, boolean allowAnonymous, IAuthorizator authorizator,
                     BrokerInterceptor interceptor, SessionsRepository sessionsRepository) {
        init(subscriptions, storageService, sessionsStore, authenticator, allowAnonymous, false, authorizator,
            interceptor, sessionsRepository);
    }

    public void init(ISubscriptionsDirectory subscriptions, IMessagesStore storageService, ISessionsStore sessionsStore,
                     IAuthenticator authenticator, boolean allowAnonymous, boolean allowZeroByteClientId,
                     IAuthorizator authorizator, BrokerInterceptor interceptor, SessionsRepository sessionsRepository) {
        init(new ConnectionDescriptorStore(sessionsRepository), subscriptions, storageService, sessionsStore, authenticator,
            allowAnonymous, allowZeroByteClientId, authorizator, interceptor, sessionsRepository);
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
    void init(ConnectionDescriptorStore connectionDescriptors, ISubscriptionsDirectory subscriptions,
              IMessagesStore storageService, ISessionsStore sessionsStore, IAuthenticator authenticator,
              boolean allowAnonymous, boolean allowZeroByteClientId, IAuthorizator authorizator,
              BrokerInterceptor interceptor, SessionsRepository sessionsRepository) {
        LOG.info("Initializing MQTT protocol processor...");
        this.connectionDescriptors = connectionDescriptors;
        this.subscriptionInCourse = new ConcurrentHashMap<>();
        this.m_interceptor = interceptor;
        this.subscriptions = subscriptions;
        this.allowAnonymous = allowAnonymous;
        this.allowZeroByteClientId = allowZeroByteClientId;
        m_authorizator = authorizator;
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initial subscriptions tree={}", subscriptions.dumpTree());
        }
        m_authenticator = authenticator;
        m_messagesStore = storageService;
        m_sessionsStore = sessionsStore;
        subscriptionStore = sessionsStore.subscriptionStore();

        this.sessionsRepository = sessionsRepository;

        LOG.info("Initializing messages publisher...");
        final PersistentQueueMessageSender messageSender = new PersistentQueueMessageSender(this.connectionDescriptors);
        this.messagesPublisher = new MessagesPublisher(connectionDescriptors, messageSender,
            subscriptions, this.sessionsRepository);

        LOG.info("Initializing QoS publish handlers...");
        this.qos0PublishHandler = new Qos0PublishHandler(m_authorizator, m_messagesStore, m_interceptor,
                this.messagesPublisher);
        this.qos1PublishHandler = new Qos1PublishHandler(m_authorizator, m_messagesStore, m_interceptor,
                this.connectionDescriptors, this.messagesPublisher);
        this.qos2PublishHandler = new Qos2PublishHandler(m_authorizator, subscriptions, m_messagesStore, m_interceptor,
                this.connectionDescriptors, this.messagesPublisher, this.sessionsRepository);

        LOG.info("Initializing internal republisher...");
        this.internalRepublisher = new InternalRepublisher(messageSender);
    }

    public void processConnect(Channel channel, MqttConnectMessage msg) {
        MqttConnectPayload payload = msg.payload();
        String clientId = payload.clientIdentifier();
        LOG.info("Processing CONNECT message. CId={}, username={}", clientId, payload.userName());

        if (msg.variableHeader().version() != MqttVersion.MQTT_3_1.protocolLevel()
                && msg.variableHeader().version() != MqttVersion.MQTT_3_1_1.protocolLevel()) {
            MqttConnAckMessage badProto = connAck(CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);

            LOG.error("MQTT protocol version is not valid. CId={}", clientId);
            channel.writeAndFlush(badProto);
            channel.close();
            return;
        }

        final boolean cleanSession = msg.variableHeader().isCleanSession();
        if (clientId == null || clientId.length() == 0) {
            if (!cleanSession || !this.allowZeroByteClientId) {
                MqttConnAckMessage badId = connAck(CONNECTION_REFUSED_IDENTIFIER_REJECTED);

                channel.writeAndFlush(badId);
                channel.close();
                LOG.error("The MQTT client ID cannot be empty. Username={}", payload.userName());
                return;
            }

            // Generating client id.
            clientId = UUID.randomUUID().toString().replace("-", "");
            LOG.info("Client has connected with a server generated identifier. CId={}, username={}", clientId,
                payload.userName());
        }

        if (!login(channel, msg, clientId)) {
            channel.close();
            return;
        }

        ConnectionDescriptor descriptor = new ConnectionDescriptor(clientId, channel, cleanSession);
        ConnectionDescriptor existing = this.connectionDescriptors.addConnection(descriptor);
        if (existing != null) {
            LOG.info("Client ID is being used in an existing connection, force to be closed. CId={}", clientId);
            existing.abort();
            //return;
            this.connectionDescriptors.removeConnection(existing);
            this.connectionDescriptors.addConnection(descriptor);
        }

        initializeKeepAliveTimeout(channel, msg, clientId);
        storeWillMessage(msg, clientId);
        if (!sendAck(descriptor, msg, clientId)) {
            channel.close();
            return;
        }

        m_interceptor.notifyClientConnected(msg);

        if (!descriptor.assignState(SENDACK, SESSION_CREATED)) {
            channel.close();
            return;
        }
        final ClientSession clientSession = this.sessionsRepository.createOrLoadClientSession(clientId, cleanSession);

        if (!republish(descriptor, msg, clientSession)) {
            channel.close();
            return;
        }
        final boolean success = descriptor.assignState(MESSAGES_REPUBLISHED, ESTABLISHED);
        if (!success) {
            channel.close();
        }

        LOG.info("CONNECT message processed CId={}, username={}", clientId, payload.userName());
    }

    private MqttConnAckMessage connAck(MqttConnectReturnCode returnCode) {
        return connAck(returnCode, false);
    }

    private MqttConnAckMessage connAckWithSessionPresent(MqttConnectReturnCode returnCode) {
        return connAck(returnCode, true);
    }

    private MqttConnAckMessage connAck(MqttConnectReturnCode returnCode, boolean sessionPresent) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE,
                false, 0);
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(returnCode, sessionPresent);
        return new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
    }

    private boolean login(Channel channel, MqttConnectMessage msg, final String clientId) {
        // handle user authentication
        if (msg.variableHeader().hasUserName()) {
            byte[] pwd = null;
            if (msg.variableHeader().hasPassword()) {
                pwd = msg.payload().password().getBytes();
            } else if (!this.allowAnonymous) {
                LOG.error("Client didn't supply any password and MQTT anonymous mode is disabled CId={}", clientId);
                failedCredentials(channel);
                return false;
            }
            if (!m_authenticator.checkValid(clientId, msg.payload().userName(), pwd)) {
                LOG.error("Authenticator has rejected the MQTT credentials CId={}, username={}, password={}",
                        clientId, msg.payload().userName(), pwd);
                failedCredentials(channel);
                return false;
            }
            NettyUtils.userName(channel, msg.payload().userName());
        } else if (!this.allowAnonymous) {
            LOG.error("Client didn't supply any credentials and MQTT anonymous mode is disabled. CId={}", clientId);
            failedCredentials(channel);
            return false;
        }
        return true;
    }

    private boolean sendAck(ConnectionDescriptor descriptor, MqttConnectMessage msg, final String clientId) {
        LOG.info("Sending CONNACK. CId={}", clientId);
        final boolean success = descriptor.assignState(DISCONNECTED, SENDACK);
        if (!success) {
            return false;
        }

        MqttConnAckMessage okResp;
        ClientSession clientSession = this.sessionsRepository.sessionForClient(clientId);
        boolean isSessionAlreadyStored = clientSession != null;
        final boolean msgCleanSessionFlag = msg.variableHeader().isCleanSession();
        if (!msgCleanSessionFlag && isSessionAlreadyStored) {
            okResp = connAckWithSessionPresent(CONNECTION_ACCEPTED);
        } else {
            okResp = connAck(CONNECTION_ACCEPTED);
        }

        descriptor.writeAndFlush(okResp);
        LOG.info("CONNACK has been sent. CId={}", clientId);

        if (isSessionAlreadyStored && msgCleanSessionFlag) {
            for (Subscription existingSub : clientSession.getSubscriptions()) {
                this.subscriptions.removeSubscription(existingSub.getTopicFilter(), clientId);
            }
        }
        return true;
    }

    private void initializeKeepAliveTimeout(Channel channel, MqttConnectMessage msg, final String clientId) {
        int keepAlive = msg.variableHeader().keepAliveTimeSeconds();
        LOG.info("Configuring connection. CId={}", clientId);
        NettyUtils.keepAlive(channel, keepAlive);
        NettyUtils.cleanSession(channel, msg.variableHeader().isCleanSession());
        NettyUtils.clientID(channel, clientId);
        int idleTime = Math.round(keepAlive * 1.5f);
        setIdleTime(channel.pipeline(), idleTime);

        LOG.debug("The connection has been configured CId={}, keepAlive={}, removeTemporaryQoS2={}, idleTime={}",
                clientId, keepAlive, msg.variableHeader().isCleanSession(), idleTime);
    }

    private void storeWillMessage(MqttConnectMessage msg, final String clientId) {
        // Handle will flag
        if (msg.variableHeader().isWillFlag()) {
            MqttQoS willQos = MqttQoS.valueOf(msg.variableHeader().willQos());
            LOG.info("Configuring MQTT last will and testament CId={}, willQos={}, willTopic={}, willRetain={}",
                    clientId, willQos, msg.payload().willTopic(), msg.variableHeader().isWillRetain());
            byte[] willPayload = msg.payload().willMessage().getBytes();
            ByteBuffer bb = (ByteBuffer) ByteBuffer.allocate(willPayload.length).put(willPayload).flip();
            // save the will testament in the clientID store
            WillMessage will = new WillMessage(msg.payload().willTopic(), bb, msg.variableHeader().isWillRetain(),
                    willQos);
            m_willStore.put(clientId, will);
            LOG.info("MQTT last will and testament has been configured. CId={}", clientId);
        }
    }

    private boolean republish(ConnectionDescriptor descriptor, MqttConnectMessage msg, ClientSession clientSession) {
        final boolean success = descriptor.assignState(SESSION_CREATED, MESSAGES_REPUBLISHED);
        if (!success) {
            return false;
        }

        if (!msg.variableHeader().isCleanSession()) {
            // force the republish of stored QoS1 and QoS2
            LOG.info("Republishing stored publish events. CId={}", clientSession.clientID);
            this.internalRepublisher.publishStored(clientSession);
        }
        int flushIntervalMs = 500/* (keepAlive * 1000) / 2 */;
        descriptor.setupAutoFlusher(flushIntervalMs);
        return true;
    }

    private void failedCredentials(Channel session) {
        session.writeAndFlush(connAck(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD));
        LOG.info("Client {} failed to connect with bad username or password.", session);
    }

    private void setIdleTime(ChannelPipeline pipeline, int idleTime) {
        if (pipeline.names().contains("idleStateHandler")) {
            pipeline.remove("idleStateHandler");
        }
        pipeline.addFirst("idleStateHandler", new IdleStateHandler(idleTime, 0, 0));
    }

    public void processPubAck(Channel channel, MqttPubAckMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        int messageID = msg.variableHeader().messageId();
        String username = NettyUtils.userName(channel);
        LOG.trace("retrieving inflight for messageID <{}>", messageID);

        ClientSession targetSession = this.sessionsRepository.sessionForClient(clientID);
        StoredMessage inflightMsg = targetSession.inFlightAcknowledged(messageID);

        String topic = inflightMsg.getTopic();
        InterceptAcknowledgedMessage wrapped = new InterceptAcknowledgedMessage(inflightMsg, topic, username, messageID);
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
 	    if(will.isRetained()) {
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
        channel.writeAndFlush(pubRelMessage);
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

    public void processDisconnect(Channel channel) throws InterruptedException {
        final String clientID = NettyUtils.clientID(channel);
        LOG.info("Processing DISCONNECT message. CId={}", clientID);
        channel.flush();
        final ConnectionDescriptor existingDescriptor = this.connectionDescriptors.getConnection(clientID);
        if (existingDescriptor == null) {
            // another client with same ID removed the descriptor, we must exit
            channel.close();
            return;
        }

        if (existingDescriptor.doesNotUseChannel(channel)) {
            // another client saved it's descriptor, exit
            LOG.warn("Another client is using the connection descriptor. Closing connection. CId={}", clientID);
            existingDescriptor.abort();
            return;
        }

        if (!removeSubscriptions(existingDescriptor, clientID)) {
            LOG.warn("Unable to remove subscriptions. Closing connection. CId={}", clientID);
            existingDescriptor.abort();
            return;
        }

        if (!dropStoredMessages(existingDescriptor, clientID)) {
            LOG.warn("Unable to drop stored messages. Closing connection. CId={}", clientID);
            existingDescriptor.abort();
            return;
        }

        if (!cleanWillMessageAndNotifyInterceptor(existingDescriptor, clientID)) {
            LOG.warn("Unable to drop will message. Closing connection. CId={}", clientID);
            existingDescriptor.abort();
            return;
        }

        if (!existingDescriptor.close()) {
            LOG.info("Connection has been closed. CId={}", clientID);
            return;
        }

        boolean stillPresent = this.connectionDescriptors.removeConnection(existingDescriptor);
        if (!stillPresent) {
            // another descriptor was inserted
            LOG.warn("Another descriptor has been inserted. CId={}", clientID);
            return;
        }
        this.sessionsRepository.disconnect(clientID);

        LOG.info("DISCONNECT message has been processed. CId={}", clientID);
    }

    private boolean removeSubscriptions(ConnectionDescriptor descriptor, String clientID) {
        final boolean success = descriptor.assignState(ESTABLISHED, SUBSCRIPTIONS_REMOVED);
        if (!success) {
            return false;
        }

        if (descriptor.cleanSession) {
            LOG.info("Removing saved subscriptions. CId={}", descriptor.clientID);
            subscriptionStore.wipeSubscriptions(clientID);
            LOG.info("Saved subscriptions have been removed. CId={}", descriptor.clientID);
        }
        return true;
    }

    private boolean dropStoredMessages(ConnectionDescriptor descriptor, String clientID) {
        final boolean success = descriptor.assignState(SUBSCRIPTIONS_REMOVED, MESSAGES_DROPPED);
        if (!success) {
            return false;
        }

        final ClientSession clientSession = this.sessionsRepository.sessionForClient(clientID);
        if (clientSession.isCleanSession()) {
            clientSession.dropQueue();
//            LOG.debug("Removing messages of session. CId={}", descriptor.clientID);
//            this.m_sessionsStore.dropQueue(clientID);
//            LOG.debug("Messages of the session have been removed. CId={}", descriptor.clientID);
        }
        return true;
    }

    private boolean cleanWillMessageAndNotifyInterceptor(ConnectionDescriptor descriptor, String clientID) {
        final boolean success = descriptor.assignState(MESSAGES_DROPPED, INTERCEPTORS_NOTIFIED);
        if (!success) {
            return false;
        }

        LOG.info("Removing will message. ClientId={}", descriptor.clientID);
        // cleanup the will store
        m_willStore.remove(clientID);
        String username = descriptor.getUsername();
        m_interceptor.notifyClientDisconnected(clientID, username);
        return true;
    }

    public void processConnectionLost(String clientID, Channel channel) {
        LOG.info("Processing connection lost event. CId={}", clientID);
        ConnectionDescriptor oldConnDescr = new ConnectionDescriptor(clientID, channel, true);
        connectionDescriptors.removeConnection(oldConnDescr);
        // publish the Will message (if any) for the clientID
        if (m_willStore.containsKey(clientID)) {
            WillMessage will = m_willStore.get(clientID);
            forwardPublishWill(will, clientID);
            m_willStore.remove(clientID);
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

        LOG.info("Processing UNSUBSCRIBE message. CId={}, topics={}", clientID, topics);

        ClientSession clientSession = this.sessionsRepository.sessionForClient(clientID);
        for (String t : topics) {
            Topic topic = new Topic(t);
            boolean validTopic = topic.isValid();
            if (!validTopic) {
                // close the connection, not valid topicFilter is a protocol violation
                channel.close();
                LOG.error("Topic filter is not valid. CId={}, topics={}, badTopicFilter={}", clientID, topics, topic);
                return;
            }

            LOG.debug("Removing subscription. CId={}, topic={}", clientID, topic);
            subscriptions.removeSubscription(topic, clientID);
            clientSession.unsubscribeFrom(topic);
            String username = NettyUtils.userName(channel);
            m_interceptor.notifyTopicUnsubscribed(topic.toString(), clientID, username);
        }

        // ack the client
        int messageID = msg.variableHeader().messageId();
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.UNSUBACK, false, AT_LEAST_ONCE, false, 0);
        MqttUnsubAckMessage ackMessage = new MqttUnsubAckMessage(fixedHeader, from(messageID));

        LOG.info("Sending UNSUBACK message. CId={}, topics={}, messageId={}", clientID, topics, messageID);
        channel.writeAndFlush(ackMessage);
    }

    public void processSubscribe(Channel channel, MqttSubscribeMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        int messageID = messageId(msg);
        LOG.info("Processing SUBSCRIBE message. CId={}, messageId={}", clientID, messageID);

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

        LOG.info("Creating and storing subscriptions CId={}, messageId={}, topics={}", clientID, messageID, ackTopics);

        List<Subscription> newSubscriptions = doStoreSubscription(ackTopics, clientID);

        // save session, persist subscriptions from session
        for (Subscription subscription : newSubscriptions) {
            subscriptions.add(subscription);
        }

        LOG.info("Sending SUBACK response CId={}, messageId={}", clientID, messageID);
        channel.writeAndFlush(ackMessage);

        // fire the persisted messages in session
        for (Subscription subscription : newSubscriptions) {
            publishRetainedMessagesInSession(subscription, username);
        }

        boolean success = this.subscriptionInCourse.remove(executionKey, SubscriptionState.STORED);
        if (!success) {
            LOG.warn("Unable to perform the final subscription state update CId={}, messageId={}", clientID, messageID);
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
            Subscription newSubscription = new Subscription(clientID, new Topic(req.topicName()),
                                                            req.qualityOfService());

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
                LOG.error("Client does not have read permissions on the topic CId={}, username={}, messageId={}, " +
                    "topic={}", clientID, username, messageId, topic);
                ackTopics.add(new MqttTopicSubscription(topic.toString(), FAILURE));
            } else {
                MqttQoS qos;
                if (topic.isValid()) {
                    LOG.info("Client will be subscribed to the topic CId={}, username={}, messageId={}, topic={}",
                        clientID, username, messageId, topic);
                    qos = req.qualityOfService();
                } else {
                    LOG.error("Topic filter is not valid CId={}, username={}, messageId={}, topic={}", clientID,
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

    private void publishRetainedMessagesInSession(final Subscription newSubscription, String username) {
        LOG.info("Retrieving retained messages CId={}, topics={}", newSubscription.getClientId(),
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
                MqttPublishMessage pubMsg = createPublishForQos(msg.msg.getTopic(), msg.msg.getQos(), msg.msg.getPayload(),
                        msg.msg.isRetained(), msg.messageId);
                channel.write(pubMsg);
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
