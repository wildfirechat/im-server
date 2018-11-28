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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.*;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.*;

import static io.moquette.broker.PostOfficeUnsubscribeTest.CONFIG;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_LEAST_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.EXACTLY_ONCE;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.*;

public class PostOfficePublishTest {

    private static final String FAKE_CLIENT_ID = "FAKE_123";
    private static final String FAKE_CLIENT_ID2 = "FAKE_456";
    static final String SUBSCRIBER_ID = "Subscriber";
    static final String PUBLISHER_ID = "Publisher";
    private static final String TEST_USER = "fakeuser";
    private static final String TEST_PWD = "fakepwd";
    private static final String NEWS_TOPIC = "/news";
    private static final String BAD_FORMATTED_TOPIC = "#MQTTClient";

    private MQTTConnection connection;
    private EmbeddedChannel channel;
    private PostOffice sut;
    private ISubscriptionsDirectory subscriptions;
    public static final String FAKE_USER_NAME = "UnAuthUser";
    private MqttConnectMessage connectMessage;
    private SessionRegistry sessionRegistry;
    private MockAuthenticator mockAuthenticator;
    static final BrokerConfiguration ALLOW_ANONYMOUS_AND_ZERO_BYTES_CLID =
        new BrokerConfiguration(true, true, false);
    private MemoryRetainedRepository retainedRepository;
    private MemoryQueueRepository queueRepository;

