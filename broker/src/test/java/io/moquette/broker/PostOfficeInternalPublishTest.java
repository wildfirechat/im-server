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
import io.moquette.broker.subscriptions.Subscription;
import io.moquette.broker.subscriptions.Topic;
import io.moquette.persistence.MemorySubscriptionsRepository;
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
    private MemoryQueueRepository queueRepository;

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
        subscriptions = new CTrieSubscriptionDirectory();
        ISubscriptionsRepository subscriptionsRepository = new MemorySubscriptionsRepository();
        subscriptions.init(subscriptionsRepository);
        retainedRepository = new MemoryRetainedRepository();
        queueRepository = new MemoryQueueRepository();

        SessionRegistry sessionRegistry = new SessionRegistry(subscriptions, queueRepository);
        sut = new PostOffice(subscriptions, new PermitAllAuthorizatorPolicy(), retainedRepository, sessionRegistry,
                             ConnectionTestUtils.NO_OBSERVERS_INTERCEPTOR);
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
        sut.internalPublish(publish);
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
