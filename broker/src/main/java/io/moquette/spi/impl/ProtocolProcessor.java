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

import cn.wildfirechat.proto.WFCMessage;
import io.moquette.persistence.RPCCenter;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptAcknowledgedMessage;
import io.moquette.persistence.MemorySessionStore;
import io.moquette.persistence.TargetEntry;
import io.moquette.server.ConnectionDescriptor;
import io.moquette.server.ConnectionDescriptorStore;
import io.moquette.server.Server;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.*;
import io.moquette.spi.IMessagesStore.StoredMessage;
import io.moquette.spi.impl.security.AES;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizator;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.liyufan.im.Utility;

import java.util.*;

import static io.moquette.spi.impl.InternalRepublisher.createPublishForQos;
import static io.moquette.spi.impl.Utils.readBytesAndRewind;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.*;
import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.*;
import static io.moquette.server.ConnectionDescriptor.ConnectionState.*;

/**
 * Class responsible to handle the logic of MQTT protocol it's the director of the protocol
 * execution.
 *
 * Used by the front facing class ProtocolProcessorBootstrapper.
 */
public class ProtocolProcessor {
    private void handleTargetRemovedFromCurrentNode(TargetEntry target) {
        System.out.println("kickof user " + target);
        if (target.type == TargetEntry.Type.TARGET_TYPE_USER) {
            Collection<MemorySessionStore.Session> sessions = m_sessionsStore.sessionForUser(target.target);
            for (MemorySessionStore.Session session : sessions) {
                ConnectionDescriptor descriptor = connectionDescriptors.getConnection(session.getClientID());
                try {
                    if (descriptor != null) {
                        processDisconnect(descriptor.getChannel(), true);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Utility.printExecption(LOG, e);
                }
            }
        } else if(target.type == TargetEntry.Type.TARGET_TYPE_CHATROOM) {

        }
    }


    private static final Logger LOG = LoggerFactory.getLogger(ProtocolProcessor.class);

    protected ConnectionDescriptorStore connectionDescriptors;

    private boolean allowAnonymous;
    private boolean allowZeroByteClientId;
    private IAuthorizator m_authorizator;

    private IMessagesStore m_messagesStore;

    private ISessionsStore m_sessionsStore;

    private IAuthenticator m_authenticator;
    private BrokerInterceptor m_interceptor;

    public Qos1PublishHandler qos1PublishHandler;
    private MessagesPublisher messagesPublisher;

    private Server mServer;

    ProtocolProcessor() {

    }

    /**
     * @param storageService
     *            the persistent store to use for save/load of messages for QoS1 and QoS2 handling.
     * @param sessionsStore
     *            the clients sessions store, used to persist subscriptions.
     * @param authenticator
     *            true to allow clients connect without a clientid
     * @param authorizator
     *            used to apply ACL policies to publishes and subscriptions.
     * @param interceptor
     *            to notify events to an intercept handler
     */
    void init(ConnectionDescriptorStore connectionDescriptors,
              IMessagesStore storageService, ISessionsStore sessionsStore, IAuthenticator authenticator, IAuthorizator authorizator,
              BrokerInterceptor interceptor, Server server) {
        LOG.info("Initializing MQTT protocol processor...");
        this.connectionDescriptors = connectionDescriptors;
        this.m_interceptor = interceptor;
        this.allowAnonymous = false;
        this.allowZeroByteClientId = false;
        m_authorizator = authorizator;
        m_authenticator = authenticator;
        m_messagesStore = storageService;
        m_sessionsStore = sessionsStore;

        LOG.info("Initializing messages publisher...");
        final PersistentQueueMessageSender messageSender = new PersistentQueueMessageSender(this.connectionDescriptors);
        this.messagesPublisher = new MessagesPublisher(connectionDescriptors, sessionsStore, messageSender, server.getHazelcastInstance(), m_messagesStore);

        LOG.info("Initializing QoS publish handlers...");
        this.qos1PublishHandler = new Qos1PublishHandler(m_authorizator, m_messagesStore, m_interceptor,
                this.connectionDescriptors, this.messagesPublisher, sessionsStore, server.getImBusinessScheduler(), server);

        mServer = server;
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

        if (clientId == null || clientId.length() == 0) {
            MqttConnAckMessage badId = connAck(CONNECTION_REFUSED_IDENTIFIER_REJECTED);

            channel.writeAndFlush(badId);
            channel.close();
            LOG.error("The MQTT client ID cannot be empty. Username={}", payload.userName());
            return;
        }


        if (!login(channel, msg, clientId)) {
            channel.close();
            return;
        }
        if (!mServer.m_initialized) {
            channel.close();
            return;
        }

        ConnectionDescriptor descriptor = new ConnectionDescriptor(clientId, channel);
        ConnectionDescriptor existing = this.connectionDescriptors.addConnection(descriptor);
        if (existing != null) {
            LOG.info("The client ID is being used in an existing connection. It will be closed. CId={}", clientId);
            this.connectionDescriptors.removeConnection(existing);
            existing.abort();
            this.connectionDescriptors.addConnection(descriptor);
        }

        initializeKeepAliveTimeout(channel, msg, clientId);
        if (!sendAck(descriptor, msg, clientId)) {
            channel.close();
            return;
        }

        m_interceptor.notifyClientConnected(msg);

        final ClientSession clientSession = createOrLoadClientSession(payload.userName(), descriptor, msg, clientId);
        if (clientSession == null) {
            MqttConnAckMessage badId = connAck(CONNECTION_REFUSED_SESSION_NOT_EXIST);

            channel.writeAndFlush(badId);
            channel.close();
            return;
        }


        int flushIntervalMs = 500/* (keepAlive * 1000) / 2 */;
        descriptor.setupAutoFlusher(flushIntervalMs);

        final boolean success = descriptor.assignState(SESSION_CREATED, ESTABLISHED);
        if (!success) {
            channel.close();
            return;
        }
        MemorySessionStore.Session session = m_sessionsStore.getSession(clientId);
        if(session != null) {
            session.refreshLastActiveTime();
        }

        LOG.info("The CONNECT message has been processed. CId={}, username={}", clientId, payload.userName());
    }

    private MqttConnAckMessage connAck(MqttConnectReturnCode returnCode) {
        return connAck(returnCode, false);
    }

    private MqttConnAckMessage connAck(MqttConnectReturnCode returnCode, boolean sessionPresent) {
        return connAck(returnCode, sessionPresent, null);
    }

    private MqttConnAckMessage connAck(MqttConnectReturnCode returnCode, byte[] data) {
        return connAck(returnCode, false, data);
    }


    private MqttConnAckMessage connAckWithSessionPresent(MqttConnectReturnCode returnCode, byte[] data) {
        return connAck(returnCode, true, data);
    }


    private MqttConnAckMessage connAck(MqttConnectReturnCode returnCode, boolean sessionPresent, byte[] data) {
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE,
            false, 0);
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(returnCode, sessionPresent);
        return new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader, new MqttConnectAckPayload(data));
    }
    private boolean login(Channel channel, MqttConnectMessage msg, final String clientId) {
        // handle user authentication
        if (msg.variableHeader().hasUserName()) {
            int status = m_messagesStore.getUserStatus(msg.payload().userName());
            if (status == 2) {
                failedBlocked(channel);
                return false;
            }
            byte[] pwd = null;
            if (msg.variableHeader().hasPassword()) {
                pwd = msg.payload().password();

                MemorySessionStore.Session session = m_sessionsStore.getSession(clientId);
                if (session != null && session.getUsername().equals(msg.payload().userName())) {
                    pwd = AES.AESDecrypt(pwd, session.getSecret(), true);
                } else {
                    LOG.error("Password decrypt failed of client {}", clientId);
                    failedCredentials(channel);
                    return false;
                }

                if (pwd == null) {
                    LOG.error("Password decrypt failed of client {}", clientId);
                    failedCredentials(channel);
                    return false;
                }
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
        LOG.info("Sending connect ACK. CId={}", clientId);
        final boolean success = descriptor.assignState(DISCONNECTED, SENDACK);
        if (!success) {
            return false;
        }

        MqttConnAckMessage okResp;
        ClientSession clientSession = m_sessionsStore.sessionForClient(clientId);
        boolean isSessionAlreadyStored = clientSession != null;

        String user = msg.payload().userName();
        long messageHead = m_messagesStore.getMessageHead(user);
        long friendHead = m_messagesStore.getFriendHead(user);
        long friendRqHead = m_messagesStore.getFriendRqHead(user);
        long settingHead = m_messagesStore.getSettingHead(user);
        WFCMessage.ConnectAckPayload payload = WFCMessage.ConnectAckPayload.newBuilder()
            .setMsgHead(messageHead)
            .setFriendHead(friendHead)
            .setFriendRqHead(friendRqHead)
            .setSettingHead(settingHead)
            .setServerTime(System.currentTimeMillis())
            .build();


        if (!msg.variableHeader().isCleanSession() && isSessionAlreadyStored) {
            okResp = connAckWithSessionPresent(CONNECTION_ACCEPTED, payload.toByteArray());
        } else {
            okResp = connAck(CONNECTION_ACCEPTED, payload.toByteArray());
        }

        descriptor.writeAndFlush(okResp);
        LOG.info("The connect ACK has been sent. CId={}", clientId);
        return true;
    }

    private void initializeKeepAliveTimeout(Channel channel, MqttConnectMessage msg, final String clientId) {
        int keepAlive = msg.variableHeader().keepAliveTimeSeconds();
        LOG.info("Configuring connection. CId={}", clientId);
        NettyUtils.keepAlive(channel, keepAlive);
        // session.attr(NettyUtils.ATTR_KEY_CLEANSESSION).set(msg.variableHeader().isCleanSession());
        NettyUtils.cleanSession(channel, msg.variableHeader().isCleanSession());
        // used to track the client in the subscription and publishing phases.
        // session.attr(NettyUtils.ATTR_KEY_CLIENTID).set(msg.getClientID());
        NettyUtils.clientID(channel, clientId);
        int idleTime = Math.round(keepAlive * 1.5f);
        setIdleTime(channel.pipeline(), idleTime);

        LOG.debug("The connection has been configured CId={}, keepAlive={}, cleanSession={}, idleTime={}",
                clientId, keepAlive, msg.variableHeader().isCleanSession(), idleTime);
    }

    private ClientSession createOrLoadClientSession(String username, ConnectionDescriptor descriptor, MqttConnectMessage msg,
            String clientId) {
        final boolean success = descriptor.assignState(SENDACK, SESSION_CREATED);
        if (!success) {
            return null;
        }

        m_sessionsStore.loadUserSession(username, clientId);
        ClientSession clientSession = m_sessionsStore.sessionForClient(clientId);
        boolean isSessionAlreadyStored = clientSession != null;
        if (isSessionAlreadyStored) {
            clientSession = m_sessionsStore.updateExistSession(username, clientId, null, msg.variableHeader().isCleanSession());
        } else {
            return null;
        }

        return clientSession;
    }

    private void failedBlocked(Channel session) {
        session.writeAndFlush(connAck(CONNECTION_REFUSED_IDENTIFIER_REJECTED));
        LOG.info("Client {} failed to connect, use is blocked.", session);
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

        ClientSession targetSession = m_sessionsStore.sessionForClient(clientID);
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

    public void processPublish(Channel channel, MqttPublishMessage msg) {
        final MqttQoS qos = msg.fixedHeader().qosLevel();
        final String clientId = NettyUtils.clientID(channel);
        
        LOG.info("Processing PUBLISH message. CId={}, topic={}, messageId={}, qos={}", clientId,
                msg.variableHeader().topicName(), msg.variableHeader().packetId(), qos);
        switch (qos) {
            case AT_MOST_ONCE:
                //not support
                break;
            case AT_LEAST_ONCE:
                this.qos1PublishHandler.receivedPublishQos1(channel, msg);
                break;
            case EXACTLY_ONCE:
                //not use
                break;
            default:
                LOG.error("Unknown QoS-Type:{}", qos);
                break;
        }
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
        //not use
    }

    public void processPubRec(Channel channel, MqttMessage msg) {
        //not use
    }

    public void processPubComp(Channel channel, MqttMessage msg) {
       //not use
    }

    public void processDisconnect(Channel channel, boolean clearSession) throws InterruptedException {
        final String clientID = NettyUtils.clientID(channel);
        LOG.info("Processing DISCONNECT message. CId={}, clearSession={}", clientID, clearSession);
        channel.flush();

        if (clientID == null) {
            LOG.error("Error. Cid not exist!!!", clientID, clearSession);
            channel.close();
            return;
        }

        if (!clearSession) {
            processConnectionLost(clientID, channel);
            return;
        }


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


        if (!dropStoredMessages(existingDescriptor, clientID)) {
            LOG.warn("Unable to drop stored messages. Closing connection. CId={}", clientID);
            existingDescriptor.abort();
            return;
        }

        if (!notifyInterceptorDisconnected(existingDescriptor, clientID)) {
            LOG.warn("Unable to drop will message. Closing connection. CId={}", clientID);
            existingDescriptor.abort();
            return;
        }

        if (!existingDescriptor.close()) {
            LOG.info("The connection has been closed. CId={}", clientID);
            return;
        }

        boolean stillPresent = this.connectionDescriptors.removeConnection(existingDescriptor);
        if (!stillPresent) {
            // another descriptor was inserted
            LOG.warn("Another descriptor has been inserted. CId={}", clientID);
            return;
        }

        LOG.info("The DISCONNECT message has been processed. CId={}", clientID);

        //disconnect the session
        m_sessionsStore.sessionForClient(clientID).disconnect(clearSession);
    }


    private boolean dropStoredMessages(ConnectionDescriptor descriptor, String clientID) {
        final boolean success = descriptor.assignState(ESTABLISHED, MESSAGES_DROPPED);
        if (!success) {
            return false;
        }

        LOG.debug("Removing messages of session. CId={}", descriptor.clientID);
        this.m_sessionsStore.dropQueue(clientID);
        LOG.debug("The messages of the session have been removed. CId={}", descriptor.clientID);

        return true;
    }

    private boolean notifyInterceptorDisconnected(ConnectionDescriptor descriptor, String clientID) {
        final boolean success = descriptor.assignState(MESSAGES_DROPPED, INTERCEPTORS_NOTIFIED);
        if (!success) {
            return false;
        }

        LOG.info("Removing will message. ClientId={}", descriptor.clientID);
        // cleanup the will store
        String username = descriptor.getUsername();
        m_interceptor.notifyClientDisconnected(clientID, username);
        return true;
    }

    public void processConnectionLost(String clientID, Channel channel) {
        LOG.info("Processing connection lost event. CId={}", clientID);
        ConnectionDescriptor oldConnDescr = new ConnectionDescriptor(clientID, channel);
        if(connectionDescriptors.removeConnection(oldConnDescr)) {
            MemorySessionStore.Session session = m_sessionsStore.getSession(clientID);
            if(session != null) {
                session.refreshLastActiveTime();
            }
            String username = NettyUtils.userName(channel);
            m_interceptor.notifyClientConnectionLost(clientID, username);
        }
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
        //Not use
    }

    public void processSubscribe(Channel channel, MqttSubscribeMessage msg) {
        //not use
    }


    /**
     * Create the SUBACK response from a list of topicFilters
     */
    private MqttSubAckMessage doAckMessageFromValidateFilters(List<MqttTopicSubscription> topicFilters, int messageId) {
        List<Integer> grantedQoSLevels = new ArrayList<>();
        for (MqttTopicSubscription req : topicFilters) {
            grantedQoSLevels.add(req.qualityOfService().value());
        }

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, AT_LEAST_ONCE, false, 0);
        MqttSubAckPayload payload = new MqttSubAckPayload(grantedQoSLevels);
        return new MqttSubAckMessage(fixedHeader, from(messageId), payload);
    }

    public void notifyChannelWritable(Channel channel) {
        String clientID = NettyUtils.clientID(channel);
        ClientSession clientSession = m_sessionsStore.sessionForClient(clientID);
        boolean emptyQueue = false;
        while (channel.isWritable() && !emptyQueue) {
            StoredMessage msg = clientSession.queue().poll();
            if (msg == null) {
                emptyQueue = true;
            } else {
                // recreate a publish from stored publish in queue
                MqttPublishMessage pubMsg = createPublishForQos( msg.getTopic(), msg.getQos(), msg.getPayload(),
                        msg.isRetained(), 0);
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

    public void onRpcMsg(String fromUser, String clientId, byte[] message, int messageId, String from, String request, boolean isAdmin) {
        if(request.equals(RPCCenter.KICKOFF_USER_REQUEST)) {
            mServer.getImBusinessScheduler().execute(()->handleTargetRemovedFromCurrentNode(new TargetEntry(TargetEntry.Type.TARGET_TYPE_USER, from)));
            return;
        }
        qos1PublishHandler.onRpcMsg(fromUser, clientId, message, messageId, from, request, isAdmin);
    }
    public void shutdown() {
        messagesPublisher.stopChatroomScheduler();
    }
}
