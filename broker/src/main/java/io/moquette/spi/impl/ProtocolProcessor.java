/*
 * Copyright (c) 2012-2017 The original author or authorsgetRockQuestions()
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

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

import io.moquette.interception.InterceptHandler;
import io.moquette.parser.proto.messages.*;
import io.moquette.server.ConnectionDescriptor;
import io.moquette.server.ConnectionDescriptor.ConnectionState;
import io.moquette.server.ConnectionDescriptorStore;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.*;
import io.moquette.spi.IMessagesStore.StoredMessage;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizator;
import io.moquette.spi.impl.subscriptions.SubscriptionsStore;
import io.moquette.spi.impl.subscriptions.Subscription;

import static io.moquette.parser.netty.Utils.VERSION_3_1;
import static io.moquette.parser.netty.Utils.VERSION_3_1_1;
import static io.moquette.spi.impl.InternalRepublisher.createPublishForQos;

import io.moquette.interception.messages.InterceptAcknowledgedMessage;
import io.moquette.parser.proto.messages.AbstractMessage.QOSType;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class responsible to handle the logic of MQTT protocol it's the director of
 * the protocol execution.
 *
 * Used by the front facing class ProtocolProcessorBootstrapper.
 *
 * @author andrea
 */
public class ProtocolProcessor {

    static final class WillMessage {
        private final String topic;
        private final ByteBuffer payload;
        private final boolean retained;
        private final QOSType qos;

        public WillMessage(String topic, ByteBuffer payload, boolean retained, QOSType qos) {
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

        public QOSType getQos() {
            return qos;
        }

    }

    private enum SubscriptionState {
        STORED, VERIFIED
    }

    private class RunningSubscription {
        final String clientID;
        final long packetId;

        RunningSubscription(String clientID, long packeId) {
            this.clientID = clientID;
            this.packetId = packeId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RunningSubscription that = (RunningSubscription) o;

            if (packetId != that.packetId) return false;
            return clientID != null ? clientID.equals(that.clientID) : that.clientID == null;

        }

        @Override
        public int hashCode() {
            int result = clientID != null ? clientID.hashCode() : 0;
            result = 31 * result + (int) (packetId ^ (packetId >>> 32));
            return result;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolProcessor.class);

    protected ConnectionDescriptorStore connectionDescriptors;
    protected ConcurrentMap<RunningSubscription, SubscriptionState> subscriptionInCourse;

    private SubscriptionsStore subscriptions;
    private boolean allowAnonymous;
    private boolean allowZeroByteClientId;
    private IAuthorizator m_authorizator;
    private IMessagesStore m_messagesStore;
    private ISessionsStore m_sessionsStore;
    private IAuthenticator m_authenticator;
    private BrokerInterceptor m_interceptor;
    private String m_server_port;

    private Qos0PublishHandler qos0PublishHandler;
    private Qos1PublishHandler qos1PublishHandler;
    private Qos2PublishHandler qos2PublishHandler;
    private MessagesPublisher messagesPublisher;
    private InternalRepublisher internalRepublisher;

    //maps clientID to Will testament, if specified on CONNECT
    private ConcurrentMap<String, WillMessage> m_willStore = new ConcurrentHashMap<>();

    ProtocolProcessor() {}

    public void init(SubscriptionsStore subscriptions, IMessagesStore storageService,
                     ISessionsStore sessionsStore,
                     IAuthenticator authenticator,
                     boolean allowAnonymous, IAuthorizator authorizator, BrokerInterceptor interceptor) {
        init(subscriptions,storageService,sessionsStore,authenticator,allowAnonymous, false, authorizator,interceptor,null);
    }

    public void init(SubscriptionsStore subscriptions, IMessagesStore storageService,
                     ISessionsStore sessionsStore,
                     IAuthenticator authenticator,
                     boolean allowAnonymous,
                     boolean allowZeroByteClientId, IAuthorizator authorizator, BrokerInterceptor interceptor) {
        init(subscriptions,storageService,sessionsStore,authenticator,allowAnonymous, allowZeroByteClientId, authorizator,interceptor,null);
    }
    
    public void init(SubscriptionsStore subscriptions, IMessagesStore storageService,
            ISessionsStore sessionsStore,
            IAuthenticator authenticator,
            boolean allowAnonymous,
            boolean allowZeroByteClientId, IAuthorizator authorizator, BrokerInterceptor interceptor, String serverPort) {
		init(new ConnectionDescriptorStore(), subscriptions, storageService, sessionsStore, authenticator,
				allowAnonymous, allowZeroByteClientId, authorizator, interceptor, serverPort);
	}

