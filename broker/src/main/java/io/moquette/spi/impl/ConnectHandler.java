package io.moquette.spi.impl;

import io.moquette.connections.IConnectionsManager;
import io.moquette.server.ConnectionDescriptor;
import io.moquette.server.netty.AutoFlushHandler;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.ClientSession;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.subscriptions.ISubscriptionsDirectory;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizatorPolicy;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.mqtt.*;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.moquette.server.ConnectionDescriptor.ConnectionState.*;
import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;
import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.*;

public class ConnectHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectHandler.class);

    private IConnectionsManager connectionDescriptors;
    private BrokerInterceptor m_interceptor;
    private SessionsRepository sessionsRepository;
    private ISessionsStore m_sessionsStore;
    private IAuthenticator m_authenticator;
    private IAuthorizatorPolicy m_authorizator;
    private ISubscriptionsDirectory subscriptions;
    private boolean allowAnonymous;
    private boolean allowZeroByteClientId;
    private boolean reauthorizeSubscriptionsOnConnect;
    private InternalRepublisher internalRepublisher;

    ConnectHandler(IConnectionsManager connectedClients, BrokerInterceptor interceptor, SessionsRepository sessionsRepository,
                   ISessionsStore sessionsStore, IAuthenticator authenticator, IAuthorizatorPolicy authorizator,
                   ISubscriptionsDirectory subscriptions, boolean allowAnonymous, boolean allowZeroByteClientId,
                   boolean reauthorizeSubscriptionsOnConnect, InternalRepublisher internalRepublisher) {
        this.connectionDescriptors = connectedClients;
        this.m_interceptor = interceptor;
        this.sessionsRepository = sessionsRepository;
        this.m_sessionsStore = sessionsStore;
        this.m_authenticator = authenticator;
        this.m_authorizator = authorizator;
        this.subscriptions = subscriptions;
        this.allowAnonymous = allowAnonymous;
        this.allowZeroByteClientId = allowZeroByteClientId;
        this.reauthorizeSubscriptionsOnConnect = reauthorizeSubscriptionsOnConnect;
        this.internalRepublisher = internalRepublisher;
    }

    public void processConnect(Channel channel, MqttConnectMessage msg) {
        MqttConnectPayload payload = msg.payload();
        String clientId = payload.clientIdentifier();
        final String username = payload.userName();
        LOG.debug("Processing CONNECT message. CId={}, username={}", clientId, username);

        if (isNotProtocolVersion(msg, MqttVersion.MQTT_3_1) && isNotProtocolVersion(msg, MqttVersion.MQTT_3_1_1)) {
            MqttConnAckMessage badProto = connAck(CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);

            LOG.error("MQTT protocol version is not valid. CId={}", clientId);
            channel.writeAndFlush(badProto).addListener(FIRE_EXCEPTION_ON_FAILURE);
            channel.close().addListener(CLOSE_ON_FAILURE);
            return;
        }

        final boolean cleanSession = msg.variableHeader().isCleanSession();
        if (clientId == null || clientId.length() == 0) {
            if (!cleanSession || !this.allowZeroByteClientId) {
                MqttConnAckMessage badId = connAck(CONNECTION_REFUSED_IDENTIFIER_REJECTED);

                channel.writeAndFlush(badId).addListener(FIRE_EXCEPTION_ON_FAILURE);
                channel.close().addListener(CLOSE_ON_FAILURE);
                LOG.error("MQTT client ID cannot be empty. Username={}", username);
                return;
            }

            // Generating client id.
            clientId = UUID.randomUUID().toString().replace("-", "");
            LOG.info("Client has connected with server generated id={}, username={}", clientId, username);
        }

        if (!login(channel, msg, clientId)) {
            channel.close().addListener(CLOSE_ON_FAILURE);
            return;
        }

        ConnectionDescriptor descriptor = new ConnectionDescriptor(clientId, channel, cleanSession);
        final ConnectionDescriptor existing = this.connectionDescriptors.addConnection(descriptor);
        if (existing != null) {
            LOG.info("Client ID is being used in an existing connection, force to be closed. CId={}", clientId);
            existing.abort();
            //return;
            this.connectionDescriptors.removeConnection(existing);
            this.connectionDescriptors.addConnection(descriptor);
        }

        if (!descriptor.assignState(DISCONNECTED, INIT_SESSION)) {
            channel.close().addListener(CLOSE_ON_FAILURE);
            return;
        }

        LOG.debug("Initializing client session {}", clientId);
        ClientSession existingSession = this.sessionsRepository.sessionForClient(clientId);
        boolean isSessionAlreadyStored = existingSession != null;
        final boolean msgCleanSessionFlag = msg.variableHeader().isCleanSession();
        if (isSessionAlreadyStored && msgCleanSessionFlag) {
            for (Subscription existingSub : existingSession.getSubscriptions()) {
                this.subscriptions.removeSubscription(existingSub.getTopicFilter(), clientId);
            }
        }

        initializeKeepAliveTimeout(channel, msg, clientId);
        final ClientSession clientSession = this.sessionsRepository.createOrLoadClientSession(clientId, cleanSession);
        clientSession.storeWillMessage(msg, clientId);
        int flushIntervalMs = 500/* (keepAlive * 1000) / 2 */;
        setupAutoFlusher(channel, flushIntervalMs);

        if (!cleanSession && reauthorizeSubscriptionsOnConnect) {
            reauthorizeOnExistingSubscriptions(clientId, username);
        }

        if (!descriptor.assignState(INIT_SESSION, SENDACK)) {
            channel.close().addListener(CLOSE_ON_FAILURE);
            return;
        }
        LOG.debug("Sending CONNACK. CId={}", clientId);
        MqttConnAckMessage okResp = createConnectAck(msg, clientId);

        final String connectClientId = clientId;

        descriptor.writeAndFlush(okResp, new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    LOG.debug("CONNACK has been sent. CId={}", connectClientId);

                    if (!descriptor.assignState(SENDACK, MESSAGES_REPUBLISHED)) {
                        channel.close().addListener(CLOSE_ON_FAILURE);
                        return;
                    }
                    m_interceptor.notifyClientConnected(msg);
                    if (!msg.variableHeader().isCleanSession()) {
                        // force the republish of stored QoS1 and QoS2
                        internalRepublisher.publishStored(clientSession);
                    }

                    if (!descriptor.assignState(MESSAGES_REPUBLISHED, ESTABLISHED)) {
                        channel.close().addListener(CLOSE_ON_FAILURE);
                    }

                    LOG.info("Connected client <{}> with login <{}>", connectClientId, username);
                } else {
                    future.channel().pipeline().fireExceptionCaught(future.cause());
                }
            }
        });
    }

    private boolean isNotProtocolVersion(MqttConnectMessage msg, MqttVersion version) {
        return msg.variableHeader().version() != version.protocolLevel();
    }

    private void reauthorizeOnExistingSubscriptions(String clientId, String username) {
        if (!m_sessionsStore.contains(clientId)) {
            return;
        }
        final Collection<Subscription> clientSubscriptions = m_sessionsStore.subscriptionStore()
            .listClientSubscriptions(clientId);
        for (Subscription sub : clientSubscriptions) {
            final Topic topicToReauthorize = sub.getTopicFilter();
            final boolean readAuthorized = m_authorizator.canRead(topicToReauthorize, username, clientId);
            if (!readAuthorized) {
                subscriptions.removeSubscription(topicToReauthorize, clientId);
            }
        }
    }

    private void setupAutoFlusher(Channel channel, int flushIntervalMs) {
        try {
            channel.pipeline().addAfter(
                "idleEventHandler",
                "autoFlusher",
                new AutoFlushHandler(flushIntervalMs, TimeUnit.MILLISECONDS));
        } catch (NoSuchElementException nseex) {
            // the idleEventHandler is not present on the pipeline
            channel.pipeline()
                .addFirst("autoFlusher", new AutoFlushHandler(flushIntervalMs, TimeUnit.MILLISECONDS));
        }
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
                pwd = msg.payload().password().getBytes(StandardCharsets.UTF_8);
            } else if (!this.allowAnonymous) {
                LOG.error("Client didn't supply any password and MQTT anonymous mode is disabled CId={}", clientId);
                failedCredentials(channel);
                return false;
            }
            final String login = msg.payload().userName();
            if (!m_authenticator.checkValid(clientId, login, pwd)) {
                LOG.error("Authenticator has rejected the MQTT credentials CId={}, username={}", clientId, login);
                failedCredentials(channel);
                return false;
            }
            NettyUtils.userName(channel, login);
        } else if (!this.allowAnonymous) {
            LOG.error("Client didn't supply any credentials and MQTT anonymous mode is disabled. CId={}", clientId);
            failedCredentials(channel);
            return false;
        }
        return true;
    }

    private MqttConnAckMessage createConnectAck(MqttConnectMessage msg, String clientId) {
        MqttConnAckMessage okResp;
        ClientSession clientSession = this.sessionsRepository.sessionForClient(clientId);
        boolean isSessionAlreadyStored = clientSession != null;
        final boolean msgCleanSessionFlag = msg.variableHeader().isCleanSession();
        if (!msgCleanSessionFlag && isSessionAlreadyStored) {
            okResp = connAckWithSessionPresent(CONNECTION_ACCEPTED);
        } else {
            okResp = connAck(CONNECTION_ACCEPTED);
        }
        return okResp;
    }

    private void initializeKeepAliveTimeout(Channel channel, MqttConnectMessage msg, String clientId) {
        int keepAlive = msg.variableHeader().keepAliveTimeSeconds();
        NettyUtils.keepAlive(channel, keepAlive);
        NettyUtils.cleanSession(channel, msg.variableHeader().isCleanSession());
        NettyUtils.clientID(channel, clientId);
        int idleTime = Math.round(keepAlive * 1.5f);
        setIdleTime(channel.pipeline(), idleTime);

        LOG.debug("Connection has been configured CId={}, keepAlive={}, removeTemporaryQoS2={}, idleTime={}",
            clientId, keepAlive, msg.variableHeader().isCleanSession(), idleTime);
    }

    private void failedCredentials(Channel session) {
        session.writeAndFlush(connAck(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD))
            .addListener(FIRE_EXCEPTION_ON_FAILURE);
        LOG.info("Client {} failed to connect with bad username or password.", session);
    }

    private void setIdleTime(ChannelPipeline pipeline, int idleTime) {
        if (pipeline.names().contains("idleStateHandler")) {
            pipeline.remove("idleStateHandler");
        }
        pipeline.addFirst("idleStateHandler", new IdleStateHandler(idleTime, 0, 0));
    }
}
