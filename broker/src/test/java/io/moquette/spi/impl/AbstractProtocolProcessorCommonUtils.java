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
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.SubscriptionsDirectory;
import io.moquette.spi.impl.subscriptions.Topic;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.*;

import java.util.*;

import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_ACCEPTED;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
    SubscriptionsDirectory subscriptions;
    MockAuthenticator m_mockAuthenticator;

    protected void initializeProcessorAndSubsystems() {
        m_channel = new EmbeddedChannel();
        NettyUtils.clientID(m_channel, FAKE_CLIENT_ID);
        NettyUtils.cleanSession(m_channel, false);

        // sleep to let the messaging batch processor to process the initEvent
        MemoryStorageService memStorage = new MemoryStorageService(null, null);
        m_messagesStore = memStorage.messagesStore();
        m_sessionStore = memStorage.sessionsStore();
        // m_messagesStore.initStore();

        Set<String> clientIds = new HashSet<>();
        clientIds.add(FAKE_CLIENT_ID);
        clientIds.add(FAKE_CLIENT_ID2);
        Map<String, String> users = new HashMap<>();
        users.put(TEST_USER, TEST_PWD);
        m_mockAuthenticator = new MockAuthenticator(clientIds, users);

        subscriptions = new SubscriptionsDirectory();
        subscriptions.init(memStorage.sessionsStore());
        m_processor = new ProtocolProcessor();
        m_processor.init(subscriptions, m_messagesStore, m_sessionStore, m_mockAuthenticator, true,
            new PermitAllAuthorizator(), NO_OBSERVERS_INTERCEPTOR);
    }

    protected void verifyNoPublishIsReceived() {
        assertNull("No out messages from the processor", m_channel.readOutbound());
    }

    protected void subscribe(String topic, MqttQoS desiredQos) {
        MqttSubscribeMessage subscribe = MqttMessageBuilders.subscribe()
            .addSubscription(desiredQos, topic)
            .messageId(1)
            .build();
        this.m_processor.processSubscribe(m_channel, subscribe);
        MqttSubAckMessage subAck = m_channel.readOutbound();
        assertEquals(desiredQos.value(), (int) subAck.payload().grantedQoSLevels().get(0));

        Subscription expectedSubscription = new Subscription(FAKE_CLIENT_ID, new Topic(topic), desiredQos);
        assertTrue(subscriptions.contains(expectedSubscription));
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
        MqttUnsubscribeMessage msg = MqttMessageBuilders.unsubscribe()
            .addTopicFilter(topic)
            .messageId(1)
            .build();

        m_processor.processUnsubscribe(m_channel, msg);
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
        NettyUtils.userName(m_channel, clientId);
        MqttPublishMessage publish = MqttMessageBuilders.publish()
            .topicName(topic)
            .retained(retained)
            .qos(qos)
            .payload(Unpooled.copiedBuffer(HELLO_WORLD_MQTT.getBytes())).build();
        this.m_processor.processPublish(m_channel, publish);
    }

    protected void publishToAs(String clientId, String topic, MqttQoS qos, int messageId, boolean retained) {
        NettyUtils.userName(m_channel, clientId);
        MqttPublishMessage publish = MqttMessageBuilders.publish()
            .topicName(topic)
            .retained(retained)
            .messageId(messageId)
            .qos(qos)
            .payload(Unpooled.copiedBuffer(HELLO_WORLD_MQTT.getBytes())).build();
        this.m_processor.processPublish(m_channel, publish);
    }


    protected void connect() {
        connectAsClient(FAKE_CLIENT_ID);
    }

    protected void connectAsClient(String clientId) {
        MqttConnectMessage connectMessage = MqttMessageBuilders.connect()
            .clientId(clientId)
            .build();
        this.m_processor.processConnect(m_channel, connectMessage);
        MqttConnAckMessage connAck = m_channel.readOutbound();
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
        MqttConnectMessage connectMessage = MqttMessageBuilders.connect()
            .clientId(FAKE_CLIENT_ID)
            .cleanSession(false)
            .build();
        this.m_processor.processConnect(m_channel, connectMessage);
        MqttConnAckMessage connAck = m_channel.readOutbound();
        assertEquals("Connect must be accepted", CONNECTION_ACCEPTED, connAck.variableHeader().connectReturnCode());
    }

    protected void disconnect() throws InterruptedException {
        this.m_processor.processDisconnect(this.m_channel);
    }

    protected void verifyPublishIsReceived() {
        final MqttPublishMessage publishReceived = m_channel.readOutbound();
        String payloadMessage = new String(publishReceived.payload().array());
        assertEquals("Sent and received payload must be identical", HELLO_WORLD_MQTT, payloadMessage);
    }
}
