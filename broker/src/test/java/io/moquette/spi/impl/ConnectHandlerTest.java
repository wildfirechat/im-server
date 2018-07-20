package io.moquette.spi.impl;

import io.moquette.persistence.MemoryStorageService;
import io.moquette.server.ConnectionDescriptorStore;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.ClientSession;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.security.PermitAllAuthorizator;
import io.moquette.spi.impl.subscriptions.CTrieSubscriptionDirectory;
import io.moquette.spi.impl.subscriptions.ISubscriptionsDirectory;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttVersion;
import org.junit.Before;
import org.junit.Test;

import static io.moquette.spi.impl.NettyChannelAssertions.assertEqualsConnAck;
import static io.moquette.spi.impl.ProtocolProcessorTest.*;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.*;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ConnectHandlerTest {

    EmbeddedChannel session;
    MqttMessageBuilders.ConnectBuilder connMsg;

    IMessagesStore m_messagesStore;
    ISessionsStore m_sessionStore;
    ISubscriptionsDirectory subscriptions;
    MockAuthenticator m_mockAuthenticator;
    private ConnectHandler sut;
    private DisconnectHandler disconnectHandler;
    private SessionsRepository sessionsRepository;

    @Before
    public void setUp() {
        connMsg = MqttMessageBuilders.connect().protocolVersion(MqttVersion.MQTT_3_1).cleanSession(true);

        session = new EmbeddedChannel();

        MemoryStorageService memStorage = new MemoryStorageService(null, null);
        m_messagesStore = memStorage.messagesStore();
        m_sessionStore = memStorage.sessionsStore();
        m_mockAuthenticator = new MockAuthenticator(singleton(FAKE_CLIENT_ID), singletonMap(TEST_USER, TEST_PWD));

        subscriptions = new CTrieSubscriptionDirectory();
        sessionsRepository = new SessionsRepository(m_sessionStore, null);
        subscriptions.init(sessionsRepository);

        final ConnectionDescriptorStore connectionsRegistry = new ConnectionDescriptorStore();
        sut = new ConnectHandler(connectionsRegistry, NO_OBSERVERS_INTERCEPTOR, sessionsRepository, m_sessionStore, m_mockAuthenticator,
            new PermitAllAuthorizator(), subscriptions, true, true, false, null);

        disconnectHandler = new DisconnectHandler(connectionsRegistry, sessionsRepository,
                                                  subscriptions, NO_OBSERVERS_INTERCEPTOR);
    }

    @Test
    public void testZeroByteClientIdWithCleanSession() {
        // Allow zero byte client ids
        sut = new ConnectHandler(new ConnectionDescriptorStore(), NO_OBSERVERS_INTERCEPTOR, new SessionsRepository(this.m_sessionStore, null), m_sessionStore, m_mockAuthenticator,
            new PermitAllAuthorizator(), subscriptions, true, true, false, null);

        // Connect message with clean session set to true and client id is null.
        MqttConnectMessage msg = connMsg.clientId(null)
            .protocolVersion(MqttVersion.MQTT_3_1_1)
            .cleanSession(true)
            .build();

        sut.processConnect(session, msg);
        assertEqualsConnAck("Connection MUST be accepted, unique clientid MUST be generated and clean session to true",
            CONNECTION_ACCEPTED, session.readOutbound());
        assertTrue("Connection should be valid and open,", session.isOpen());
    }

    @Test
    public void invalidAuthentication() {
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID)
            .username(TEST_USER + "_fake")
            .password(TEST_PWD)
            .build();

        // Exercise
        sut.processConnect(session, msg);

        // Verify
        assertEqualsConnAck(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, session.readOutbound());
        assertFalse("Connection should be closed by the broker.", session.isOpen());
    }

    @Test
    public void testConnect_badClientID() {
        connMsg.clientId("extremely_long_clientID_greater_than_23").build();

        // Exercise
        sut.processConnect(session, connMsg.clientId("extremely_long_clientID_greater_than_23").build());

        // Verify
        assertEqualsConnAck(CONNECTION_ACCEPTED, session.readOutbound());
    }

    @Test
    public void testWill() {
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID).willFlag(true)
            .willTopic("topic").willMessage("Topic message").build();

        // Exercise
        // m_handler.setMessaging(mockedMessaging);
        sut.processConnect(session, msg);

        // Verify
        assertEqualsConnAck(CONNECTION_ACCEPTED, session.readOutbound());
        assertTrue("Connection is accepted and therefore should remain open.", session.isOpen());
        // TODO verify the call
        /*
         * verify(mockedMessaging).publish(eq("topic"), eq("Topic message".getBytes()),
         * any(AbstractMessage.QOSType.class), anyBoolean(), eq("123"), any(IoSession.class));
         */
    }

    @Test
    public void connectWithSameClientIDBadCredentialsDoesntDropExistingClient() {
        // Connect a client1
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID)
            .username(TEST_USER)
            .password(TEST_PWD)
            .build();
        sut.processConnect(session, msg);
        assertEqualsConnAck(CONNECTION_ACCEPTED, session.readOutbound());

        // create another connect same clientID but with bad credentials
        MqttConnectMessage evilClientConnMsg = MqttMessageBuilders.connect()
            .protocolVersion(MqttVersion.MQTT_3_1)
            .clientId(FAKE_CLIENT_ID)
            .username(ProtocolProcessorTest.EVIL_TEST_USER)
            .password(ProtocolProcessorTest.EVIL_TEST_PWD)
            .build();

        EmbeddedChannel evilSession = new EmbeddedChannel();

        // Exercise
        sut.processConnect(evilSession, evilClientConnMsg);

        // Verify
        // the evil client gets a not auth notification
        assertEqualsConnAck(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, evilSession.readOutbound());
        // the good client remains connected
        assertTrue(session.isOpen());
        assertFalse(evilSession.isOpen());
    }

    @Test
    public void acceptAnonymousClient() {
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID).build();

        // Exercise
        sut.processConnect(session, msg);

        // Verify
        assertEqualsConnAck(CONNECTION_ACCEPTED, session.readOutbound());
        assertTrue("Connection is accepted and therefore should remain open.", session.isOpen());
    }

    @Test
    public void validAuthentication() {
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID)
            .username(TEST_USER).password(TEST_PWD).build();

        // Exercise
        sut.processConnect(session, msg);

        // Verify
        assertEqualsConnAck(CONNECTION_ACCEPTED, session.readOutbound());
        assertTrue("Connection is accepted and therefore should remain open.", session.isOpen());
    }

    @Test
    public void noPasswdAuthentication() {
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID)
            .username(TEST_USER)
            .build();

        // Exercise
        sut.processConnect(session, msg);

        // Verify
        assertEqualsConnAck(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, session.readOutbound());
        assertFalse("Connection should be closed by the broker.", session.isOpen());
    }

    @Test
    public void prohibitAnonymousClient() {
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID).build();
        reinitProcessorProhibitingAnonymousClients();

        // Exercise
        sut.processConnect(session, msg);

        // Verify
        assertEqualsConnAck(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, session.readOutbound());
        assertFalse("Connection should be closed by the broker.", session.isOpen());
    }

    protected void reinitProcessorProhibitingAnonymousClients() {
        sut = new ConnectHandler(new ConnectionDescriptorStore(), NO_OBSERVERS_INTERCEPTOR, new SessionsRepository(this.m_sessionStore, null), m_sessionStore, m_mockAuthenticator,
            new PermitAllAuthorizator(), subscriptions, false, true, false, null);
    }

    @Test
    public void prohibitAnonymousClient_providingUsername() {
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID)
            .username(TEST_USER + "_fake")
            .build();
        reinitProcessorProhibitingAnonymousClients();

        // Exercise
        sut.processConnect(session, msg);

        // Verify
        assertEqualsConnAck(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, session.readOutbound());
        assertFalse("Connection should be closed by the broker.", session.isOpen());
    }

    @Test
    public void testZeroByteClientIdNotAllowed() {
        sut = new ConnectHandler(new ConnectionDescriptorStore(), NO_OBSERVERS_INTERCEPTOR, new SessionsRepository(this.m_sessionStore, null), m_sessionStore, m_mockAuthenticator,
            new PermitAllAuthorizator(), subscriptions, false, false, false, null);

        // Connect message with clean session set to true and client id is null.
        MqttConnectMessage msg = connMsg.clientId(null)
            .protocolVersion(MqttVersion.MQTT_3_1_1)
            .cleanSession(true)
            .build();

        sut.processConnect(session, msg);
        assertEqualsConnAck("Zero byte client identifiers are not allowed",
            CONNECTION_REFUSED_IDENTIFIER_REJECTED, session.readOutbound());

        assertFalse("Connection should closed.", session.isOpen());
    }

    @Test
    public void testZeroByteClientIdWithoutCleanSession() {
        // Allow zero byte client ids
        reinitProtocolProcessorWithZeroLengthClientIdAndAnonymousClients();

        // Connect message without clean session set to true but client id is still null
        MqttConnectMessage msg = MqttMessageBuilders.connect().clientId(null).protocolVersion(MqttVersion.MQTT_3_1_1)
            .build();

        sut.processConnect(session, msg);
        assertEqualsConnAck(
            "Identifier should be rejected due to having clean session set to false.",
            CONNECTION_REFUSED_IDENTIFIER_REJECTED,
            session.readOutbound());

        assertFalse("Connection should be closed by the broker.", session.isOpen());
    }

    protected void reinitProtocolProcessorWithZeroLengthClientIdAndAnonymousClients() {
        sut = new ConnectHandler(new ConnectionDescriptorStore(), NO_OBSERVERS_INTERCEPTOR, new SessionsRepository(this.m_sessionStore, null), m_sessionStore, m_mockAuthenticator,
            new PermitAllAuthorizator(), subscriptions, true, true, false, null);
    }

    @Test
    public void testConnAckContainsSessionPresentFlag() {
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID)
            .protocolVersion(MqttVersion.MQTT_3_1_1).build();
        NettyUtils.clientID(session, FAKE_CLIENT_ID);
        NettyUtils.cleanSession(session, false);

        // Connect a first time
        sut.processConnect(session, msg);
        // disconnect
        disconnectHandler.processDisconnect(session, FAKE_CLIENT_ID);

        // Exercise, reconnect
        EmbeddedChannel firstReceiverSession = new EmbeddedChannel();
        sut.processConnect(firstReceiverSession, msg);

        // Verify
        assertEqualsConnAck(CONNECTION_ACCEPTED, firstReceiverSession.readOutbound());
        assertTrue("Connection is accepted and therefore should remain open.", firstReceiverSession.isOpen());
    }

    @Test
    public void connectWithCleanSessionUpdateClientSession() {
        // first connect with clean session true
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID).cleanSession(true).build();
        sut.processConnect(session, msg);
        assertEqualsConnAck(CONNECTION_ACCEPTED, session.readOutbound());
        disconnectHandler.processDisconnect(session, FAKE_CLIENT_ID);
        assertFalse(session.isOpen());

        // second connect with clean session false
        session = new EmbeddedChannel();
        MqttConnectMessage secondConnMsg = MqttMessageBuilders.connect().clientId(FAKE_CLIENT_ID)
            .protocolVersion(MqttVersion.MQTT_3_1).build();

        sut.processConnect(session, secondConnMsg);
        assertEqualsConnAck(CONNECTION_ACCEPTED, session.readOutbound());

        // Verify client session is clean false
        ClientSession session = sessionsRepository.sessionForClient(FAKE_CLIENT_ID);
        assertFalse(session.isCleanSession());

        // Verify
        // assertEqualsConnAck(CONNECTION_ACCEPTED, session.readOutbound());
    }
}
