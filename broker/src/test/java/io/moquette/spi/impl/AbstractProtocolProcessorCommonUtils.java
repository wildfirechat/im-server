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

import io.moquette.interception.InterceptHandler;
import io.moquette.persistence.MemoryStorageService;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.security.PermitAllAuthorizator;
import io.moquette.spi.impl.subscriptions.*;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.*;

import java.util.*;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_ACCEPTED;
import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_LEAST_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

abstract class AbstractProtocolProcessorCommonUtils {

    static final String FAKE_CLIENT_ID = "FAKE_123";
    static final String FAKE_CLIENT_ID2 = "FAKE_456";

    static final String TEST_USER = "fakeuser";
    static final String TEST_PWD = "fakepwd";

    static final List<InterceptHandler> EMPTY_OBSERVERS = Collections.emptyList();
    static final BrokerInterceptor NO_OBSERVERS_INTERCEPTOR = new BrokerInterceptor(EMPTY_OBSERVERS);
    public static final String HELLO_WORLD_MQTT = "Hello world MQTT!!";

    EmbeddedChannel m_channel;
    ProtocolProcessor m_processor;

    IMessagesStore m_messagesStore;
    ISessionsStore m_sessionStore;
    ISubscriptionsDirectory subscriptions;
    MockAuthenticator m_mockAuthenticator;
    protected SessionsRepository sessionsRepository;

    protected void initializeProcessorAndSubsystems() {
        m_channel = new EmbeddedChannel();
        NettyUtils.clientID(m_channel, FAKE_CLIENT_ID);
        NettyUtils.cleanSession(m_channel, false);

        // sleep to let the messaging batch processor to process the initEvent
        MemoryStorageService memStorage = new MemoryStorageService(null, null);
        m_messagesStore = memStorage.messagesStore();
        m_sessionStore = memStorage.sessionsStore();
        // m_messagesStore.initStore();

        sessionsRepository = new SessionsRepository(m_sessionStore, null);

        Set<String> clientIds = new HashSet<>();
        clientIds.add(FAKE_CLIENT_ID);
        clientIds.add(FAKE_CLIENT_ID2);
        Map<String, String> users = new HashMap<>();
        users.put(TEST_USER, TEST_PWD);
        m_mockAuthenticator = new MockAuthenticator(clientIds, users);

        subscriptions = new CTrieSubscriptionDirectory();
        subscriptions.init(sessionsRepository);
        m_processor = new ProtocolProcessor();
        m_processor.init(subscriptions, m_messagesStore, m_sessionStore, m_mockAuthenticator, true,
            new PermitAllAuthorizator(), NO_OBSERVERS_INTERCEPTOR, sessionsRepository);
    }

    protected void verifyNoPublishIsReceived() {
        final Object messageReceived = m_channel.readOutbound();
        assertNull("Received an out message from processor while not expected", messageReceived);
    }

    protected void subscribe(String topic, MqttQoS desiredQos) {
        subscribe(this.m_channel, topic, desiredQos);
    }

    protected void subscribe(EmbeddedChannel channel, String topic, MqttQoS desiredQos) {
        MqttSubscribeMessage subscribe = MqttMessageBuilders.subscribe()
            .addSubscription(desiredQos, topic)
            .messageId(1)
            .build();
        this.m_processor.processSubscribe(channel, subscribe);
        MqttSubAckMessage subAck = channel.readOutbound();
        assertEquals(desiredQos.value(), (int) subAck.payload().grantedQoSLevels().get(0));

        final String clientId = NettyUtils.clientID(channel);
        Subscription expectedSubscription = new Subscription(clientId, new Topic(topic), desiredQos);
        verifySubscriptionExists(channel, m_sessionStore, expectedSubscription);
    }

    protected static void verifySubscriptionExists(Channel channel, ISessionsStore sessionsStore, Subscription expectedSubscription) {
        final String clientId = NettyUtils.clientID(channel);
        final Subscription subscription = sessionsStore.subscriptionStore().reload(expectedSubscription);
        assertEquals(expectedSubscription, subscription);
    }

    protected MqttSubAckMessage subscribeWithoutVerify(String topic, MqttQoS desiredQos) {
        MqttSubscribeMessage subscribe = MqttMessageBuilders.subscribe()
            .addSubscription(desiredQos, topic)
            .messageId(1)
            .build();
        this.m_processor.processSubscribe(m_channel, subscribe);
        return m_channel.readOutbound();
    }