    @Before
    public void setUp() {
        sessionRegistry = initPostOfficeAndSubsystems();

        mockAuthenticator = new MockAuthenticator(singleton(FAKE_CLIENT_ID), singletonMap(TEST_USER, TEST_PWD));
        connection = createMQTTConnection(ALLOW_ANONYMOUS_AND_ZERO_BYTES_CLID);

        connectMessage = ConnectionTestUtils.buildConnect(FAKE_CLIENT_ID);
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

    @Test
    public void testPublishQoS0ToItself() {
        connection.processConnect(connectMessage);
        ConnectionTestUtils.assertConnectAccepted(channel);

        // subscribe
        subscribe(AT_MOST_ONCE, NEWS_TOPIC, connection);

        // Exercise
        final ByteBuf payload = Unpooled.copiedBuffer("Hello world!", Charset.defaultCharset());
        sut.receivedPublishQos0(new Topic(NEWS_TOPIC), TEST_USER, FAKE_CLIENT_ID, payload, false,
            MqttMessageBuilders.publish()
                .payload(payload.retainedDuplicate())
                .qos(MqttQoS.AT_MOST_ONCE)
                .retained(false)
                .topicName(NEWS_TOPIC).build());

        // Verify
        ConnectionTestUtils.verifyReceivePublish(channel, NEWS_TOPIC, "Hello world!");
    }

    @Test
    public void testForceClientDisconnection_issue116() {
        final MQTTConnection clientXA = connectAs("subscriber");
        subscribe(clientXA, NEWS_TOPIC, AT_MOST_ONCE);

        final MQTTConnection clientXB = connectAs("publisher");
        final ByteBuf anyPayload = Unpooled.copiedBuffer("Hello", Charset.defaultCharset());
        sut.receivedPublishQos2(clientXB, MqttMessageBuilders.publish()
            .payload(anyPayload)
            .qos(MqttQoS.EXACTLY_ONCE)
            .retained(false)
            .topicName(NEWS_TOPIC).build(), "username");

        final MQTTConnection clientYA = connectAs("subscriber");
        subscribe(clientYA, NEWS_TOPIC, AT_MOST_ONCE);

        final MQTTConnection clientYB = connectAs("publisher");
        final ByteBuf anyPayload2 = Unpooled.copiedBuffer("Hello 2", Charset.defaultCharset());
        sut.receivedPublishQos2(clientYB, MqttMessageBuilders.publish()
            .payload(anyPayload2)
            .qos(MqttQoS.EXACTLY_ONCE)
            .retained(true)
            .topicName(NEWS_TOPIC).build(), "username");

        // Verify
        assertFalse("First 'subscriber' channel MUST be closed by the broker", clientXA.channel.isOpen());
        ConnectionTestUtils.verifyPublishIsReceived((EmbeddedChannel) clientYA.channel, AT_MOST_ONCE, "Hello 2");
    }

    private MQTTConnection connectAs(String clientId) {
        EmbeddedChannel channel = new EmbeddedChannel();
        MQTTConnection connection = createMQTTConnection(CONFIG, channel);
        connection.processConnect(ConnectionTestUtils.buildConnect(clientId));
        ConnectionTestUtils.assertConnectAccepted(channel);
        return connection;
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

    @Test
    public void testPublishToMultipleSubscribers() {
        final Set<String> clientIds = new HashSet<>(Arrays.asList(FAKE_CLIENT_ID, FAKE_CLIENT_ID2));
        mockAuthenticator = new MockAuthenticator(clientIds, singletonMap(TEST_USER, TEST_PWD));
        EmbeddedChannel channel1 = new EmbeddedChannel();
        MQTTConnection connection1 = createMQTTConnection(ALLOW_ANONYMOUS_AND_ZERO_BYTES_CLID, channel1);
        connection1.processConnect(ConnectionTestUtils.buildConnect(FAKE_CLIENT_ID));
        ConnectionTestUtils.assertConnectAccepted(channel1);

        EmbeddedChannel channel2 = new EmbeddedChannel();
        MQTTConnection connection2 = createMQTTConnection(ALLOW_ANONYMOUS_AND_ZERO_BYTES_CLID, channel2);
        connection2.processConnect(ConnectionTestUtils.buildConnect(FAKE_CLIENT_ID2));
        ConnectionTestUtils.assertConnectAccepted(channel2);

        // subscribe
        final MqttQoS qos = AT_MOST_ONCE;
        final String newsTopic = NEWS_TOPIC;
        subscribe(qos, newsTopic, connection1);
        subscribe(qos, newsTopic, connection2);

        // Exercise
        final ByteBuf payload = Unpooled.copiedBuffer("Hello world!", Charset.defaultCharset());
        sut.receivedPublishQos0(new Topic(NEWS_TOPIC), TEST_USER, FAKE_CLIENT_ID, payload, false,
            MqttMessageBuilders.publish()
                .payload(payload.retainedDuplicate())
                .qos(MqttQoS.AT_MOST_ONCE)
                .retained(false)
                .topicName(NEWS_TOPIC).build());

        // Verify
        ConnectionTestUtils.verifyReceivePublish(channel1, NEWS_TOPIC, "Hello world!");
        ConnectionTestUtils.verifyReceivePublish(channel2, NEWS_TOPIC, "Hello world!");
    }

    @Test
    public void testPublishWithEmptyPayloadClearRetainedStore() {
        connection.processConnect(connectMessage);
        ConnectionTestUtils.assertConnectAccepted(channel);

        this.retainedRepository.retain(new Topic(NEWS_TOPIC), MqttMessageBuilders.publish()
            .payload(ByteBufUtil.writeAscii(UnpooledByteBufAllocator.DEFAULT, "Hello world!"))
            .qos(AT_LEAST_ONCE)
            .build());

        // Exercise
        final ByteBuf anyPayload = Unpooled.copiedBuffer("Any payload", Charset.defaultCharset());
        sut.receivedPublishQos0(new Topic(NEWS_TOPIC), TEST_USER, FAKE_CLIENT_ID, anyPayload, true,
            MqttMessageBuilders.publish()
                .payload(anyPayload)
                .qos(MqttQoS.AT_MOST_ONCE)
                .retained(false)
                .topicName(NEWS_TOPIC).build());

        // Verify
        assertTrue("QoS0 MUST clean retained message for topic", retainedRepository.isEmpty());
    }

    @Test
    public void testPublishWithQoS1() {
        connection.processConnect(connectMessage);
        ConnectionTestUtils.assertConnectAccepted(channel);
        subscribe(connection, NEWS_TOPIC, AT_LEAST_ONCE);

        // Exercise
        final ByteBuf anyPayload = Unpooled.copiedBuffer("Any payload", Charset.defaultCharset());
        sut.receivedPublishQos1(connection, new Topic(NEWS_TOPIC), TEST_USER, anyPayload, 1, true,
            MqttMessageBuilders.publish()
                .payload(Unpooled.copiedBuffer("Any payload", Charset.defaultCharset()))
                .qos(MqttQoS.AT_LEAST_ONCE)
                .retained(true)
                .topicName(NEWS_TOPIC).build());

        // Verify
        ConnectionTestUtils.verifyPublishIsReceived(channel, AT_LEAST_ONCE, "Any payload");
    }

    @Test
    public void testPublishWithQoS2() {
        connection.processConnect(connectMessage);
        ConnectionTestUtils.assertConnectAccepted(channel);
        subscribe(connection, NEWS_TOPIC, EXACTLY_ONCE);

        // Exercise
        final ByteBuf anyPayload = Unpooled.copiedBuffer("Any payload", Charset.defaultCharset());
        sut.receivedPublishQos2(connection, MqttMessageBuilders.publish()
                .payload(anyPayload)
                .qos(MqttQoS.EXACTLY_ONCE)
                .retained(true)
                .topicName(NEWS_TOPIC).build(), "username");

        // Verify
        ConnectionTestUtils.verifyPublishIsReceived(channel, EXACTLY_ONCE, "Any payload");
    }

    // aka testPublishWithQoS1_notCleanSession
    @Test
    public void forwardQoS1PublishesWhenNotCleanSessionReconnects() {
        connection.processConnect(ConnectionTestUtils.buildConnectNotClean(FAKE_CLIENT_ID));
        ConnectionTestUtils.assertConnectAccepted(channel);
        subscribe(connection, NEWS_TOPIC, AT_LEAST_ONCE);
        connection.processDisconnect(null);

        // publish a QoS 1 message from another client publish a message on the topic
        EmbeddedChannel pubChannel = new EmbeddedChannel();
        MQTTConnection pubConn = createMQTTConnection(ALLOW_ANONYMOUS_AND_ZERO_BYTES_CLID, pubChannel);
        pubConn.processConnect(ConnectionTestUtils.buildConnect(PUBLISHER_ID));
        ConnectionTestUtils.assertConnectAccepted(pubChannel);

        final ByteBuf anyPayload = Unpooled.copiedBuffer("Any payload", Charset.defaultCharset());
        sut.receivedPublishQos1(pubConn, new Topic(NEWS_TOPIC), TEST_USER, anyPayload, 1, true,
            MqttMessageBuilders.publish()
                .payload(anyPayload.retainedDuplicate())
                .qos(MqttQoS.AT_LEAST_ONCE)
                .topicName(NEWS_TOPIC).build());

        // simulate a reconnection from the other client
        connection = createMQTTConnection(ALLOW_ANONYMOUS_AND_ZERO_BYTES_CLID);
        connectMessage = ConnectionTestUtils.buildConnectNotClean(FAKE_CLIENT_ID);
        connection.processConnect(connectMessage);
        ConnectionTestUtils.assertConnectAccepted(channel);

        // Verify
        ConnectionTestUtils.verifyPublishIsReceived(channel, AT_LEAST_ONCE, "Any payload");
    }

    @Test
    public void checkReceivePublishedMessage_after_a_reconnect_with_notCleanSession() {
        // first connect - subscribe -disconnect
        connection.processConnect(ConnectionTestUtils.buildConnectNotClean(FAKE_CLIENT_ID));
        ConnectionTestUtils.assertConnectAccepted(channel);
        subscribe(connection, NEWS_TOPIC, AT_LEAST_ONCE);
        connection.processDisconnect(null);

        // connect - subscribe from another connection but with same ClientID
        EmbeddedChannel secondChannel = new EmbeddedChannel();
        MQTTConnection secondConn = createMQTTConnection(ALLOW_ANONYMOUS_AND_ZERO_BYTES_CLID, secondChannel);
        secondConn.processConnect(ConnectionTestUtils.buildConnect(FAKE_CLIENT_ID));
        ConnectionTestUtils.assertConnectAccepted(secondChannel);
        subscribe(secondConn, NEWS_TOPIC, AT_LEAST_ONCE);

        // publish a QoS 1 message another client publish a message on the topic
        EmbeddedChannel pubChannel = new EmbeddedChannel();
        MQTTConnection pubConn = createMQTTConnection(ALLOW_ANONYMOUS_AND_ZERO_BYTES_CLID, pubChannel);
        pubConn.processConnect(ConnectionTestUtils.buildConnect(PUBLISHER_ID));
        ConnectionTestUtils.assertConnectAccepted(pubChannel);

        final ByteBuf anyPayload = Unpooled.copiedBuffer("Any payload", Charset.defaultCharset());
        sut.receivedPublishQos1(pubConn, new Topic(NEWS_TOPIC), TEST_USER, anyPayload, 1, true,
            MqttMessageBuilders.publish()
                .payload(anyPayload.retainedDuplicate())
                .qos(MqttQoS.AT_LEAST_ONCE)
                .topicName(NEWS_TOPIC).build());

        // Verify that after a reconnection the client receive the message
        ConnectionTestUtils.verifyPublishIsReceived(secondChannel, AT_LEAST_ONCE, "Any payload");
    }

    @Test
    public void noPublishToInactiveSession() {
        // create an inactive session for Subscriber
        connection.processConnect(ConnectionTestUtils.buildConnectNotClean(SUBSCRIBER_ID));
        ConnectionTestUtils.assertConnectAccepted(channel);
        subscribe(connection, NEWS_TOPIC, AT_LEAST_ONCE);
        connection.processDisconnect(null);

        // Exercise
        EmbeddedChannel pubChannel = new EmbeddedChannel();
        MQTTConnection pubConn = createMQTTConnection(ALLOW_ANONYMOUS_AND_ZERO_BYTES_CLID, pubChannel);
        pubConn.processConnect(ConnectionTestUtils.buildConnect(PUBLISHER_ID));
        ConnectionTestUtils.assertConnectAccepted(pubChannel);

        final ByteBuf anyPayload = Unpooled.copiedBuffer("Any payload", Charset.defaultCharset());
        sut.receivedPublishQos1(pubConn, new Topic(NEWS_TOPIC), TEST_USER, anyPayload, 1, true,
            MqttMessageBuilders.publish()
                .payload(anyPayload)
                .qos(MqttQoS.AT_LEAST_ONCE)
                .retained(true)
                .topicName(NEWS_TOPIC).build());

        verifyNoPublishIsReceived(channel);
    }

    private void verifyNoPublishIsReceived(EmbeddedChannel channel) {
        final Object messageReceived = channel.readOutbound();
        assertNull("Received an out message from processor while not expected", messageReceived);
    }

    @Test
    public void cleanRetainedMessageStoreWhenPublishWithRetainedQos0IsReceived() {
        connection.processConnect(connectMessage);
        ConnectionTestUtils.assertConnectAccepted(channel);

        // publish a QoS1 retained message
        final ByteBuf anyPayload = Unpooled.copiedBuffer("Any payload", Charset.defaultCharset());
        final MqttPublishMessage publishMsg = MqttMessageBuilders.publish()
            .payload(Unpooled.copiedBuffer("Any payload", Charset.defaultCharset()))
            .qos(MqttQoS.AT_LEAST_ONCE)
            .retained(true)
            .topicName(NEWS_TOPIC)
            .build();
        sut.receivedPublishQos1(connection, new Topic(NEWS_TOPIC), TEST_USER, anyPayload, 1, true,
                                publishMsg);

        assertMessageIsRetained(NEWS_TOPIC, anyPayload);

        // publish a QoS0 retained message
        // Exercise
        final ByteBuf qos0Payload = Unpooled.copiedBuffer("QoS0 payload", Charset.defaultCharset());
        sut.receivedPublishQos0(new Topic(NEWS_TOPIC), TEST_USER, connection.getClientId(), qos0Payload, true,
            MqttMessageBuilders.publish()
                .payload(qos0Payload)
                .qos(MqttQoS.AT_MOST_ONCE)
                .retained(false)
                .topicName(NEWS_TOPIC).build());

        // Verify
        assertTrue("Retained message for topic /news must be cleared", retainedRepository.isEmpty());
    }

    private void assertMessageIsRetained(String expectedTopicName, ByteBuf expectedPayload) {
        List<RetainedMessage> msgs = retainedRepository.retainedOnTopic(expectedTopicName);
        assertEquals(1, msgs.size());
        RetainedMessage msg = msgs.get(0);
        assertEquals(ByteBufUtil.hexDump(expectedPayload), ByteBufUtil.hexDump(msg.getPayload()));
    }
}
