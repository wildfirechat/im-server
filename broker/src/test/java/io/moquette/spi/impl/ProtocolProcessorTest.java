/*
 * Copyright (c) 2012-2017 The original author or authorsgetRockQuestions()
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
import io.moquette.server.netty.MessageBuilder;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.IMatchingCondition;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.IMessagesStore.StoredMessage;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.security.PermitAllAuthorizator;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.SubscriptionsStore;
import io.moquette.spi.security.IAuthorizator;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.*;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.BlockingQueue;

import static io.moquette.spi.impl.NettyChannelAssertions.assertConnAckAccepted;
import static io.moquette.spi.impl.ProtocolProcessor.lowerQosToTheSubscriptionDesired;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author andrea
 */
public class ProtocolProcessorTest {
    final static String FAKE_CLIENT_ID = "FAKE_123";
    final static String FAKE_CLIENT_ID2 = "FAKE_456";
    final static String FAKE_PUBLISHER_ID = "Publisher";
    final static String FAKE_TOPIC = "/news";
    final static String BAD_FORMATTED_TOPIC = "#MQTTClient";

    final static String TEST_USER = "fakeuser";
    final static String TEST_PWD = "fakepwd";
    final static String EVIL_TEST_USER = "eviluser";
    final static String EVIL_TEST_PWD = "unsecret";

    final static List<InterceptHandler> EMPTY_OBSERVERS = Collections.emptyList();
    final static BrokerInterceptor NO_OBSERVERS_INTERCEPTOR = new BrokerInterceptor(EMPTY_OBSERVERS);

    EmbeddedChannel m_channel;
    MqttConnectMessage connMsg;
    ProtocolProcessor m_processor;

    IMessagesStore m_messagesStore;
    ISessionsStore m_sessionStore;
    SubscriptionsStore subscriptions;
    MockAuthenticator m_mockAuthenticator;

    @Before
    public void setUp() throws InterruptedException {
        /*connMsg = new ConnectMessage();
        connMsg.setProtocolVersion((byte) 0x03);

        connMsg = MessageBuilder.connect()
                .protocolVersion(MqttVersion.MQTT_3_1)
                .clientId(FAKE_CLIENT_ID)
                .cleanSession(true)
                .build();*/

        m_channel = new EmbeddedChannel();
        NettyUtils.clientID(m_channel, FAKE_CLIENT_ID);
        NettyUtils.cleanSession(m_channel, false);

        //sleep to let the messaging batch processor to process the initEvent
        Thread.sleep(300);
        MemoryStorageService memStorage = new MemoryStorageService();
        memStorage.initStore();
        m_messagesStore = memStorage.messagesStore();
        m_sessionStore = memStorage.sessionsStore();
        //m_messagesStore.initStore();

        Set<String> clientIds = new HashSet<>();
        clientIds.add(FAKE_CLIENT_ID);
        clientIds.add(FAKE_CLIENT_ID2);
        Map<String, String> users = new HashMap<>();
        users.put(TEST_USER, TEST_PWD);
        m_mockAuthenticator = new MockAuthenticator(clientIds, users);

        subscriptions = new SubscriptionsStore();
        subscriptions.init(memStorage.sessionsStore());
        m_processor = new ProtocolProcessor();
        m_processor.init(subscriptions, m_messagesStore, m_sessionStore, m_mockAuthenticator, true,
                new PermitAllAuthorizator(), NO_OBSERVERS_INTERCEPTOR);
    }