    /**
     * @param subscriptions the subscription store where are stored all the existing
     *  clients subscriptions.
     * @param storageService the persistent store to use for save/load of messages
     *  for QoS1 and QoS2 handling.
     * @param sessionsStore the clients sessions store, used to persist subscriptions.
     * @param authenticator the authenticator used in connect messages.
     * @param allowAnonymous true connection to clients without credentials.
     * @param allowZeroByteClientId true to allow clients connect without a clientid
     * @param authorizator used to apply ACL policies to publishes and subscriptions.
     * @param interceptor to notify events to an intercept handler
     */
    void init(ConnectionDescriptorStore connectionDescriptors, SubscriptionsStore subscriptions, IMessagesStore storageService,
              ISessionsStore sessionsStore,
              IAuthenticator authenticator,
              boolean allowAnonymous,
              boolean allowZeroByteClientId, IAuthorizator authorizator, BrokerInterceptor interceptor, String serverPort) {
        LOG.info("Initializing MQTT protocol processor...");
        this.connectionDescriptors = connectionDescriptors;
        this.subscriptionInCourse = new ConcurrentHashMap<>();
        this.m_interceptor = interceptor;
        this.subscriptions = subscriptions;
        this.allowAnonymous = allowAnonymous;
        this.allowZeroByteClientId = allowZeroByteClientId;
        m_authorizator = authorizator;
		if (LOG.isDebugEnabled()) {
			LOG.debug("Initial subscriptions tree = {}.", subscriptions.dumpTree());
		}
        m_authenticator = authenticator;
        m_messagesStore = storageService;
        m_sessionsStore = sessionsStore;
        m_server_port = serverPort;

        LOG.info("Initializing messages publisher...");
        final PersistentQueueMessageSender messageSender = new PersistentQueueMessageSender(this.connectionDescriptors);
        this.messagesPublisher = new MessagesPublisher(connectionDescriptors, sessionsStore, m_messagesStore, messageSender);

        LOG.info("Initializing QoS publish handlers...");
        this.qos0PublishHandler = new Qos0PublishHandler(m_authorizator, subscriptions, m_messagesStore,
                m_interceptor, this.messagesPublisher);
        this.qos1PublishHandler = new Qos1PublishHandler(m_authorizator, subscriptions, m_messagesStore,
                m_interceptor, this.connectionDescriptors, m_server_port, this.messagesPublisher);
        this.qos2PublishHandler = new Qos2PublishHandler(m_authorizator, subscriptions, m_messagesStore,
                m_interceptor, this.connectionDescriptors, m_sessionsStore, m_server_port, this.messagesPublisher);

        LOG.info("Initializing internal republisher...");
        this.internalRepublisher = new InternalRepublisher(messageSender);
    }

    public void processConnect(Channel channel, ConnectMessage msg) {
        LOG.info("Processing CONNECT message. MqttClientId = {}, username = {}, password = {}.", msg.getClientID(),
				msg.getUsername(), msg.getPassword());

        if (msg.getProtocolVersion() != VERSION_3_1 && msg.getProtocolVersion() != VERSION_3_1_1) {
            ConnAckMessage badProto = new ConnAckMessage();
            badProto.setReturnCode(ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION);
            LOG.error("The MQTT protocol version is not valid. MqttClientId = {}, protocolVersion = {}.",
					msg.getProtocolVersion());
            channel.writeAndFlush(badProto);
            channel.close();
            return;
        }

        if (msg.getClientID() == null || msg.getClientID().length() == 0) {
            if(!msg.isCleanSession() || !this.allowZeroByteClientId) {
                ConnAckMessage okResp = new ConnAckMessage();
                okResp.setReturnCode(ConnAckMessage.IDENTIFIER_REJECTED);
                channel.writeAndFlush(okResp);
                channel.close();
                LOG.error("The MQTT client ID cannot be empty., Username = {}, password = {}.", msg.getUsername(),
						msg.getPassword());
                return;
            }

            // Generating client id.
            String randomIdentifier = UUID.randomUUID().toString().replace("-", "");
            msg.setClientID(randomIdentifier);
			LOG.info("The client has connected with a server generated identifier. MqttClientId = {}, username = {}, password = {}.",
					randomIdentifier, msg.getUsername(), msg.getPassword());
        }

        if (!login(channel, msg)) {
            channel.close();
            return;
        }

        final String clientID = msg.getClientID();
        ConnectionDescriptor descriptor = new ConnectionDescriptor(clientID, channel, msg.isCleanSession());
        ConnectionDescriptor existing = this.connectionDescriptors.addConnection(descriptor);
        if (existing != null) {
            LOG.info("The client ID is being used in an existing connection. It will be closed. MqttClientId = {}.",
					msg.getClientID());
            existing.abort();
            return;
        }

        initializeKeepAliveTimeout(channel, msg);

        storeWillMessage(msg);

        if (!sendAck(descriptor, msg)) {
            channel.close();
            return;
        }

        m_interceptor.notifyClientConnected(msg);

        final ClientSession clientSession = createOrLoadClientSession(descriptor, msg);
        if (clientSession == null) {
            channel.close();
            return;
        }

        if (!republish(descriptor, msg, clientSession)) {
            channel.close();
            return;
        }
        final boolean success = descriptor.assignState(ConnectionState.MESSAGES_REPUBLISHED, ConnectionState.ESTABLISHED);
        if (!success) {
            channel.close();
        }

        LOG.info("The CONNECT message has been processed. MqttClientId = {}, username = {}, password = {}.",
				msg.getClientID(), msg.getUsername(), msg.getPassword());
    }

