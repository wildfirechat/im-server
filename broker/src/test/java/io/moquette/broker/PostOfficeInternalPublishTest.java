package io.moquette.broker;

import io.moquette.persistence.MemoryStorageService;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.MockAuthenticator;
import io.moquette.spi.impl.SessionsRepository;
import io.moquette.spi.impl.security.PermitAllAuthorizatorPolicy;
import io.moquette.spi.impl.subscriptions.CTrieSubscriptionDirectory;
import io.moquette.spi.impl.subscriptions.ISubscriptionsDirectory;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static io.moquette.broker.PostOfficeUnsubscribeTest.CONFIG;
import static io.netty.handler.codec.mqtt.MqttQoS.*;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.*;

public class PostOfficeInternalPublishTest {

    private static final String FAKE_CLIENT_ID = "FAKE_123";
    private static final String TEST_USER = "fakeuser";
    private static final String TEST_PWD = "fakepwd";
    private static final String PAYLOAD = "Hello MQTT World";

    private MQTTConnection connection;
    private EmbeddedChannel channel;
    private PostOffice sut;
    private ISubscriptionsDirectory subscriptions;
    private MqttConnectMessage connectMessage;
    private SessionRegistry sessionRegistry;
    private MockAuthenticator mockAuthenticator;
    private static final BrokerConfiguration ALLOW_ANONYMOUS_AND_ZERO_BYTES_CLID =
        new BrokerConfiguration(true, true, false);
    private MemoryRetainedRepository retainedRepository;

    @Before
    public void setUp() {
        sessionRegistry = initPostOfficeAndSubsystems();

        mockAuthenticator = new MockAuthenticator(singleton(FAKE_CLIENT_ID), singletonMap(TEST_USER, TEST_PWD));
        connection = createMQTTConnection(ALLOW_ANONYMOUS_AND_ZERO_BYTES_CLID);

        connectMessage = ConnectionTestUtils.buildConnect(FAKE_CLIENT_ID);

        connection.processConnect(connectMessage);
        ConnectionTestUtils.assertConnectAccepted(channel);
    }

    private MQTTConnection createMQTTConnection(BrokerConfiguration config) {
        channel = new EmbeddedChannel();
        return createMQTTConnection(config, channel);
    }

    private MQTTConnection createMQTTConnection(BrokerConfiguration config, Channel channel) {
        return new MQTTConnection(channel, config, mockAuthenticator, sessionRegistry, sut);
    }

    private SessionRegistry initPostOfficeAndSubsystems() {
        MemoryStorageService memStorage = new MemoryStorageService(null, null);
        ISessionsStore sessionStore = memStorage.sessionsStore();

        subscriptions = new CTrieSubscriptionDirectory();
        SessionsRepository sessionsRepository = new SessionsRepository(sessionStore, null);
        subscriptions.init(sessionsRepository);
        retainedRepository = new MemoryRetainedRepository();

        SessionRegistry sessionRegistry = new SessionRegistry(subscriptions);
        sut = new PostOffice(subscriptions, new PermitAllAuthorizatorPolicy(), retainedRepository, sessionRegistry);
        return sessionRegistry;
    }

    private void internalPublishNotRetainedTo(String topic) {
        internalPublishTo(topic, AT_MOST_ONCE, false);
    }

    private void internalPublishRetainedTo(String topic) {
        internalPublishTo(topic, AT_MOST_ONCE, true);
    }

    private void internalPublishTo(String topic, MqttQoS qos, boolean retained) {
        MqttPublishMessage publish = MqttMessageBuilders.publish()
            .topicName(topic)
            .retained(retained)
            .qos(qos)
            .payload(Unpooled.copiedBuffer(PAYLOAD.getBytes(UTF_8))).build();
        sut.internalPublish(publish, "INTRPUBL");
    }

    @Test
    public void testClientSubscribeAfterNotRetainedQoS0IsSent() {
//        connection.processConnect(connectMessage);
//        ConnectionTestUtils.assertConnectAccepted(channel);

        // Exercise
        final String topic = "/topic";
        internalPublishNotRetainedTo(topic);

        subscribe(AT_MOST_ONCE, topic, connection);

        // Verify
        verifyNoPublishIsReceived(channel);
    }

    @Test
    public void testClientSubscribeAfterRetainedQoS0IsSent() {
        // Exercise
        final String topic = "/topic";
        internalPublishRetainedTo(topic);

        subscribe(AT_MOST_ONCE, topic, connection);

        // Verify
        verifyNoPublishIsReceived(channel);
    }

    @Test
    public void testClientSubscribeBeforeNotRetainedQoS0IsSent() {
        subscribe(AT_MOST_ONCE, "/topic", connection);

        // Exercise
        internalPublishNotRetainedTo("/topic");

        // Verify
        ConnectionTestUtils.verifyPublishIsReceived(channel, AT_MOST_ONCE, PAYLOAD);
    }

    @Test
    public void testClientSubscribeBeforeRetainedQoS0IsSent() {
        subscribe(AT_MOST_ONCE, "/topic", connection);

        // Exercise
        internalPublishRetainedTo("/topic");

        // Verify
        ConnectionTestUtils.verifyPublishIsReceived(channel, AT_MOST_ONCE, PAYLOAD);
    }

    @Test
    public void testClientSubscribeBeforeNotRetainedQoS1IsSent() {
        subscribe(AT_LEAST_ONCE, "/topic", connection);

        // Exercise
        internalPublishTo("/topic", AT_LEAST_ONCE, false);

        // Verify
        ConnectionTestUtils.verifyPublishIsReceived(channel, AT_LEAST_ONCE, PAYLOAD);
    }

