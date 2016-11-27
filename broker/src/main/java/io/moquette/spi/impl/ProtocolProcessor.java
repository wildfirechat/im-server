/*
 * Copyright (c) 2012-2015 The original author or authors
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import io.moquette.interception.InterceptHandler;
import io.moquette.server.ConnectionDescriptor;
import io.moquette.server.ConnectionDescriptor.ConnectionState;
import io.moquette.server.netty.AutoFlushHandler;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.*;
import io.moquette.spi.IMessagesStore.StoredMessage;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizator;
import io.moquette.spi.impl.subscriptions.SubscriptionsStore;
import io.moquette.spi.impl.subscriptions.Subscription;

import static io.moquette.parser.netty.Utils.VERSION_3_1;
import static io.moquette.parser.netty.Utils.VERSION_3_1_1;
import io.moquette.interception.messages.InterceptAcknowledgedMessage;
import io.moquette.parser.proto.messages.AbstractMessage;
import io.moquette.parser.proto.messages.AbstractMessage.QOSType;
import io.moquette.parser.proto.messages.ConnAckMessage;
import io.moquette.parser.proto.messages.ConnectMessage;
import io.moquette.parser.proto.messages.PubAckMessage;
import io.moquette.parser.proto.messages.PubCompMessage;
import io.moquette.parser.proto.messages.PubRecMessage;
import io.moquette.parser.proto.messages.PubRelMessage;
import io.moquette.parser.proto.messages.PublishMessage;
import io.moquette.parser.proto.messages.SubAckMessage;
import io.moquette.parser.proto.messages.SubscribeMessage;
import io.moquette.parser.proto.messages.UnsubAckMessage;
import io.moquette.parser.proto.messages.UnsubscribeMessage;
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
        final long packeId;

        RunningSubscription(String clientID, long packeId) {
            this.clientID = clientID;
            this.packeId = packeId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RunningSubscription that = (RunningSubscription) o;

            if (packeId != that.packeId) return false;
            return clientID != null ? clientID.equals(that.clientID) : that.clientID == null;

        }

        @Override
        public int hashCode() {
            int result = clientID != null ? clientID.hashCode() : 0;
            result = 31 * result + (int) (packeId ^ (packeId >>> 32));
            return result;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolProcessor.class);

    protected ConcurrentMap<String, ConnectionDescriptor> connectionDescriptors;
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
    void init(SubscriptionsStore subscriptions, IMessagesStore storageService,
              ISessionsStore sessionsStore,
              IAuthenticator authenticator,
              boolean allowAnonymous,
              boolean allowZeroByteClientId, IAuthorizator authorizator, BrokerInterceptor interceptor, String serverPort) {
        this.connectionDescriptors = new ConcurrentHashMap<>();
        this.subscriptionInCourse = new ConcurrentHashMap<>();
        this.m_interceptor = interceptor;
        this.subscriptions = subscriptions;
        this.allowAnonymous = allowAnonymous;
        this.allowZeroByteClientId = allowZeroByteClientId;
        m_authorizator = authorizator;
        LOG.trace("subscription tree on init {}", subscriptions.dumpTree());
        m_authenticator = authenticator;
        m_messagesStore = storageService;
        m_sessionsStore = sessionsStore;
        m_server_port = serverPort;
    }

    public void processConnect(Channel channel, ConnectMessage msg) {
        LOG.info("CONNECT for client <{}>", msg.getClientID());

        if (msg.getProtocolVersion() != VERSION_3_1 && msg.getProtocolVersion() != VERSION_3_1_1) {
            ConnAckMessage badProto = new ConnAckMessage();
            badProto.setReturnCode(ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION);
            LOG.warn("CONNECT sent bad proto ConnAck");
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
                LOG.warn("CONNECT sent rejected identifier ConnAck");
                return;
            }

            // Generating client id.
            String randomIdentifier = UUID.randomUUID().toString().replace("-", "");
            msg.setClientID(randomIdentifier);
            LOG.info("Client connected with server generated identifier: {}", randomIdentifier);
        }

        if (!login(channel, msg)) {
            channel.close();
            return;
        }

        final String clientID = msg.getClientID();
        ConnectionDescriptor descriptor = new ConnectionDescriptor(clientID, channel, msg.isCleanSession());
        ConnectionDescriptor existing = this.connectionDescriptors.putIfAbsent(clientID, descriptor);
        if (existing != null) {
            LOG.info("Found an existing connection with same client ID <{}>, forcing to close", msg.getClientID());
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
        } else {
            LOG.info("Connection established");
        }
        LOG.info("CONNECT processed");
    }

    private boolean login(Channel channel, ConnectMessage msg) {
        //handle user authentication
        if (msg.isUserFlag()) {
            byte[] pwd = null;
            if (msg.isPasswordFlag()) {
                pwd = msg.getPassword();
            } else if (!this.allowAnonymous) {
                failedCredentials(channel);
                return false;
            }
            if (!m_authenticator.checkValid(msg.getClientID(), msg.getUsername(), pwd)) {
                failedCredentials(channel);
                return false;
            }
            NettyUtils.userName(channel, msg.getUsername());
        } else if (!this.allowAnonymous) {
            failedCredentials(channel);
            return false;
        }
        return true;
    }

    private boolean sendAck(ConnectionDescriptor descriptor, ConnectMessage msg) {
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
            clientSession.cleanSession(msg.isCleanSession());
        }
        descriptor.channel.writeAndFlush(okResp);
        return true;
    }

    private void initializeKeepAliveTimeout(Channel channel, ConnectMessage msg) {
        int keepAlive = msg.getKeepAlive();
        LOG.debug("Connect with keepAlive {} s",  keepAlive);
        NettyUtils.keepAlive(channel, keepAlive);
        //session.attr(NettyUtils.ATTR_KEY_CLEANSESSION).set(msg.isCleanSession());
        NettyUtils.cleanSession(channel, msg.isCleanSession());
        //used to track the client in the subscription and publishing phases.
        //session.attr(NettyUtils.ATTR_KEY_CLIENTID).set(msg.getClientID());
        NettyUtils.clientID(channel, msg.getClientID());
        LOG.debug("Connect create session <{}>", channel);

        setIdleTime(channel.pipeline(), Math.round(keepAlive * 1.5f));
    }

    private void storeWillMessage(ConnectMessage msg) {
        //Handle will flag
        if (msg.isWillFlag()) {
            AbstractMessage.QOSType willQos = AbstractMessage.QOSType.valueOf(msg.getWillQos());
            byte[] willPayload = msg.getWillMessage();
            ByteBuffer bb = (ByteBuffer) ByteBuffer.allocate(willPayload.length).put(willPayload).flip();
            //save the will testament in the clientID store
            WillMessage will = new WillMessage(msg.getWillTopic(), bb, msg.isWillRetain(),willQos);
            m_willStore.put(msg.getClientID(), will);
            LOG.info("Session for clientID <{}> with will to topic {}", msg.getClientID(), msg.getWillTopic());
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
            LOG.debug("Create persistent session for clientID <{}>", msg.getClientID());
            clientSession = m_sessionsStore.createNewSession(msg.getClientID(), msg.isCleanSession());
        }
        if (msg.isCleanSession()) {
            clientSession.cleanSession();
        }
        LOG.debug("Created session for client ID <{}> with clean session {}", msg.getClientID(), msg.isCleanSession());
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
        setupAutoFlusher(descriptor.channel.pipeline(), flushIntervalMs);
        return true;
    }

    private void failedCredentials(Channel session) {
        ConnAckMessage okResp = new ConnAckMessage();
        okResp.setReturnCode(ConnAckMessage.BAD_USERNAME_OR_PASSWORD);
        session.writeAndFlush(okResp);
        LOG.info("Client {} failed to connect with bad username or password.", session);
    }

    private void setupAutoFlusher(ChannelPipeline pipeline, int flushIntervalMs) {
        AutoFlushHandler autoFlushHandler = new AutoFlushHandler(flushIntervalMs, TimeUnit.MILLISECONDS);
        try {
            pipeline.addAfter("idleEventHandler", "autoFlusher", autoFlushHandler);
        } catch (NoSuchElementException nseex) {
            //the idleEventHandler is not present on the pipeline
            pipeline.addFirst("autoFlusher", autoFlushHandler);
        }
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
        LOG.trace("republishStoredInSession for client <{}>", clientSession);
        List<IMessagesStore.StoredMessage> publishedEvents = clientSession.storedMessages();
        if (publishedEvents.isEmpty()) {
            LOG.info("No stored messages for client <{}>", clientSession.clientID);
            return;
        }

        LOG.info("republishing stored messages to client <{}>", clientSession.clientID);
        for (IMessagesStore.StoredMessage pubEvt : publishedEvents) {
            //put in flight zone
            LOG.trace("Adding to inflight <{}>", pubEvt.getMessageID());
            clientSession.inFlightAckWaiting(pubEvt.getGuid(), pubEvt.getMessageID());
            directSend(clientSession, pubEvt.getTopic(), pubEvt.getQos(),
                    pubEvt.getMessage(), false, pubEvt.getMessageID());
            clientSession.removeEnqueued(pubEvt.getGuid());
        }
    }

    public void processPubAck(Channel channel, PubAckMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        int messageID = msg.getMessageID();
        String username = NettyUtils.userName(channel);
        LOG.trace("retrieving inflight for messageID <{}>", messageID);

        //Remove the message from message store
        ClientSession targetSession = m_sessionsStore.sessionForClient(clientID);
        StoredMessage inflightMsg = targetSession.getInflightMessage(messageID);
        targetSession.inFlightAcknowledged(messageID);

        String topic = inflightMsg.getTopic();

        m_interceptor.notifyMessageAcknowledged(new InterceptAcknowledgedMessage(inflightMsg, topic, username));
    }

    private static IMessagesStore.StoredMessage asStoredMessage(PublishMessage msg) {
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
        LOG.info("PUB --PUBLISH--> SRV executePublish invoked with {}", msg);
        String clientID = NettyUtils.clientID(channel);
        final String topic = msg.getTopicName();
        //check if the topic can be wrote
        String username = NettyUtils.userName(channel);
        if (!m_authorizator.canWrite(topic, username, clientID)) {
            LOG.debug("topic {} doesn't have write credentials", topic);
            return;
        }
        final AbstractMessage.QOSType qos = msg.getQos();
        final Integer messageID = msg.getMessageID();
        LOG.info("PUBLISH on server {} from clientID <{}> on topic <{}> with QoS {}", m_server_port, clientID, topic, qos);

        MessageGUID guid = null;
        IMessagesStore.StoredMessage toStoreMsg = asStoredMessage(msg);
        toStoreMsg.setClientID(clientID);
        if (qos == AbstractMessage.QOSType.MOST_ONE) { //QoS0
            route2Subscribers(toStoreMsg);
        } else if (qos == AbstractMessage.QOSType.LEAST_ONE) { //QoS1
            route2Subscribers(toStoreMsg);
            if (msg.isLocal()) {
                sendPubAck(clientID, messageID);
            }
            LOG.info("server {} replying with PubAck to MSG ID {}", m_server_port, messageID);
        } else if (qos == AbstractMessage.QOSType.EXACTLY_ONCE) { //QoS2
            guid = m_messagesStore.storePublishForFuture(toStoreMsg);
            if (msg.isLocal()) {
                sendPubRec(clientID, messageID);
            }
            //Next the client will send us a pub rel
            //NB publish to subscribers for QoS 2 happen upon PUBREL from publisher
        }

        if (msg.isRetainFlag()) {
            if (qos == AbstractMessage.QOSType.MOST_ONE) {
                //QoS == 0 && retain => clean old retained
                m_messagesStore.cleanRetained(topic);
            } else {
                if (!msg.getPayload().hasRemaining()) {
                    m_messagesStore.cleanRetained(topic);
                } else {
                    if (guid == null) {
                        //before wasn't stored
                        guid = m_messagesStore.storePublishForFuture(toStoreMsg);
                    }
                    m_messagesStore.storeRetained(topic, guid);
                }
            }
        }
        m_interceptor.notifyTopicPublished(msg, clientID, username);
    }

    /**
     * Intended usage is only for embedded versions of the broker, where the hosting application want to use the
     * broker to send a publish message.
     * Inspired by {@link #processPublish} but with some changes to avoid security check, and the handshake phases
     * for Qos1 and Qos2.
     * It also doesn't notifyTopicPublished because using internally the owner should already know where
     * it's publishing.
     * */
    public void internalPublish(PublishMessage msg) {
        final AbstractMessage.QOSType qos = msg.getQos();
        final String topic = msg.getTopicName();
        LOG.info("embedded PUBLISH on topic <{}> with QoS {}", topic, qos);

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
        route2Subscribers(toStoreMsg);

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
        route2Subscribers(tobeStored);
    }


    /**
     * Flood the subscribers with the message to notify. MessageID is optional and should only used for QoS 1 and 2
     * */
    void route2Subscribers(IMessagesStore.StoredMessage pubMsg) {
        final String topic = pubMsg.getTopic();
        final AbstractMessage.QOSType publishingQos = pubMsg.getQos();
        final ByteBuffer origMessage = pubMsg.getMessage();
        LOG.debug("route2Subscribers republishing to existing subscribers that matches the topic {}", topic);
        if (LOG.isTraceEnabled()) {
            LOG.trace("content <{}>", DebugUtils.payload2Str(origMessage));
            LOG.trace("subscription tree {}", subscriptions.dumpTree());
        }
        //if QoS 1 or 2 store the message
        MessageGUID guid = null;
        if (publishingQos == QOSType.EXACTLY_ONCE || publishingQos == QOSType.LEAST_ONE) {
            guid = m_messagesStore.storePublishForFuture(pubMsg);
        }

        List<Subscription> topicMatchingSubscriptions = subscriptions.matches(topic);
        LOG.trace("Found {} matching subscriptions to <{}>", topicMatchingSubscriptions.size(), topic);
        for (final Subscription sub : topicMatchingSubscriptions) {
            AbstractMessage.QOSType qos = publishingQos;
            if (qos.byteValue() > sub.getRequestedQos().byteValue()) {
                qos = sub.getRequestedQos();
            }
            ClientSession targetSession = m_sessionsStore.sessionForClient(sub.getClientId());

            boolean targetIsActive = this.connectionDescriptors.containsKey(sub.getClientId());

            LOG.debug("Broker republishing to client <{}> topic <{}> qos <{}>, active {}",
                    sub.getClientId(), sub.getTopicFilter(), qos, targetIsActive);
            ByteBuffer message = origMessage.duplicate();
            if (qos == AbstractMessage.QOSType.MOST_ONE && targetIsActive) {
                //QoS 0
                directSend(targetSession, topic, qos, message, false, null);
            } else {
                //QoS 1 or 2
                //if the target subscription is not clean session and is not connected => store it
                if (!targetSession.isCleanSession() && !targetIsActive) {
                    //store the message in targetSession queue to deliver
                    targetSession.enqueueToDeliver(guid);
                } else  {
                    //publish
                    if (targetIsActive) {
                        int messageId = targetSession.nextPacketId();
                        targetSession.inFlightAckWaiting(guid, messageId);
                        directSend(targetSession, topic, qos, message, false, messageId);
                    }
                }
            }
        }
    }

    protected void directSend(ClientSession clientsession, String topic, AbstractMessage.QOSType qos,
                              ByteBuffer message, boolean retained, Integer messageID) {
        String clientId = clientsession.clientID;
        LOG.debug("directSend invoked clientId <{}> on topic <{}> QoS {} retained {} messageID {}",
                clientId, topic, qos, retained, messageID);
        PublishMessage pubMessage = new PublishMessage();
        pubMessage.setRetainFlag(retained);
        pubMessage.setTopicName(topic);
        pubMessage.setQos(qos);
        pubMessage.setPayload(message);

        LOG.info("send publish message to <{}> on topic <{}>", clientId, topic);
        if (LOG.isDebugEnabled()) {
            LOG.debug("content <{}>", DebugUtils.payload2Str(message));
        }
        //set the PacketIdentifier only for QoS > 0
        if (pubMessage.getQos() != AbstractMessage.QOSType.MOST_ONE) {
            pubMessage.setMessageID(messageID);
        } else {
            if (messageID != null) {
                throw new RuntimeException("Internal bad error, trying to forwardPublish a QoS 0 message " +
                        "with PacketIdentifier: " + messageID);
            }
        }

        if (connectionDescriptors == null) {
            throw new RuntimeException("Internal bad error, found connectionDescriptors to null while it should be " +
                    "initialized, somewhere it's overwritten!!");
        }
        if (connectionDescriptors.get(clientId) == null) {
            //TODO while we were publishing to the target client, that client disconnected,
            // could happen is not an error HANDLE IT
            throw new RuntimeException(String.format("Can't find a ConnectionDescriptor for client <%s> in cache <%s>",
                    clientId, connectionDescriptors));
        }
        Channel channel = connectionDescriptors.get(clientId).channel;
        LOG.trace("Session for clientId {}", clientId);
        if (channel.isWritable()) {
            LOG.debug("channel is writable");
            //if channel is writable don't enqueue
            channel.writeAndFlush(pubMessage);
        } else {
            //enqueue to the client session
            LOG.debug("enqueue to client session");
            clientsession.enqueue(pubMessage);
        }
    }

    private void sendPubRec(String clientID, int messageID) {
        LOG.trace("PUB <--PUBREC-- SRV sendPubRec invoked for clientID {} with messageID {}", clientID, messageID);
        PubRecMessage pubRecMessage = new PubRecMessage();
        pubRecMessage.setMessageID(messageID);
        connectionDescriptors.get(clientID).channel.writeAndFlush(pubRecMessage);
    }

    private void sendPubAck(String clientId, int messageID) {
        LOG.trace("sendPubAck invoked");
        PubAckMessage pubAckMessage = new PubAckMessage();
        pubAckMessage.setMessageID(messageID);

        try {
            if (connectionDescriptors == null) {
                throw new RuntimeException("Internal bad error, found connectionDescriptors to null while it should be initialized, somewhere it's overwritten!!");
            }
            LOG.debug("clientIDs are {}", connectionDescriptors);
            if (connectionDescriptors.get(clientId) == null) {
                throw new RuntimeException(String.format("Can't find a ConnectionDescriptor for client %s in cache %s", clientId, connectionDescriptors));
            }
            connectionDescriptors.get(clientId).channel.writeAndFlush(pubAckMessage);
        } catch(Throwable t) {
            LOG.error(null, t);
        }
    }

    /**
     * Second phase of a publish QoS2 protocol, sent by publisher to the broker. Search the stored message and publish
     * to all interested subscribers.
     * */
    public void processPubRel(Channel channel, PubRelMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        int messageID = msg.getMessageID();
        LOG.debug("PUB --PUBREL--> SRV processPubRel invoked for clientID {} ad messageID {}", clientID, messageID);
        ClientSession targetSession = m_sessionsStore.sessionForClient(clientID);
        IMessagesStore.StoredMessage evt = targetSession.storedMessage(messageID);
        route2Subscribers(evt);

        if (evt.isRetained()) {
            final String topic = evt.getTopic();
            if (!evt.getMessage().hasRemaining()) {
                m_messagesStore.cleanRetained(topic);
            } else {
                m_messagesStore.storeRetained(topic, evt.getGuid());
            }
        }

        sendPubComp(clientID, messageID);
    }

    private void sendPubComp(String clientID, int messageID) {
        LOG.debug("PUB <--PUBCOMP-- SRV sendPubComp invoked for clientID {} ad messageID {}", clientID, messageID);
        PubCompMessage pubCompMessage = new PubCompMessage();
        pubCompMessage.setMessageID(messageID);

        connectionDescriptors.get(clientID).channel.writeAndFlush(pubCompMessage);
    }

    public void processPubRec(Channel channel, PubRecMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        ClientSession targetSession = m_sessionsStore.sessionForClient(clientID);
        //remove from the inflight and move to the QoS2 second phase queue
        int messageID = msg.getMessageID();
        targetSession.moveInFlightToSecondPhaseAckWaiting(messageID);
        //once received a PUBREC reply with a PUBREL(messageID)
        LOG.debug("\t\tSRV <--PUBREC-- SUB processPubRec invoked for clientID {} ad messageID {}", clientID, messageID);
        PubRelMessage pubRelMessage = new PubRelMessage();
        pubRelMessage.setMessageID(messageID);
        pubRelMessage.setQos(AbstractMessage.QOSType.LEAST_ONE);

        channel.writeAndFlush(pubRelMessage);
    }

    public void processPubComp(Channel channel, PubCompMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        int messageID = msg.getMessageID();
        LOG.debug("\t\tSRV <--PUBCOMP-- SUB processPubComp invoked for clientID {} ad messageID {}", clientID, messageID);
        //once received the PUBCOMP then remove the message from the temp memory
        ClientSession targetSession = m_sessionsStore.sessionForClient(clientID);
        StoredMessage inflightMsg = targetSession.secondPhaseAcknowledged(messageID);
        String username = NettyUtils.userName(channel);
        String topic = inflightMsg.getTopic();
        m_interceptor.notifyMessageAcknowledged(new InterceptAcknowledgedMessage(inflightMsg, topic, username));
    }

    public void processDisconnect(Channel channel) throws InterruptedException {
        channel.flush();
        final String clientID = NettyUtils.clientID(channel);
        final ConnectionDescriptor existingDescriptor = this.connectionDescriptors.get(clientID);
        if (existingDescriptor == null) {
            //another client with same ID removed the descriptor, we must exit
            channel.close();
            return;
        }

        if (existingDescriptor.channel != channel) {
            //another client saved it's descriptor, exit
            channel.close();
            return;
        }

        if (!removeSubscriptions(existingDescriptor, clientID)) {
            channel.close();
            return;
        }

        if (!dropStoredMessages(existingDescriptor, clientID)) {
            channel.close();
            return;
        }

        if (!cleanWillMessageAndNotifyInterceptor(existingDescriptor, clientID)) {
            channel.close();
            return;
        }

        if (!closeChannel(existingDescriptor)) {
            return;
        }

        boolean stillPresent = this.connectionDescriptors.remove(clientID, existingDescriptor);
        if (!stillPresent) {
            //another descriptor was inserted
            return;
        }

        LOG.info("DISCONNECT client <{}> finished", clientID);
    }

    private boolean removeSubscriptions(ConnectionDescriptor descriptor, String clientID) {
        final boolean success = descriptor.assignState(ConnectionState.ESTABLISHED, ConnectionState.SUBSCRIPTIONS_REMOVED);
        if (!success) {
            return false;
        }

        if (descriptor.cleanSession) {
            LOG.info("cleaning old saved subscriptions for client <{}>", clientID);
            m_sessionsStore.wipeSubscriptions(clientID);
            LOG.debug("Wiped subscriptions for client <{}>", clientID);
        }
        return true;
    }

    private boolean dropStoredMessages(ConnectionDescriptor descriptor, String clientID) {
        final boolean success = descriptor.assignState(ConnectionState.SUBSCRIPTIONS_REMOVED, ConnectionState.MESSAGES_DROPPED);
        if (!success) {
            return false;
        }

        if (descriptor.cleanSession) {
            LOG.debug("Removing messages in session for client <{}>", clientID);
            this.m_messagesStore.dropMessagesInSession(clientID);
            LOG.debug("Removed messages in session for client <{}>", clientID);
        }
        return true;
    }

    private boolean cleanWillMessageAndNotifyInterceptor(ConnectionDescriptor descriptor, String clientID) {
        final boolean success = descriptor.assignState(ConnectionState.MESSAGES_DROPPED, ConnectionState.INTERCEPTORS_NOTIFIED);
        if (!success) {
            return false;
        }

        //cleanup the will store
        m_willStore.remove(clientID);
        String username = NettyUtils.userName(descriptor.channel);
        m_interceptor.notifyClientDisconnected(clientID, username);
        return true;
    }

    private boolean closeChannel(ConnectionDescriptor descriptor) {
        final boolean success = descriptor.assignState(ConnectionState.INTERCEPTORS_NOTIFIED, ConnectionState.DISCONNECTED);
        if (!success) {
            return false;
        }
        descriptor.channel.close();
        return true;
    }

    public void processConnectionLost(String clientID, Channel channel) {
        ConnectionDescriptor oldConnDescr = new ConnectionDescriptor(clientID, channel, true);
        connectionDescriptors.remove(clientID, oldConnDescr);
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
     * doesn't reply any error
     */
    public void processUnsubscribe(Channel channel, UnsubscribeMessage msg) {
        List<String> topics = msg.topicFilters();
        String clientID = NettyUtils.clientID(channel);

        LOG.debug("UNSUBSCRIBE subscription on topics {} for clientID <{}>", topics, clientID);

        ClientSession clientSession = m_sessionsStore.sessionForClient(clientID);
        for (String topic : topics) {
            boolean validTopic = SubscriptionsStore.validate(topic);
            if (!validTopic) {
                //close the connection, not valid topicFilter is a protocol violation
                channel.close();
                LOG.warn("UNSUBSCRIBE found an invalid topic filter <{}> for clientID <{}>", topic, clientID);
                return;
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

        LOG.info("replying with UnsubAck to MSG ID {}", messageID);
        channel.writeAndFlush(ackMessage);
    }

    public void processSubscribe(Channel channel, SubscribeMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        LOG.info("SUBSCRIBE client <{}>", clientID);
        int messageID = msg.getMessageID();
        LOG.debug("SUBSCRIBE client <{}> on server {} packetID {}", clientID, m_server_port, messageID);

        RunningSubscription executionKey = new RunningSubscription(clientID, messageID);
        SubscriptionState currentStatus = this.subscriptionInCourse.putIfAbsent(executionKey, SubscriptionState.VERIFIED);
        if (currentStatus != null) {
            LOG.debug("The client <{}> sent another SUBSCRIBE while this one was processing", clientID);
            return;
        }
        String username = NettyUtils.userName(channel);
        List<SubscribeMessage.Couple> ackTopics = doVerify(clientID, username, msg);
        SubAckMessage ackMessage = doAckMessageFromValidateFilters(ackTopics);
        if (!this.subscriptionInCourse.replace(executionKey, SubscriptionState.VERIFIED, SubscriptionState.STORED)) {
            LOG.debug("The client {} sent another SUBSCRIBE while this one was verifing topicFilters");
            return;
        }

        ackMessage.setMessageID(messageID);
        List<Subscription> newSubscriptions = doStoreSubscription(ackTopics, clientID);

        //save session, persist subscriptions from session
        LOG.debug("SUBACK for packetID {}", messageID);
        if (LOG.isTraceEnabled()) {
            LOG.trace("subscription tree {}", subscriptions.dumpTree());
        }

        for (Subscription subscription : newSubscriptions) {
            LOG.debug("Persisting subscription {}", subscription);
            subscriptions.add(subscription.asClientTopicCouple());
        }
        channel.writeAndFlush(ackMessage);

        //fire the persisted messages in session
        for (Subscription subscription : newSubscriptions) {
            publishStoredMessagesInSession(subscription, username);
        }

        boolean success = this.subscriptionInCourse.remove(executionKey, SubscriptionState.STORED);
        if (!success) {
            LOG.warn("Failed to remove the descriptor, something bad happened");
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
                LOG.debug("topic {} doesn't have read credentials", req.topicFilter);
                ackTopics.add(new SubscribeMessage.Couple(AbstractMessage.QOSType.FAILURE.byteValue(), req.topicFilter));
            } else {
                boolean validTopic = SubscriptionsStore.validate(req.topicFilter);
                AbstractMessage.QOSType qos = validTopic ?
                        AbstractMessage.QOSType.valueOf(req.qos)
                        : AbstractMessage.QOSType.FAILURE;
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



    private void publishStoredMessagesInSession(final Subscription newSubscription, String username) {
        LOG.debug("Publish persisted messages in session {}", newSubscription);

        //scans retained messages to be published to the new subscription
        //TODO this is ugly, it does a linear scan on potential big dataset
        Collection<IMessagesStore.StoredMessage> messages = m_messagesStore.searchMatching(new IMatchingCondition() {
            @Override
            public boolean match(String key) {
                return SubscriptionsStore.matchTopics(key, newSubscription.getTopicFilter());
            }
        });

        LOG.debug("Found {} messages to republish", messages.size());
        ClientSession targetSession = m_sessionsStore.sessionForClient(newSubscription.getClientId());
        for (IMessagesStore.StoredMessage storedMsg : messages) {
            //fire as retained the message
            LOG.trace("send publish message for topic {}", newSubscription.getTopicFilter());
            Integer packetID = storedMsg.getQos() == QOSType.MOST_ONE ? null : targetSession.nextPacketId();
            if (packetID != null) {
                LOG.trace("Adding to inflight <{}>", packetID);
                targetSession.inFlightAckWaiting(storedMsg.getGuid(), packetID);
            }
            directSend(targetSession, storedMsg.getTopic(), storedMsg.getQos(), storedMsg.getPayload(), true, packetID);
        }

        //notify the Observables
        m_interceptor.notifyTopicSubscribed(newSubscription, username);
    }

    public void notifyChannelWritable(Channel channel) {
        String clientID = NettyUtils.clientID(channel);
        ClientSession clientSession = m_sessionsStore.sessionForClient(clientID);
        boolean emptyQueue = false;
        while (channel.isWritable()  && !emptyQueue) {
            AbstractMessage msg = clientSession.dequeue();
            if (msg == null) {
                emptyQueue = true;
            } else {
                channel.write(msg);
            }
        }
        channel.flush();
    }

    public boolean addInterceptHandler(InterceptHandler interceptHandler) {
        return this.m_interceptor.addInterceptHandler(interceptHandler);
    }

    public boolean removeInterceptHandler(InterceptHandler interceptHandler) {
        return this.m_interceptor.removeInterceptHandler(interceptHandler);
    }
}