    private boolean login(Channel channel, ConnectMessage msg) {
        //handle user authentication
        if (msg.isUserFlag()) {
            byte[] pwd = null;
            if (msg.isPasswordFlag()) {
                pwd = msg.getPassword();
            } else if (!this.allowAnonymous) {
				LOG.error("The client didn't supply any password and MQTT anonymous mode is disabled. MqttClientId = {}.",
						msg.getClientID());
                failedCredentials(channel);
                return false;
            }
            if (!m_authenticator.checkValid(msg.getClientID(), msg.getUsername(), pwd)) {
				LOG.error("The authenticator has rejected the MQTT credentials. MqttClientId = {}, username = {}, password = {}.",
						msg.getClientID(), msg.getUsername(), pwd);
                failedCredentials(channel);
                return false;
            }
            NettyUtils.userName(channel, msg.getUsername());
        } else if (!this.allowAnonymous) {
			LOG.error("The client didn't supply any credentials and MQTT anonymous mode is disabled. MqttClientId = {}.",
					msg.getClientID());
            failedCredentials(channel);
            return false;
        }
        return true;
    }

    private boolean sendAck(ConnectionDescriptor descriptor, ConnectMessage msg) {
		LOG.info("Sending connect ACK. MqttClientId = {}.", msg.getClientID());
        final boolean success = descriptor.assignState(ConnectionState.DISCONNECTED, ConnectionState.SENDACK);
        if (!success) {
            return false;
        }
        ConnAckMessage okResp = new ConnAckMessage();
        okResp.setReturnCode(ConnAckMessage.CONNECTION_ACCEPTED);

        ClientSession clientSession = m_sessionsStore.sessionForClient(msg.getClientID());
        boolean isSessionAlreadyStored = clientSession != null;
        if (!msg.isCleanSession() && isSessionAlreadyStored) {
            okResp.setSessionPresent(true);
        }
        if (isSessionAlreadyStored) {
			LOG.info("Cleaning session. MqttClientId = {}.", msg.getClientID());
            clientSession.cleanSession(msg.isCleanSession());
        }
        descriptor.writeAndFlush(okResp);
        LOG.info("The connect ACK has been sent. MqttClientId = {}.", msg.getClientID());
        return true;
    }

    private void initializeKeepAliveTimeout(Channel channel, ConnectMessage msg) {
        int keepAlive = msg.getKeepAlive();
        LOG.info("Configuring connection. MqttClientId = {}.", msg.getClientID());
        NettyUtils.keepAlive(channel, keepAlive);
        //session.attr(NettyUtils.ATTR_KEY_CLEANSESSION).set(msg.isCleanSession());
        NettyUtils.cleanSession(channel, msg.isCleanSession());
        //used to track the client in the subscription and publishing phases.
        //session.attr(NettyUtils.ATTR_KEY_CLIENTID).set(msg.getClientID());
        NettyUtils.clientID(channel, msg.getClientID());
        int idleTime = Math.round(keepAlive * 1.5f);
        setIdleTime(channel.pipeline(), idleTime);

        LOG.info("The connection has been configured. MqttClientId = {}, keepAlive = {}, cleanSession = {}, idleTime = {}.",
				msg.getClientID(), keepAlive, msg.isCleanSession(), idleTime);
    }

    private void storeWillMessage(ConnectMessage msg) {
        //Handle will flag
        if (msg.isWillFlag()) {
            AbstractMessage.QOSType willQos = AbstractMessage.QOSType.valueOf(msg.getWillQos());
			LOG.info("Configuring MQTT last will and testament. MqttClientId = {}, willTopic = {}, willQos = {}, willRetain = {}.",
					msg.getClientID(), willQos, msg.getWillTopic(), msg.isWillRetain());
            byte[] willPayload = msg.getWillMessage();
            ByteBuffer bb = (ByteBuffer) ByteBuffer.allocate(willPayload.length).put(willPayload).flip();
            //save the will testament in the clientID store
            WillMessage will = new WillMessage(msg.getWillTopic(), bb, msg.isWillRetain(),willQos);
            m_willStore.put(msg.getClientID(), will);
            LOG.info("MQTT last will and testament has been configured. MqttClientId = {}.", msg.getClientID());
        }
    }