    protected void subscribeAndNotReadResponse(String topic, MqttQoS desiredQos) {
        MqttSubscribeMessage subscribe = MqttMessageBuilders.subscribe()
            .addSubscription(desiredQos, topic)
            .messageId(1)
            .build();
        this.m_processor.processSubscribe(m_channel, subscribe);
    }

    protected void unsubscribe(String topic) {
        final int messageId = 1;
        MqttUnsubscribeMessage msg = MqttMessageBuilders.unsubscribe()
            .addTopicFilter(topic)
            .messageId(messageId)
            .build();

        m_processor.processUnsubscribe(m_channel, msg);
    }

    protected void unsubscribeAndVerifyAck(String topic) {
        final int messageId = 1;
        MqttUnsubscribeMessage msg = MqttMessageBuilders.unsubscribe()
            .addTopicFilter(topic)
            .messageId(messageId)
            .build();

        m_processor.processUnsubscribe(m_channel, msg);

        MqttUnsubAckMessage unsubAckMessageAck = m_channel.readOutbound();
        assertEquals("Unsubscribe must be accepted", messageId, unsubAckMessageAck.variableHeader().messageId());
    }

    protected void internalPublishNotRetainedTo(String topic) {
        internalPublishTo(topic, AT_MOST_ONCE, false);
    }

    protected void internalPublishRetainedTo(String topic) {
        internalPublishTo(topic, AT_MOST_ONCE, true);
    }

    protected void internalPublishTo(String topic, MqttQoS qos, boolean retained) {
        MqttPublishMessage publish = MqttMessageBuilders.publish()
            .topicName(topic)
            .retained(retained)
            .qos(qos)
            .payload(Unpooled.copiedBuffer(HELLO_WORLD_MQTT.getBytes())).build();
        this.m_processor.internalPublish(publish, "INTRPUBL");
    }

    protected void publishToAs(String clientId, String topic, MqttQoS qos, boolean retained) {
        publishToAs(m_channel, clientId, topic, qos, retained);
    }

    protected void publishToAs(EmbeddedChannel channel, String clientId, String topic, MqttQoS qos, boolean retained) {
        NettyUtils.userName(channel, clientId);
        MqttPublishMessage publish = MqttMessageBuilders.publish()
            .topicName(topic)
            .retained(retained)
            .qos(qos)
            .payload(Unpooled.copiedBuffer(HELLO_WORLD_MQTT.getBytes())).build();
        this.m_processor.processPublish(m_channel, publish);
    }

    protected void publishToAs(String clientId, String topic, MqttQoS qos, int messageId, boolean retained) {
        publishToAs(this.m_channel, clientId, topic, qos, messageId, retained);
    }

    protected void publishToAs(EmbeddedChannel channel, String clientId, String topic, MqttQoS qos, int messageId, boolean retained) {
        publishToAs(channel, clientId, topic, HELLO_WORLD_MQTT, qos, messageId, retained);
    }

    protected void publishToAs(EmbeddedChannel channel, String clientId, String topic, String payload, MqttQoS qos, int messageId, boolean retained) {
        NettyUtils.userName(channel, clientId);
        MqttPublishMessage publish = MqttMessageBuilders.publish()
            .topicName(topic)
            .retained(retained)
            .messageId(messageId)
            .qos(qos)
            .payload(Unpooled.copiedBuffer(payload.getBytes())).build();
        this.m_processor.processPublish(channel, publish);
    }

    protected void publishQoS2ToAs(EmbeddedChannel channel, String clientId, String topic, int messageId, boolean retained) {
        publishQoS2ToAs(channel, clientId, topic, HELLO_WORLD_MQTT, messageId, retained);
    }

    protected void publishQoS2ToAs(EmbeddedChannel channel, String clientId, String topic, String payload, int messageId, boolean retained) {
        NettyUtils.userName(channel, clientId);
        MqttPublishMessage publish = MqttMessageBuilders.publish()
            .topicName(topic)
            .retained(retained)
            .messageId(messageId)
            .qos(MqttQoS.EXACTLY_ONCE)
            .payload(Unpooled.copiedBuffer(payload.getBytes())).build();
        this.m_processor.processPublish(channel, publish);

        verifyPubrecIsReceived(channel, messageId);

        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBREL, false, AT_LEAST_ONCE, false, 0);
        MqttMessage pubRel = new MqttMessage(mqttFixedHeader, from(messageId));
        this.m_processor.processPubRel(channel, pubRel);