    @Test
    public void testPublishToItself() throws InterruptedException {
        final Subscription subscription = new Subscription(FAKE_CLIENT_ID, 
                FAKE_TOPIC, MqttQoS.AT_MOST_ONCE);

        //subscriptions.matches(topic) redefine the method to return true
        SubscriptionsStore subs = new SubscriptionsStore() {
            @Override
            public List<Subscription> matches(String topic) {
                if (topic.equals(FAKE_TOPIC)) {
                    return Collections.singletonList(subscription);
                } else {
                    throw new IllegalArgumentException("Expected " + FAKE_TOPIC + " buf found " + topic);
                }
            }
        };
        
        //simulate a connect that register a clientID to an IoSession
        MemoryStorageService storageService = new MemoryStorageService();
        storageService.initStore();
        subs.init(storageService.sessionsStore());
        m_processor.init(subs, m_messagesStore, m_sessionStore, null, true, new PermitAllAuthorizator(), NO_OBSERVERS_INTERCEPTOR);
        MqttConnectMessage connectMessage = MessageBuilder.connect()
                .protocolVersion(MqttVersion.MQTT_3_1)
                .clientId(FAKE_CLIENT_ID)
                .cleanSession(true)
                .build();
        m_processor.processConnect(m_channel, connectMessage);
        
        
        //Exercise
        MqttPublishMessage msg = MessageBuilder.publish()
                .topicName(FAKE_TOPIC)
                .qos(MqttQoS.AT_MOST_ONCE)
                .retained(false)
                .payload("Hello".getBytes())
                .build();

        NettyUtils.userName(m_channel, "FakeCLI");
        m_processor.processPublish(m_channel, msg);

        //Verify
        assertNotNull(m_channel.readOutbound());
        //TODO check received message attributes
    }
    
    @Test
    public void testPublishToMultipleSubscribers() throws InterruptedException {
        final Subscription subscription = new Subscription(FAKE_CLIENT_ID, 
                FAKE_TOPIC, MqttQoS.AT_MOST_ONCE);
        final Subscription subscriptionClient2 = new Subscription(FAKE_CLIENT_ID2, 
                FAKE_TOPIC, MqttQoS.AT_MOST_ONCE);

        //subscriptions.matches(topic) redefine the method to return true
        SubscriptionsStore subs = new SubscriptionsStore() {
            @Override
            public List<Subscription> matches(String topic) {
                if (topic.equals(FAKE_TOPIC)) {
                    return Arrays.asList(subscription, subscriptionClient2);
                } else {
                    throw new IllegalArgumentException("Expected " + FAKE_TOPIC + " buf found " + topic);
                }
            }
        };
        
        //simulate a connect that register a clientID to an IoSession
        MemoryStorageService storageService = new MemoryStorageService();
        storageService.initStore();
        subs.init(storageService.sessionsStore());
        m_processor.init(subs, m_messagesStore, m_sessionStore, null, true, new PermitAllAuthorizator(), NO_OBSERVERS_INTERCEPTOR);

        EmbeddedChannel firstReceiverChannel = new EmbeddedChannel();
        MqttConnectMessage connectMessage = MessageBuilder.connect()
                .protocolVersion(MqttVersion.MQTT_3_1)
                .clientId(FAKE_CLIENT_ID)
                .cleanSession(true)
                .build();
        m_processor.processConnect(firstReceiverChannel, connectMessage);
        assertConnAckAccepted(firstReceiverChannel);
        
        //connect the second fake subscriber
        EmbeddedChannel secondReceiverChannel = new EmbeddedChannel();
        MqttConnectMessage connectMessage2 = MessageBuilder.connect()
                .protocolVersion(MqttVersion.MQTT_3_1)
                .clientId(FAKE_CLIENT_ID2)
                .cleanSession(true)
                .build();
        m_processor.processConnect(secondReceiverChannel, connectMessage2);
        assertConnAckAccepted(secondReceiverChannel);

        //Exercise
        MqttPublishMessage msg = MessageBuilder.publish()
                .topicName(FAKE_TOPIC)
                .qos(MqttQoS.AT_MOST_ONCE)
                .retained(false)
                .payload("Hello".getBytes())
                .build();
        NettyUtils.userName(m_channel, "FakeCLI");
        m_processor.processPublish(m_channel, msg);


        //Verify
        firstReceiverChannel.flush();
        MqttPublishMessage pub2FirstSubscriber = firstReceiverChannel.readOutbound();
        assertNotNull(pub2FirstSubscriber);
        String firstMessageContent = DebugUtils.payload2Str(pub2FirstSubscriber.payload());
        assertEquals("Hello", firstMessageContent);

        secondReceiverChannel.flush();
        MqttPublishMessage pub2SecondSubscriber = secondReceiverChannel.readOutbound();
        assertNotNull(pub2SecondSubscriber);
        String secondMessageContent = DebugUtils.payload2Str(pub2SecondSubscriber.payload());
        assertEquals("Hello", secondMessageContent);
    }