    private ClientSession createOrLoadClientSession(ConnectionDescriptor descriptor, ConnectMessage msg) {
        final boolean success = descriptor.assignState(ConnectionState.SENDACK, ConnectionState.SESSION_CREATED);
        if (!success) {
            return null;
        }

        ClientSession clientSession = m_sessionsStore.sessionForClient(msg.getClientID());
        boolean isSessionAlreadyStored = clientSession != null;
        if (!isSessionAlreadyStored) {
            clientSession = m_sessionsStore.createNewSession(msg.getClientID(), msg.isCleanSession());
        }
        if (msg.isCleanSession()) {
			LOG.info("Cleaning session. MqttClientId = {}.", msg.getClientID());
            clientSession.cleanSession();
        }
        return clientSession;
    }

    private boolean republish(ConnectionDescriptor descriptor, ConnectMessage msg, ClientSession clientSession) {
        final boolean success = descriptor.assignState(ConnectionState.SESSION_CREATED, ConnectionState.MESSAGES_REPUBLISHED);
        if (!success) {
            return false;
        }

        if (!msg.isCleanSession()) {
            //force the republish of stored QoS1 and QoS2
            republishStoredInSession(clientSession);
        }
        int flushIntervalMs = 500/*(keepAlive * 1000) / 2*/;
        descriptor.setupAutoFlusher(flushIntervalMs);
        return true;
    }

    private void failedCredentials(Channel session) {
        ConnAckMessage okResp = new ConnAckMessage();
        okResp.setReturnCode(ConnAckMessage.BAD_USERNAME_OR_PASSWORD);
        session.writeAndFlush(okResp);
        LOG.info("Client {} failed to connect with bad username or password.", session);
    }

    private void setIdleTime(ChannelPipeline pipeline, int idleTime) {
        if (pipeline.names().contains("idleStateHandler")) {
            pipeline.remove("idleStateHandler");
        }
        pipeline.addFirst("idleStateHandler", new IdleStateHandler(0, 0, idleTime));
    }

    /**
     * Republish QoS1 and QoS2 messages stored into the session for the clientID.
     * */
    private void republishStoredInSession(ClientSession clientSession) {
        LOG.info("Republishing stored publish events. MqttClientId = {}.", clientSession.clientID);
        BlockingQueue<StoredMessage> publishedEvents = clientSession.queue();
        if (publishedEvents.isEmpty()) {
            LOG.info("There are no stored publish events. ClientId = {}.", clientSession.clientID);
            return;
        }

        this.internalRepublisher.publishStored(clientSession, publishedEvents);
    }

    public void processPubAck(Channel channel, PubAckMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        int messageID = msg.getMessageID();
        String username = NettyUtils.userName(channel);
        LOG.trace("retrieving inflight for messageID <{}>", messageID);

        ClientSession targetSession = m_sessionsStore.sessionForClient(clientID);
        StoredMessage inflightMsg = targetSession.getInflightMessage(messageID);
        targetSession.inFlightAcknowledged(messageID);

        String topic = inflightMsg.getTopic();

//        MessageGUID guid = inflightMsg.getGuid();
        //Remove the message from message store
//        m_messagesStore.decUsageCounter(guid);

        m_interceptor.notifyMessageAcknowledged(new InterceptAcknowledgedMessage(inflightMsg, topic, username));
    }

    public static IMessagesStore.StoredMessage asStoredMessage(PublishMessage msg) {
        IMessagesStore.StoredMessage stored = new IMessagesStore.StoredMessage(msg.getPayload().array(), msg.getQos(), msg.getTopicName());
        stored.setRetained(msg.isRetainFlag());
        stored.setMessageID(msg.getMessageID());
        return stored;
    }

    private static IMessagesStore.StoredMessage asStoredMessage(WillMessage will) {
        IMessagesStore.StoredMessage pub = new IMessagesStore.StoredMessage(will.getPayload().array(), will.getQos(), will.getTopic());
        pub.setRetained(will.isRetained());
        return pub;
    }