    @Test
    public void testClientSubscribeAfterNotRetainedQoS1IsSent() {
        // Exercise
        internalPublishTo("/topic", AT_LEAST_ONCE, false);
        subscribe(AT_LEAST_ONCE, "/topic", connection);

        // Verify
        verifyNoPublishIsReceived(channel);
    }

    @Test
    public void testClientSubscribeBeforeRetainedQoS1IsSent() {
        subscribe(AT_LEAST_ONCE, "/topic", connection);

        // Exercise
        internalPublishTo("/topic", AT_LEAST_ONCE, true);

        // Verify
        ConnectionTestUtils.verifyPublishIsReceived(channel, AT_LEAST_ONCE, PAYLOAD);
    }

    @Test
    public void testClientSubscribeAfterRetainedQoS1IsSent() {
        // Exercise
        internalPublishTo("/topic", AT_LEAST_ONCE, true);
        subscribe(AT_LEAST_ONCE, "/topic", connection);

        // Verify
        ConnectionTestUtils.verifyPublishIsReceived(channel, AT_LEAST_ONCE, PAYLOAD);
    }

    @Test
    public void testClientSubscribeBeforeNotRetainedQoS2IsSent() {
        subscribe(EXACTLY_ONCE, "/topic", connection);

        // Exercise
        internalPublishTo("/topic", EXACTLY_ONCE, false);

        // Verify
        ConnectionTestUtils.verifyPublishIsReceived(channel, EXACTLY_ONCE, PAYLOAD);
    }

    @Test
    public void testClientSubscribeAfterNotRetainedQoS2IsSent() {
        // Exercise
        internalPublishTo("/topic", EXACTLY_ONCE, false);
        subscribe(EXACTLY_ONCE, "/topic", connection);

        // Verify
        verifyNoPublishIsReceived(channel);
    }

    @Test
    public void testClientSubscribeBeforeRetainedQoS2IsSent() {
        subscribe(EXACTLY_ONCE, "/topic", connection);

        // Exercise
        internalPublishTo("/topic", EXACTLY_ONCE, true);

        // Verify
        ConnectionTestUtils.verifyPublishIsReceived(channel, EXACTLY_ONCE, PAYLOAD);
    }

    @Test
    public void testClientSubscribeAfterRetainedQoS2IsSent() {
        // Exercise
        internalPublishTo("/topic", EXACTLY_ONCE, true);
        subscribe(EXACTLY_ONCE, "/topic", connection);

        // Verify
        ConnectionTestUtils.verifyPublishIsReceived(channel, EXACTLY_ONCE, PAYLOAD);
    }

    @Test
    public void testClientSubscribeAfterDisconnected() {
        subscribe(AT_MOST_ONCE, "foo", connection);
        connection.processDisconnect(null);

        internalPublishTo("foo", AT_MOST_ONCE, false);

        verifyNoPublishIsReceived(channel);
    }

    @Test
    public void testClientSubscribeWithoutCleanSession() {
        subscribe(AT_MOST_ONCE, "foo", connection);
        connection.processDisconnect(null);
        assertEquals(1, subscriptions.size());

        MQTTConnection anotherConn = createMQTTConnection(CONFIG);

        MqttConnectMessage connectMessage = MqttMessageBuilders.connect()
            .clientId(FAKE_CLIENT_ID)
            .cleanSession(false)
            .build();
        anotherConn.processConnect(connectMessage);
        ConnectionTestUtils.assertConnectAccepted((EmbeddedChannel) anotherConn.channel);

        assertEquals(1, subscriptions.size());
        internalPublishTo("foo", MqttQoS.AT_MOST_ONCE, false);
        ConnectionTestUtils.verifyPublishIsReceived((EmbeddedChannel) anotherConn.channel, AT_MOST_ONCE, PAYLOAD);
    }

    private void subscribe(MqttQoS topic, String newsTopic, MQTTConnection connection) {
        MqttSubscribeMessage subscribe = MqttMessageBuilders.subscribe()
            .addSubscription(topic, newsTopic)
            .messageId(1)
            .build();
        sut.subscribeClientToTopics(subscribe, connection.getClientId(), null, this.connection);

        MqttSubAckMessage subAck = ((EmbeddedChannel) this.connection.channel).readOutbound();
        assertEquals(topic.value(), (int) subAck.payload().grantedQoSLevels().get(0));
    }

    protected void subscribe(MQTTConnection connection, String topic, MqttQoS desiredQos) {
        EmbeddedChannel channel = (EmbeddedChannel) connection.channel;
        MqttSubscribeMessage subscribe = MqttMessageBuilders.subscribe()
            .addSubscription(desiredQos, topic)
            .messageId(1)
            .build();
        sut.subscribeClientToTopics(subscribe, connection.getClientId(), null, connection);

        MqttSubAckMessage subAck = channel.readOutbound();
        assertEquals(desiredQos.value(), (int) subAck.payload().grantedQoSLevels().get(0));

        final String clientId = connection.getClientId();
        Subscription expectedSubscription = new Subscription(clientId, new Topic(topic), desiredQos);

        final Set<Subscription> matchedSubscriptions = subscriptions.matchWithoutQosSharpening(new Topic(topic));
        assertEquals(1, matchedSubscriptions.size());
        final Subscription onlyMatchedSubscription = matchedSubscriptions.iterator().next();
        assertEquals(expectedSubscription, onlyMatchedSubscription);
    }


    private void verifyNoPublishIsReceived(EmbeddedChannel channel) {
        final Object messageReceived = channel.readOutbound();
        assertNull("Received an out message from processor while not expected", messageReceived);
    }
}