        verifyPubCompIsReceived(channel, messageId);
    }

    private void verifyPubrecIsReceived(EmbeddedChannel channel, int messageId) {
        final MqttMessage pubRec = channel.readOutbound();
        assertEquals(messageId, Utils.messageId(pubRec));
    }

    private void verifyPubCompIsReceived(EmbeddedChannel channel, int messageId) {
        final MqttMessage pubComp = channel.readOutbound();
        assertEquals(messageId, Utils.messageId(pubComp));
    }


    protected void connect() {
        connectAsClient(FAKE_CLIENT_ID);
    }

    protected void connectAsClient(String clientId) {
        connectAsClient(m_channel, clientId);
    }

    protected void connectAsClient(EmbeddedChannel channel, String clientId) {
        MqttConnectMessage connectMessage = MqttMessageBuilders.connect()
            .clientId(clientId)
            .build();
        this.m_processor.processConnect(channel, connectMessage);
        MqttConnAckMessage connAck = channel.readOutbound();
        assertEquals("Connect must be accepted", CONNECTION_ACCEPTED, connAck.variableHeader().connectReturnCode());
    }

    protected void connect_v3_1() {
        connect_v3_1_asClient(FAKE_CLIENT_ID);
    }

    protected void connect_v3_1_asClient(String clientID) {
        MqttConnectMessage connectMessage = MqttMessageBuilders.connect()
            .clientId(clientID)
            .cleanSession(true)
            .protocolVersion(MqttVersion.MQTT_3_1)
            .build();
        this.m_processor.processConnect(m_channel, connectMessage);
        MqttConnAckMessage connAck = m_channel.readOutbound();
        assertEquals("Connect must be accepted", CONNECTION_ACCEPTED, connAck.variableHeader().connectReturnCode());
    }

    protected void connectNoCleanSession() {
        connectNoCleanSession(FAKE_CLIENT_ID);
    }

    protected void connectNoCleanSession(String clientId) {
        MqttConnectMessage connectMessage = MqttMessageBuilders.connect()
            .clientId(clientId)
            .cleanSession(false)
            .build();
        this.m_processor.processConnect(m_channel, connectMessage);
        MqttConnAckMessage connAck = m_channel.readOutbound();
        assertEquals("Connect must be accepted", CONNECTION_ACCEPTED, connAck.variableHeader().connectReturnCode());
    }

    protected void connectWithCleanSession(String clientId) {
        MqttConnectMessage connectMessage = MqttMessageBuilders.connect()
            .clientId(clientId)
            .cleanSession(true)
            .build();
        this.m_processor.processConnect(m_channel, connectMessage);
        MqttConnAckMessage connAck = m_channel.readOutbound();
        assertEquals("Connect must be accepted", CONNECTION_ACCEPTED, connAck.variableHeader().connectReturnCode());
    }

    protected void disconnect() throws InterruptedException {
        disconnect(this.m_channel);
    }

    protected void disconnect(EmbeddedChannel channel) throws InterruptedException {
        this.m_processor.processDisconnect(channel);
    }

    protected void verifyPublishIsReceived() {
        verifyPublishIsReceived(m_channel);
    }

    protected void verifyPublishIsReceived(EmbeddedChannel channel) {
        final MqttPublishMessage publishReceived = channel.readOutbound();
        String payloadMessage = new String(publishReceived.payload().array());
        assertEquals("Sent and received payload must be identical", HELLO_WORLD_MQTT, payloadMessage);
    }

    protected void verifyPublishIsReceived(MqttQoS expectedQoS) {
        verifyPublishIsReceived(HELLO_WORLD_MQTT, expectedQoS);
    }

    protected void verifyPublishIsReceived(String expectedPayload, MqttQoS expectedQoS) {
        verifyPublishIsReceived(this.m_channel, expectedPayload, expectedQoS);
    }

    protected void verifyPublishIsReceived(EmbeddedChannel channel, String expectedPayload, MqttQoS expectedQoS) {
        final MqttPublishMessage publishReceived = channel.readOutbound();
        String payloadMessage = new String(publishReceived.payload().array());
        assertEquals("Sent and received payload must be identical", expectedPayload, payloadMessage);
        assertEquals("Expected QoS don't match", expectedQoS, publishReceived.fixedHeader().qosLevel());
    }
}