    public void processPublish(Channel channel, PublishMessage msg) {
		LOG.info("Processing PUBLISH message. MqttClientId = {}, topic = {}, messageId = {}, qos = {}.",
				msg.getClientId(), msg.getTopicName(), msg.getMessageID(), msg.getQos());
        final AbstractMessage.QOSType qos = msg.getQos();
        switch (qos) {
            case MOST_ONE:
                this.qos0PublishHandler.receivedPublishQos0(channel, msg);
                break;
            case LEAST_ONE:
                this.qos1PublishHandler.receivedPublishQos1(channel, msg);
                break;
            case EXACTLY_ONCE:
                this.qos2PublishHandler.receivedPublishQos2(channel, msg);
                break;
            default:
            	break;
        }
    }

    /**
     * Intended usage is only for embedded versions of the broker, where the hosting application want to use the
     * broker to send a publish message.
     * Inspired by {@link #processPublish} but with some changes to avoid security check, and the handshake phases
     * for Qos1 and Qos2.
     * It also doesn't notifyTopicPublished because using internally the owner should already know where
     * it's publishing.
     *
     * @param msg the message to publish.
     * */
    public void internalPublish(PublishMessage msg) {
        final AbstractMessage.QOSType qos = msg.getQos();
        final String topic = msg.getTopicName();
        LOG.info("Sending PUBLISH message. Topic = {}, qos = {}.", topic, qos);

        MessageGUID guid = null;
        IMessagesStore.StoredMessage toStoreMsg = asStoredMessage(msg);
        if (msg.getClientId() == null || msg.getClientId().isEmpty()) {
            toStoreMsg.setClientID("BROKER_SELF");
        } else {
            toStoreMsg.setClientID(msg.getClientId());
        }
        toStoreMsg.setMessageID(1);
        if (qos == AbstractMessage.QOSType.EXACTLY_ONCE) { //QoS2
            guid = m_messagesStore.storePublishForFuture(toStoreMsg);
        }
        List<Subscription> topicMatchingSubscriptions = subscriptions.matches(topic);
        this.messagesPublisher.publish2Subscribers(toStoreMsg, topicMatchingSubscriptions);

        if (!msg.isRetainFlag()) {
            return;
        }
        if (qos == AbstractMessage.QOSType.MOST_ONE || !msg.getPayload().hasRemaining()) {
            //QoS == 0 && retain => clean old retained
            m_messagesStore.cleanRetained(topic);
            return;
        }
        if (guid == null) {
            //before wasn't stored
            guid = m_messagesStore.storePublishForFuture(toStoreMsg);
        }
        m_messagesStore.storeRetained(topic, guid);
    }

    /**
     * Specialized version to publish will testament message.
     */
    private void forwardPublishWill(WillMessage will, String clientID) {
        //it has just to publish the message downstream to the subscribers
        //NB it's a will publish, it needs a PacketIdentifier for this conn, default to 1
        Integer messageId = null;
        if (will.getQos() != AbstractMessage.QOSType.MOST_ONE) {
            messageId = m_sessionsStore.nextPacketID(clientID);
        }

        IMessagesStore.StoredMessage tobeStored = asStoredMessage(will);
        tobeStored.setClientID(clientID);
        tobeStored.setMessageID(messageId);
        String topic = tobeStored.getTopic();
        List<Subscription> topicMatchingSubscriptions = subscriptions.matches(topic);

		LOG.info("Publishing will message. MqttClientId = {}, messageId = {}, topic = {}.", clientID, messageId, topic);
        this.messagesPublisher.publish2Subscribers(tobeStored, topicMatchingSubscriptions);
    }

    static QOSType lowerQosToTheSubscriptionDesired(Subscription sub, QOSType qos) {
        if (qos.byteValue() > sub.getRequestedQos().byteValue()) {
            qos = sub.getRequestedQos();
        }
        return qos;
    }

    /**
     * Second phase of a publish QoS2 protocol, sent by publisher to the broker. Search the stored message and publish
     * to all interested subscribers.
     * @param channel the channel of the incoming message.
     * @param msg the decoded pubrel message.
     * */
    public void processPubRel(Channel channel, PubRelMessage msg) {
        this.qos2PublishHandler.processPubRel(channel, msg);
    }

    public void processPubRec(Channel channel, PubRecMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        ClientSession targetSession = m_sessionsStore.sessionForClient(clientID);
        //remove from the inflight and move to the QoS2 second phase queue
        int messageID = msg.getMessageID();
        targetSession.moveInFlightToSecondPhaseAckWaiting(messageID);
        //once received a PUBREC reply with a PUBREL(messageID)
		if (LOG.isDebugEnabled()) {
			LOG.debug("Processing PUBREC message. MqttClientId = {}, messageId = {}.", clientID, messageID);
		}
        PubRelMessage pubRelMessage = new PubRelMessage();
        pubRelMessage.setMessageID(messageID);
        pubRelMessage.setQos(AbstractMessage.QOSType.LEAST_ONE);

        channel.writeAndFlush(pubRelMessage);
    }

