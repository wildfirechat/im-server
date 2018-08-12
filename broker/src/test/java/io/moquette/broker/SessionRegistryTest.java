package io.moquette.broker;

import io.moquette.persistence.MemoryStorageService;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.MockAuthenticator;
import io.moquette.spi.impl.SessionsRepository;
import io.moquette.spi.impl.security.PermitAllAuthorizatorPolicy;
import io.moquette.spi.impl.subscriptions.CTrieSubscriptionDirectory;
import io.moquette.spi.impl.subscriptions.ISubscriptionsDirectory;
import io.moquette.spi.security.IAuthenticator;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttVersion;
import org.junit.Before;
import org.junit.Test;

import static io.moquette.spi.impl.NettyChannelAssertions.assertEqualsConnAck;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_ACCEPTED;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class SessionRegistryTest {

    static final String FAKE_CLIENT_ID = "FAKE_123";
    static final String FAKE_CLIENT_ID2 = "FAKE_456";
    static final String PUBLISHER_ID = "Publisher";
    private static final String SUBSCRIBER_ID = "Subscriber";
    static final String NEWS_TOPIC = "/news";
    static final String BAD_FORMATTED_TOPIC = "#MQTTClient";

    static final String TEST_USER = "fakeuser";
    static final String TEST_PWD = "fakepwd";
    static final String EVIL_TEST_USER = "eviluser";
    static final String EVIL_TEST_PWD = "unsecret";

    private MQTTConnection connection;
    private EmbeddedChannel channel;
    private SessionRegistry sut;
    private MqttMessageBuilders.ConnectBuilder connMsg;
    private static final BrokerConfiguration ALLOW_ANONYMOUS_AND_ZEROBYTE_CLIENT_ID =
        new BrokerConfiguration(true, true, false);

    @Before
    public void setUp() {
        connMsg = MqttMessageBuilders.connect().protocolVersion(MqttVersion.MQTT_3_1).cleanSession(true);

        createMQTTConnection(ALLOW_ANONYMOUS_AND_ZEROBYTE_CLIENT_ID);
    }

    private void createMQTTConnection(BrokerConfiguration config) {
        channel = new EmbeddedChannel();
        connection = createMQTTConnection(config, channel);
    }

    private MQTTConnection createMQTTConnection(BrokerConfiguration config, Channel channel) {
        MemoryStorageService memStorage = new MemoryStorageService(null, null);
        ISessionsStore sessionStore = memStorage.sessionsStore();
        IAuthenticator mockAuthenticator = new MockAuthenticator(singleton(FAKE_CLIENT_ID), singletonMap(TEST_USER, TEST_PWD));

        ISubscriptionsDirectory subscriptions = new CTrieSubscriptionDirectory();
        SessionsRepository sessionsRepository = new SessionsRepository(sessionStore, null);
        subscriptions.init(sessionsRepository);

        sut = new SessionRegistry(subscriptions);
        final PostOffice postOffice = new PostOffice(subscriptions, new PermitAllAuthorizatorPolicy(),
                                                     new MemoryRetainedRepository(), sut);
        return new MQTTConnection(channel, config, mockAuthenticator, sut, postOffice);
    }

    @Test
    public void testConnAckContainsSessionPresentFlag() {
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID)
                                        .protocolVersion(MqttVersion.MQTT_3_1_1)
                                        .build();
        NettyUtils.clientID(channel, FAKE_CLIENT_ID);
        NettyUtils.cleanSession(channel, false);

        // Connect a first time
        sut.bindToSession(connection, msg, FAKE_CLIENT_ID);
        // disconnect
        sut.disconnect(FAKE_CLIENT_ID);

        // Exercise, reconnect
        EmbeddedChannel anotherChannel = new EmbeddedChannel();
        MQTTConnection anotherConnection = createMQTTConnection(ALLOW_ANONYMOUS_AND_ZEROBYTE_CLIENT_ID, anotherChannel);
        sut.bindToSession(anotherConnection, msg, FAKE_CLIENT_ID);

        // Verify
        assertEqualsConnAck(CONNECTION_ACCEPTED, anotherChannel.readOutbound());
        assertTrue("Connection is accepted and therefore should remain open", anotherChannel.isOpen());
    }

    @Test
    public void connectWithCleanSessionUpdateClientSession() {
        // first connect with clean session true
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID).cleanSession(true).build();
        connection.processConnect(msg);
        assertEqualsConnAck(CONNECTION_ACCEPTED, channel.readOutbound());
        connection.processDisconnect(null);
        assertFalse(channel.isOpen());

        // second connect with clean session false
        EmbeddedChannel anotherChannel = new EmbeddedChannel();
        MQTTConnection anotherConnection = createMQTTConnection(ALLOW_ANONYMOUS_AND_ZEROBYTE_CLIENT_ID,
                                                                anotherChannel);
        MqttConnectMessage secondConnMsg = MqttMessageBuilders.connect()
            .clientId(FAKE_CLIENT_ID)
            .protocolVersion(MqttVersion.MQTT_3_1)
            .build();

        anotherConnection.processConnect(secondConnMsg);
        assertEqualsConnAck(CONNECTION_ACCEPTED, anotherChannel.readOutbound());

        // Verify client session is clean false
        Session session = sut.retrieve(FAKE_CLIENT_ID);
        assertFalse(session.isClean());
    }
}