    @Test
    public void testSubscribe() {
        MqttSubscribeMessage msg = MessageBuilder.subscribe()
                .addSubscription(MqttQoS.AT_MOST_ONCE, FAKE_TOPIC)
                .messageId(10)
                .build();
        //Exercise
        m_sessionStore.createNewSession(FAKE_CLIENT_ID, false);
        m_processor.processSubscribe(m_channel, msg);

        //Verify
        assertTrue(m_channel.readOutbound() instanceof MqttSubAckMessage);
        Subscription expectedSubscription = new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, MqttQoS.AT_MOST_ONCE);
        assertTrue(subscriptions.contains(expectedSubscription));
    }

    @Test
    public void testSubscribedToNotAuthorizedTopic() {
        final String fakeUserName = "UnAuthUser";
        NettyUtils.userName(m_channel, fakeUserName);

        IAuthorizator mockAuthorizator = mock(IAuthorizator.class);
        when(mockAuthorizator.canRead(eq(FAKE_TOPIC), eq(fakeUserName), eq(FAKE_CLIENT_ID))).thenReturn(false);
        m_processor.init(subscriptions, m_messagesStore, m_sessionStore, m_mockAuthenticator, true,
                mockAuthorizator, NO_OBSERVERS_INTERCEPTOR);

        //Exercise
        MqttSubscribeMessage msg = MessageBuilder.subscribe()
                .addSubscription(MqttQoS.AT_MOST_ONCE, FAKE_TOPIC)
                .messageId(10)
                .build();

        m_sessionStore.createNewSession(FAKE_CLIENT_ID, false);
        m_processor.processSubscribe(m_channel, msg);

        //Verify
        Object ackMsg = m_channel.readOutbound();
        assertTrue(ackMsg instanceof MqttSubAckMessage);
        MqttSubAckMessage subAckMsg = (MqttSubAckMessage) ackMsg;
        verifyFailureQos(subAckMsg);
    }

    private void verifyFailureQos(MqttSubAckMessage subAckMsg) {
        List<Integer> grantedQoSes = subAckMsg.payload().grantedQoSLevels();
        assertEquals(1, grantedQoSes.size());
        assertTrue(grantedQoSes.contains(MqttQoS.FAILURE.value()));
    }

    @Test
    public void testDoubleSubscribe() {
        MqttSubscribeMessage msg = MessageBuilder.subscribe()
                .addSubscription(MqttQoS.AT_MOST_ONCE, FAKE_TOPIC)
                .messageId(10)
                .build();
        m_sessionStore.createNewSession(FAKE_CLIENT_ID, false);
        assertEquals(0, subscriptions.size());
        
        m_processor.processSubscribe(m_channel, msg);
                
        //Exercise
        m_processor.processSubscribe(m_channel, msg);

        //Verify
        assertEquals(1, subscriptions.size());
        Subscription expectedSubscription = new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, MqttQoS.AT_MOST_ONCE);
        assertTrue(subscriptions.contains(expectedSubscription));
    }


    @Test
    public void testSubscribeWithBadFormattedTopic() {
        MqttSubscribeMessage msg = MessageBuilder.subscribe()
                .addSubscription(MqttQoS.AT_MOST_ONCE, BAD_FORMATTED_TOPIC)
                .messageId(10)
                .build();

        m_sessionStore.createNewSession(FAKE_CLIENT_ID, false);
        assertEquals(0, subscriptions.size());

        //Exercise
        m_processor.processSubscribe(m_channel, msg);

        //Verify
        assertEquals(0, subscriptions.size());
        Object recvSubAckMessage = m_channel.readOutbound();
        assertTrue(recvSubAckMessage instanceof MqttSubAckMessage);
        verifyFailureQos((MqttSubAckMessage) recvSubAckMessage);
    }

    /*
     * Check topicFilter is a valid MQTT topic filter (issue 68)
     * */
    @Test
    public void testUnsubscribeWithBadFormattedTopic() {
        MqttUnsubscribeMessage msg = MessageBuilder.unsubscribe()
                .addTopicFilter(BAD_FORMATTED_TOPIC)
                .messageId(1)
                .build();

        //Exercise
        m_processor.processUnsubscribe(m_channel, msg);

        //Verify
        assertFalse("If client unsubscribe with bad topic than channel must be closed", m_channel.isOpen());
    }

    
    @Test
    public void testPublishOfRetainedMessage_afterNewSubscription() throws Exception {
        //simulate a connect that register a clientID to an IoSession
        final Subscription subscription = new Subscription(FAKE_PUBLISHER_ID, 
                FAKE_TOPIC, MqttQoS.AT_MOST_ONCE);

        //subscriptions.matches(topic) redefine the method to return true
        SubscriptionsStore subs = new SubscriptionsStore() {
            @Override
            public List<Subscription> matches(String topic) {
                if (topic.equals(FAKE_TOPIC)) {
                    return Collections.singletonList(subscription);
                } else {
                    throw new IllegalArgumentException("Expected " + FAKE_TOPIC + " buf found " + topic);
                }
            }
        };
        MemoryStorageService storageService = new MemoryStorageService();
        storageService.initStore();
        subs.init(storageService.sessionsStore());

        //simulate a connect that register a clientID to an IoSession
        m_processor.init(subs, m_messagesStore, m_sessionStore, null, true, new PermitAllAuthorizator(), NO_OBSERVERS_INTERCEPTOR);
        MqttConnectMessage connectMessage = MessageBuilder.connect()
                .clientId(FAKE_PUBLISHER_ID)
                .protocolVersion(MqttVersion.MQTT_3_1)
                .cleanSession(true)
                .build();

        m_processor.processConnect(m_channel, connectMessage);
        assertConnAckAccepted(m_channel);
        MqttPublishMessage pubmsg = MessageBuilder.publish()
                .topicName(FAKE_TOPIC)
                .qos(MqttQoS.AT_MOST_ONCE)
                .payload("Hello".getBytes())
                .retained(true)
                .build();

        NettyUtils.clientID(m_channel, FAKE_PUBLISHER_ID);
        m_processor.processPublish(m_channel, pubmsg);
        NettyUtils.cleanSession(m_channel, false);

        //Exercise
        MqttSubscribeMessage msg = MessageBuilder.subscribe()
                .messageId(10)
                .addSubscription(MqttQoS.AT_MOST_ONCE, "#")
                .build();
        m_processor.processSubscribe(m_channel, msg);

        //Verify
        //wait the latch
        Object pubMessage = m_channel.readOutbound();
        assertNotNull(pubMessage);
        assertTrue(pubMessage instanceof MqttPublishMessage);
        assertEquals(FAKE_TOPIC, ((MqttPublishMessage) pubMessage).variableHeader().topicName());
    }

    @Test
    public void testRepublishAndConsumePersistedMessages_onReconnect() {
        SubscriptionsStore subs = mock(SubscriptionsStore.class);
        List<Subscription> emptySubs = Collections.emptyList();
        when(subs.matches(anyString())).thenReturn(emptySubs);

        StoredMessage retainedMessage = new StoredMessage("Hello".getBytes(), MqttQoS.EXACTLY_ONCE, "/topic");
        retainedMessage.setRetained(true);
        retainedMessage.setMessageID(120);
        retainedMessage.setClientID(FAKE_PUBLISHER_ID);
        m_messagesStore.storePublishForFuture(retainedMessage);

        m_processor.init(subs, m_messagesStore, m_sessionStore, null, true, new PermitAllAuthorizator(), NO_OBSERVERS_INTERCEPTOR);

        MqttConnectMessage connectMessage = MessageBuilder.connect()
                .clientId(FAKE_PUBLISHER_ID)
                .protocolVersion(MqttVersion.MQTT_3_1)
                .build();

        m_processor.processConnect(m_channel, connectMessage);

        //Verify no messages are still stored
        BlockingQueue<StoredMessage> messages = m_sessionStore.queue(FAKE_PUBLISHER_ID);
        assertTrue(messages.isEmpty());
    }
    
    @Test
    public void publishNoPublishToInactiveSession() {
        //create an inactive session for Subscriber
        m_sessionStore.createNewSession("Subscriber", false);

        SubscriptionsStore mockedSubscriptions = mock(SubscriptionsStore.class);
        Subscription inactiveSub = new Subscription("Subscriber", "/topic", MqttQoS.AT_LEAST_ONCE);
        List<Subscription> inactiveSubscriptions = Collections.singletonList(inactiveSub);
        when(mockedSubscriptions.matches(eq("/topic"))).thenReturn(inactiveSubscriptions);
        m_processor = new ProtocolProcessor();
        m_processor.init(mockedSubscriptions, m_messagesStore, m_sessionStore, null, true, new PermitAllAuthorizator(),
                NO_OBSERVERS_INTERCEPTOR);
        
        //Exercise
        MqttPublishMessage msg = MessageBuilder.publish()
                .topicName("/topic")
                .qos(MqttQoS.AT_MOST_ONCE)
                .payload("Hello".getBytes())
                .retained(true)
                .build();

        NettyUtils.clientID(m_channel, "Publisher");
        m_processor.processPublish(m_channel, msg);

        //Verify no message is received
        assertNull(m_channel.readOutbound());
    }
    
    /**
     * Verify that receiving a publish with retained message and with Q0S = 0 
     * clean the existing retained messages for that topic.
     */
    @Test
    public void testCleanRetainedStoreAfterAQoS0AndRetainedTrue() {
        //force a connect
        connMsg = MessageBuilder.connect()
                .protocolVersion(MqttVersion.MQTT_3_1)
                .clientId("Publisher")
                .cleanSession(true)
                .build();
        m_processor.processConnect(m_channel, connMsg);
        //prepare and existing retained store
        NettyUtils.clientID(m_channel, "Publisher");
        MqttPublishMessage msg = MessageBuilder.publish()
                .topicName(FAKE_TOPIC)
                .qos(MqttQoS.AT_LEAST_ONCE)
                .payload("Hello".getBytes())
                .retained(true)
                .messageId(100)
                .build();
        m_processor.processPublish(m_channel, msg);

        Collection<IMessagesStore.StoredMessage> messages = m_messagesStore.searchMatching(new IMatchingCondition() {
            public boolean match(String key) {
                return  SubscriptionsStore.matchTopics(key, FAKE_TOPIC);
            }
        });
        assertFalse(messages.isEmpty());

        //Exercise
        MqttPublishMessage cleanPubMsg = MessageBuilder.publish()
                .topicName(FAKE_TOPIC)
                .qos(MqttQoS.AT_MOST_ONCE)
                .payload("Hello".getBytes())
                .retained(true)
                .build();

        m_processor.processPublish(m_channel, cleanPubMsg);
        
        //Verify
        messages = m_messagesStore.searchMatching(new IMatchingCondition() {
            public boolean match(String key) {
                return  SubscriptionsStore.matchTopics(key, FAKE_TOPIC);
            }
        });
        assertTrue(messages.isEmpty());
    }

    @Test
    public void testLowerTheQosToTheRequestedBySubscription() {
        Subscription subQos1 = new Subscription("Sub A", "a/b", MqttQoS.AT_LEAST_ONCE);
        assertEquals(MqttQoS.AT_LEAST_ONCE, lowerQosToTheSubscriptionDesired(subQos1, MqttQoS.EXACTLY_ONCE));

        Subscription subQos2 = new Subscription("Sub B", "a/+", MqttQoS.EXACTLY_ONCE);
        assertEquals(MqttQoS.EXACTLY_ONCE, lowerQosToTheSubscriptionDesired(subQos2, MqttQoS.EXACTLY_ONCE));
    }
}