    public void processPubComp(Channel channel, PubCompMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        int messageID = msg.getMessageID();
		if (LOG.isDebugEnabled()) {
			LOG.debug("Processing PUBCOMP message. MqttClientId = {}, messageId = {}.", clientID, messageID);
		}
        //once received the PUBCOMP then remove the message from the temp memory
        ClientSession targetSession = m_sessionsStore.sessionForClient(clientID);
        StoredMessage inflightMsg = targetSession.secondPhaseAcknowledged(messageID);
        String username = NettyUtils.userName(channel);
        String topic = inflightMsg.getTopic();
        m_interceptor.notifyMessageAcknowledged(new InterceptAcknowledgedMessage(inflightMsg, topic, username));
    }

    public void processDisconnect(Channel channel) throws InterruptedException {
	final String clientID = NettyUtils.clientID(channel);
	LOG.info("Processing DISCONNECT message. MqttClientId = {}.", clientID);
	channel.flush();
        final ConnectionDescriptor existingDescriptor = this.connectionDescriptors.getConnection(clientID);
        if (existingDescriptor == null) {
            //another client with same ID removed the descriptor, we must exit
            channel.close();
            return;
        }

        if (existingDescriptor.doesNotUseChannel(channel)) {
            //another client saved it's descriptor, exit
			LOG.warn("Another client is using the connection descriptor. Closing connection. MqttClientId = {}.",
					clientID);
			existingDescriptor.abort();
            return;
        }

        if (!removeSubscriptions(existingDescriptor, clientID)) {
			LOG.warn("Unable to remove subscriptions. Closing connection. MqttClientId = {}.", clientID);
			existingDescriptor.abort();
            return;
        }

        if (!dropStoredMessages(existingDescriptor, clientID)) {
			LOG.warn("Unable to drop stored messages. Closing connection. MqttClientId = {}.", clientID);
			existingDescriptor.abort();
            return;
        }

        if (!cleanWillMessageAndNotifyInterceptor(existingDescriptor, clientID)) {
			LOG.warn("Unable to drop will message. Closing connection. MqttClientId = {}.", clientID);
			existingDescriptor.abort();
            return;
        }

        if (!existingDescriptor.close()) {
			LOG.info("The connection has been closed. MqttClientId = {}.", clientID);
            return;
        }

        boolean stillPresent = this.connectionDescriptors.removeConnection(existingDescriptor);
        if (!stillPresent) {
            //another descriptor was inserted
			LOG.warn("Another descriptor has been inserted. MqttClientId = {}.", clientID);
			return;
        }

		LOG.info("The DISCONNECT message has been processed. MqttClientId = {}.", clientID);
    }

    private boolean removeSubscriptions(ConnectionDescriptor descriptor, String clientID) {
        final boolean success = descriptor.assignState(ConnectionState.ESTABLISHED, ConnectionState.SUBSCRIPTIONS_REMOVED);
        if (!success) {
            return false;
        }

        if (descriptor.cleanSession) {
            LOG.info("Removing saved subscriptions. MqttClientId = {}.", descriptor.clientID);
            m_sessionsStore.wipeSubscriptions(clientID);
            LOG.info("The saved subscriptions have been removed. MqttClientId = {}.", descriptor.clientID);
        }
        return true;
    }

    private boolean dropStoredMessages(ConnectionDescriptor descriptor, String clientID) {
        final boolean success = descriptor.assignState(ConnectionState.SUBSCRIPTIONS_REMOVED, ConnectionState.MESSAGES_DROPPED);
        if (!success) {
            return false;
        }

        if (descriptor.cleanSession) {
            LOG.debug("Removing messages of session. MqttClientId = {}.", descriptor.clientID);
            this.m_sessionsStore.dropQueue(clientID);
            LOG.debug("The messages of the session have been removed. MqttClientId = {}.", descriptor.clientID);
        }
        return true;
    }

    private boolean cleanWillMessageAndNotifyInterceptor(ConnectionDescriptor descriptor, String clientID) {
        final boolean success = descriptor.assignState(ConnectionState.MESSAGES_DROPPED, ConnectionState.INTERCEPTORS_NOTIFIED);
        if (!success) {
            return false;
        }

        LOG.info("Removing will message. ClientId = {}.", descriptor.clientID);
        //cleanup the will store
        m_willStore.remove(clientID);
        String username = descriptor.getUsername();
        m_interceptor.notifyClientDisconnected(clientID, username);
        return true;
    }

