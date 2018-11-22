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
package io.moquette.broker;

import io.moquette.broker.security.PermitAllAuthorizatorPolicy;
import io.moquette.broker.subscriptions.CTrieSubscriptionDirectory;
import io.moquette.broker.subscriptions.ISubscriptionsDirectory;
import io.moquette.broker.security.IAuthenticator;
import io.moquette.persistence.MemorySubscriptionsRepository;
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
    private MemoryQueueRepository queueRepository;

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
        IAuthenticator mockAuthenticator = new MockAuthenticator(singleton(FAKE_CLIENT_ID),
                                                                 singletonMap(TEST_USER, TEST_PWD));

        ISubscriptionsDirectory subscriptions = new CTrieSubscriptionDirectory();
        ISubscriptionsRepository subscriptionsRepository = new MemorySubscriptionsRepository();
        subscriptions.init(subscriptionsRepository);
        queueRepository = new MemoryQueueRepository();

        sessionRegistry = new SessionRegistry(subscriptions, queueRepository);
        final PostOffice postOffice = new PostOffice(subscriptions, new PermitAllAuthorizatorPolicy(),
                                                     new MemoryRetainedRepository(), sessionRegistry,
                                                     ConnectionTestUtils.NO_OBSERVERS_INTERCEPTOR);
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
