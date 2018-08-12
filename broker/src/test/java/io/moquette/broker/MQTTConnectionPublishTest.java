package io.moquette.broker;

import io.moquette.persistence.MemoryStorageService;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.MockAuthenticator;
import io.moquette.spi.impl.SessionsRepository;
import io.moquette.spi.impl.security.PermitAllAuthorizatorPolicy;
import io.moquette.spi.impl.subscriptions.CTrieSubscriptionDirectory;
import io.moquette.spi.impl.subscriptions.ISubscriptionsDirectory;
import io.moquette.spi.security.IAuthenticator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttVersion;
import org.junit.Before;
import org.junit.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertFalse;

public class MQTTConnectionPublishTest {

    private static final String FAKE_CLIENT_ID = "FAKE_123";
    private static final String TEST_USER = "fakeuser";
    private static final String TEST_PWD = "fakepwd";

    private MQTTConnection sut;
    private EmbeddedChannel channel;
    private SessionRegistry sessionRegistry;
    private MqttMessageBuilders.ConnectBuilder connMsg;

    @Before
    public void setUp() {
        connMsg = MqttMessageBuilders.connect().protocolVersion(MqttVersion.MQTT_3_1).cleanSession(true);

        BrokerConfiguration config = new BrokerConfiguration(true, true, false);

        createMQTTConnection(config);
    }

    private void createMQTTConnection(BrokerConfiguration config) {
        channel = new EmbeddedChannel();
        sut = createMQTTConnection(config, channel);
    }

    private MQTTConnection createMQTTConnection(BrokerConfiguration config, Channel channel) {
        MemoryStorageService memStorage = new MemoryStorageService(null, null);
        ISessionsStore sessionStore = memStorage.sessionsStore();
        IAuthenticator mockAuthenticator = new MockAuthenticator(singleton(FAKE_CLIENT_ID), singletonMap(TEST_USER, TEST_PWD));

        ISubscriptionsDirectory subscriptions = new CTrieSubscriptionDirectory();
        SessionsRepository sessionsRepository = new SessionsRepository(sessionStore, null);
        subscriptions.init(sessionsRepository);

        sessionRegistry = new SessionRegistry(subscriptions);
        final PostOffice postOffice = new PostOffice(subscriptions, new PermitAllAuthorizatorPolicy(),
                                                     new MemoryRetainedRepository(), sessionRegistry);
        return new MQTTConnection(channel, config, mockAuthenticator, sessionRegistry, postOffice);
    }

    @Test
    public void dropConnectionOnPublishWithInvalidTopicFormat() {
        // Connect message with clean session set to true and client id is null.
        MqttPublishMessage publish = MqttMessageBuilders.publish()
            .topicName("")
            .retained(false)
            .qos(MqttQoS.AT_MOST_ONCE)
            .payload(Unpooled.copiedBuffer("Hello MQTT world!".getBytes(UTF_8))).build();

        sut.processPublish(publish);

        // Verify
        assertFalse("Connection should be closed by the broker", channel.isOpen());
    }

}