    public void processConnectionLost(String clientID, Channel channel) {
		LOG.info("Processing connection lost event. MqttClientId = {}.", clientID);
        ConnectionDescriptor oldConnDescr = new ConnectionDescriptor(clientID, channel, true);
        connectionDescriptors.removeConnection(oldConnDescr);
        //publish the Will message (if any) for the clientID
        if (m_willStore.containsKey(clientID)) {
            WillMessage will = m_willStore.get(clientID);
            forwardPublishWill(will, clientID);
            m_willStore.remove(clientID);
        }

        String username = NettyUtils.userName(channel);
        m_interceptor.notifyClientConnectionLost(clientID, username);
    }

    /**
     * Remove the clientID from topic subscription, if not previously subscribed,
     * doesn't reply any error.
     * @param channel the channel of the incoming message.
     * @param msg the decoded unsubscribe message.
     */
    public void processUnsubscribe(Channel channel, UnsubscribeMessage msg) {
        List<String> topics = msg.topicFilters();
        String clientID = NettyUtils.clientID(channel);

		LOG.info("Processing UNSUBSCRIBE message. MqttClientId = {}, topics = {}.", clientID, topics);

        ClientSession clientSession = m_sessionsStore.sessionForClient(clientID);
        for (String topic : topics) {
            boolean validTopic = SubscriptionsStore.validate(topic);
            if (!validTopic) {
                //close the connection, not valid topicFilter is a protocol violation
                channel.close();
				LOG.error("The topic filter is not valid. MqttClientId = {}, topics = {}, badTopicFilter = {}.",
						clientID, topics, topic);
                return;
            }

			if (LOG.isDebugEnabled()) {
				LOG.debug("Removing subscription. MqttClientId = {}, topic = {}.", clientID, topic);
			}
            subscriptions.removeSubscription(topic, clientID);
            clientSession.unsubscribeFrom(topic);
            String username = NettyUtils.userName(channel);
            m_interceptor.notifyTopicUnsubscribed(topic, clientID, username);
        }

        //ack the client
        int messageID = msg.getMessageID();
        UnsubAckMessage ackMessage = new UnsubAckMessage();
        ackMessage.setMessageID(messageID);

		LOG.info("Sending UNSUBACK message. MqttClientId = {}, topics = {}, messageId = {}.", clientID, topics,
				messageID);
        channel.writeAndFlush(ackMessage);
    }

    public void processSubscribe(Channel channel, SubscribeMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        int messageID = msg.getMessageID();
		LOG.info("Processing SUBSCRIBE message. MqttClientId = {}, messageId = {}.", clientID, msg.getMessageID());

        RunningSubscription executionKey = new RunningSubscription(clientID, messageID);
        SubscriptionState currentStatus = this.subscriptionInCourse.putIfAbsent(executionKey, SubscriptionState.VERIFIED);
        if (currentStatus != null) {
			LOG.warn("The client sent another SUBSCRIBE message while this one was being processed. MqttClientId = {}, messageId = {}.",
					clientID, msg.getMessageID());
            return;
        }
        String username = NettyUtils.userName(channel);
        List<SubscribeMessage.Couple> ackTopics = doVerify(clientID, username, msg);
        SubAckMessage ackMessage = doAckMessageFromValidateFilters(ackTopics);
        if (!this.subscriptionInCourse.replace(executionKey, SubscriptionState.VERIFIED, SubscriptionState.STORED)) {
			LOG.warn("The client sent another SUBSCRIBE message while the topic filters were being verified. MqttClientId = {}, messageId = {}.",
					clientID, msg.getMessageID());
            return;
        }

		LOG.info("Creating and storing subscriptions. MqttClientId = {}, messageId = {}, topics = {}.", clientID,
				msg.getMessageID(), ackTopics);

        ackMessage.setMessageID(messageID);
        List<Subscription> newSubscriptions = doStoreSubscription(ackTopics, clientID);

        //save session, persist subscriptions from session

        for (Subscription subscription : newSubscriptions) {
            subscriptions.add(subscription.asClientTopicCouple());
        }

		LOG.info("Sending SUBACK response. MqttClientId = {}, messageId = {}.", clientID, msg.getMessageID());
        channel.writeAndFlush(ackMessage);

        //fire the persisted messages in session
        for (Subscription subscription : newSubscriptions) {
            publishRetainedMessagesInSession(subscription, username);
        }

        boolean success = this.subscriptionInCourse.remove(executionKey, SubscriptionState.STORED);
        if (!success) {
			LOG.warn("Unable to perform the final subscription state update. MqttClientId = {}, messageId = {}.",
					clientID, msg.getMessageID());
        }
    }

    private List<Subscription> doStoreSubscription(List<SubscribeMessage.Couple> ackTopics, String clientID) {
        ClientSession clientSession = m_sessionsStore.sessionForClient(clientID);

        List<Subscription> newSubscriptions = new ArrayList<>();
        for (SubscribeMessage.Couple req : ackTopics) {
            //TODO this is SUPER UGLY
            if (req.qos == AbstractMessage.QOSType.FAILURE.byteValue()) {
                continue;
            }
            AbstractMessage.QOSType qos = AbstractMessage.QOSType.valueOf(req.qos);
            Subscription newSubscription = new Subscription(clientID, req.topicFilter, qos);
            clientSession.subscribe(newSubscription);
            newSubscriptions.add(newSubscription);
        }
        return newSubscriptions;
    }

    /**
     * @return the list of verified topics
     * @param clientID
     * @param username
     * */
    private List<SubscribeMessage.Couple> doVerify(String clientID, String username, SubscribeMessage msg) {
        ClientSession clientSession = m_sessionsStore.sessionForClient(clientID);
        List<SubscribeMessage.Couple> ackTopics = new ArrayList<>();

        for (SubscribeMessage.Couple req : msg.subscriptions()) {
            if (!m_authorizator.canRead(req.topicFilter, username, clientSession.clientID)) {
                //send SUBACK with 0x80, the user hasn't credentials to read the topic
				LOG.error("The client does not have read permissions on the topic. MqttClientId = {}, username = {}, messageId = {}, topic = {}.",
						clientID, username, msg.getMessageID(), req.topicFilter);
                ackTopics.add(new SubscribeMessage.Couple(AbstractMessage.QOSType.FAILURE.byteValue(), req.topicFilter));
            } else {
                AbstractMessage.QOSType qos = null;
                if (SubscriptionsStore.validate(req.topicFilter)) {
					qos = AbstractMessage.QOSType.valueOf(req.qos);
					LOG.info("The client will be subscribed to the topic. MqttClientId = {}, username = {}, messageId = {}, topic = {}.",
							clientID, username, msg.getMessageID(), req.topicFilter);
                } else {
					LOG.error("The topic filter is not valid. MqttClientId = {}, username = {}, messageId = {}, topic = {}.",
							clientID, username, msg.getMessageID(), req.topicFilter);
					qos = AbstractMessage.QOSType.FAILURE;
                }
                ackTopics.add(new SubscribeMessage.Couple(qos.byteValue(), req.topicFilter));
            }
        }
        return ackTopics;
    }

    /**
     * Create the SUBACK response from a list of topicFilters
     * */
    private SubAckMessage doAckMessageFromValidateFilters(List<SubscribeMessage.Couple> topicFilters) {
        //ack the client
        SubAckMessage ackMessage = new SubAckMessage();
        for (SubscribeMessage.Couple req : topicFilters) {
            ackMessage.addType(AbstractMessage.QOSType.valueOf(req.qos));
        }
        return ackMessage;
    }

    private void publishRetainedMessagesInSession(final Subscription newSubscription, String username) {
		LOG.info("Retrieving retained messages. MqttClientId = {}, topics = {}.", newSubscription.getClientId(),
				newSubscription.getTopicFilter());

        //scans retained messages to be published to the new subscription
        //TODO this is ugly, it does a linear scan on potential big dataset
        Collection<IMessagesStore.StoredMessage> messages = m_messagesStore.searchMatching(new IMatchingCondition() {
            @Override
            public boolean match(String key) {
                return SubscriptionsStore.matchTopics(key, newSubscription.getTopicFilter());
            }
        });

        if (!messages.isEmpty()) {
			LOG.info("Publishing retained messages. MqttClientId = {}, topics = {}, messagesNo = {}.",
					newSubscription.getClientId(), newSubscription.getTopicFilter(), messages.size());
        }
        ClientSession targetSession = m_sessionsStore.sessionForClient(newSubscription.getClientId());
        this.internalRepublisher.publishRetained(targetSession, messages);

        //notify the Observables
        m_interceptor.notifyTopicSubscribed(newSubscription, username);
    }

    public void notifyChannelWritable(Channel channel) {
        String clientID = NettyUtils.clientID(channel);
        ClientSession clientSession = m_sessionsStore.sessionForClient(clientID);
        boolean emptyQueue = false;
        while (channel.isWritable()  && !emptyQueue) {
            StoredMessage msg = clientSession.queue().poll();
            if (msg == null) {
                emptyQueue = true;
            } else {
                //recreate a publish from stored publish in queue
                boolean retained = m_messagesStore.getMessageByGuid(msg.getGuid()) != null;
                PublishMessage pubMsg = createPublishForQos(msg.getTopic(), msg.getQos(), msg.getMessage(), retained);
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
}
