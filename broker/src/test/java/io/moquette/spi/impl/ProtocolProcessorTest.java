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

package io.moquette.spi.impl;

import io.moquette.broker.PostOfficePublishTest;
import io.moquette.interception.InterceptHandler;
import io.moquette.persistence.MemoryStorageService;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.IMessagesStore.StoredMessage;
import io.moquette.spi.impl.security.PermitAllAuthorizatorPolicy;
import io.moquette.spi.impl.subscriptions.CTrieSubscriptionDirectory;
import io.moquette.spi.impl.subscriptions.ISubscriptionsDirectory;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;
import io.moquette.spi.security.IAuthorizatorPolicy;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.*;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static io.moquette.spi.impl.NettyChannelAssertions.assertConnAckAccepted;
import static io.moquette.spi.impl.ProtocolProcessor.lowerQosToTheSubscriptionDesired;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_LEAST_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.EXACTLY_ONCE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ProtocolProcessorTest extends AbstractProtocolProcessorCommonUtils {

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

    public static final List<InterceptHandler> EMPTY_OBSERVERS = Collections.emptyList();
    public static final BrokerInterceptor NO_OBSERVERS_INTERCEPTOR = new BrokerInterceptor(EMPTY_OBSERVERS);

    @Before
    public void setUp() {
        initializeProcessorAndSubsystems();
    }

    //same test moved into PostOfficePublishTest
//    @Test
//    public void testPublishToItself() {
//        final Subscription subscription = new Subscription(FAKE_CLIENT_ID, new Topic(NEWS_TOPIC), AT_MOST_ONCE);
//
//        // subscriptions.matches(topic) redefine the method to return true
//        ISubscriptionsDirectory subs = new CTrieSubscriptionDirectory() {
//
//            @Override
//            public List<Subscription> matches(Topic topic) {
//                if (topic.toString().equals(NEWS_TOPIC)) {
//                    return Collections.singletonList(subscription);
//                } else {
//                    throw new IllegalArgumentException("Expected " + NEWS_TOPIC + " buf found " + topic);
//                }
//            }
//        };
//
//        // simulate a connect that register a clientID to an IoSession
//        MemoryStorageService storageService = new MemoryStorageService(null, null);
//        SessionsRepository sessionsRepository = new SessionsRepository(storageService.sessionsStore(), null);
//        subs.init(sessionsRepository);
//        m_processor.init(subs, m_messagesStore, m_sessionStore, null, true, new PermitAllAuthorizatorPolicy(),
//                NO_OBSERVERS_INTERCEPTOR, this.sessionsRepository, false);
//
//        connect_v3_1();
//
//        // Exercise
//        publishToAs("FakeCLI", NEWS_TOPIC, AT_MOST_ONCE, false);
//
//        // Verify
//        verifyPublishIsReceived();
//    }

    //same test moved into PostOfficePublishTest
//    @Test
//    public void testPublishToMultipleSubscribers() {
//        final Subscription subscription = new Subscription(FAKE_CLIENT_ID, new Topic(NEWS_TOPIC), AT_MOST_ONCE);
//        final Subscription subscriptionClient2 = new Subscription(
//                FAKE_CLIENT_ID2,
//                new Topic(NEWS_TOPIC),
//                AT_MOST_ONCE);
//
//        // subscriptions.matches(topic) redefine the method to return true
//        ISubscriptionsDirectory subs = new CTrieSubscriptionDirectory() {
//
//            @Override
//            public List<Subscription> matches(Topic topic) {
//                if (topic.toString().equals(NEWS_TOPIC)) {
//                    return Arrays.asList(subscription, subscriptionClient2);
//                } else {
//                    throw new IllegalArgumentException("Expected " + NEWS_TOPIC + " buf found " + topic);
//                }
//            }
//        };
//
//        // simulate a connect that register a clientID to an IoSession
//        MemoryStorageService storageService = new MemoryStorageService(null, null);
//        SessionsRepository sessionsRepository = new SessionsRepository(storageService.sessionsStore(), null);
//        subs.init(sessionsRepository);
//        m_processor.init(subs, m_messagesStore, m_sessionStore, null, true, new PermitAllAuthorizatorPolicy(),
//                NO_OBSERVERS_INTERCEPTOR, this.sessionsRepository, false);
//
//        EmbeddedChannel firstReceiverChannel = new EmbeddedChannel();
//        MqttConnectMessage connectMessage = MqttMessageBuilders.connect().protocolVersion(MqttVersion.MQTT_3_1)
//                .clientId(FAKE_CLIENT_ID).cleanSession(true).build();
//        m_processor.processConnect(firstReceiverChannel, connectMessage);
//        assertConnAckAccepted(firstReceiverChannel);
//
//        // connect the second fake subscriber
//        EmbeddedChannel secondReceiverChannel = new EmbeddedChannel();
//        MqttConnectMessage connectMessage2 = MqttMessageBuilders.connect().protocolVersion(MqttVersion.MQTT_3_1)
//                .clientId(FAKE_CLIENT_ID2).cleanSession(true).build();
//        m_processor.processConnect(secondReceiverChannel, connectMessage2);
//        assertConnAckAccepted(secondReceiverChannel);
//
//        // Exercise
//        MqttPublishMessage msg = MqttMessageBuilders.publish().topicName(NEWS_TOPIC).qos(AT_MOST_ONCE)
//                .retained(false).payload(Unpooled.copiedBuffer("Hello".getBytes(UTF_8))).build();
//        NettyUtils.userName(m_channel, "FakeCLI");
//        m_processor.processPublish(m_channel, msg);
//
//        // Verify
//        firstReceiverChannel.flush();
//        MqttPublishMessage pub2FirstSubscriber = firstReceiverChannel.readOutbound();
//        assertNotNull(pub2FirstSubscriber);
//        String firstMessageContent = DebugUtils.payload2Str(pub2FirstSubscriber.payload());
//        assertEquals("Hello", firstMessageContent);
//
//        secondReceiverChannel.flush();
//        MqttPublishMessage pub2SecondSubscriber = secondReceiverChannel.readOutbound();
//        assertNotNull(pub2SecondSubscriber);
//        String secondMessageContent = DebugUtils.payload2Str(pub2SecondSubscriber.payload());
//        assertEquals("Hello", secondMessageContent);
//    }
//
//    //TODO move this test on PostOfficeSubscribeTest
//    @Test
//    public void testPublishOfRetainedMessage_afterNewSubscription() throws Exception {
//        // simulate a connect that register a clientID to an IoSession
//        final Subscription subscription =
//                new Subscription(PUBLISHER_ID, new Topic(NEWS_TOPIC), AT_MOST_ONCE);
//
//        // subscriptions.matches(topic) redefine the method to return true
//        ISubscriptionsDirectory subs = new CTrieSubscriptionDirectory() {
//
//            @Override
//            public List<Subscription> matches(Topic topic) {
//                if (topic.toString().equals(NEWS_TOPIC)) {
//                    return Collections.singletonList(subscription);
//                } else {
//                    throw new IllegalArgumentException("Expected " + NEWS_TOPIC + " buf found " + topic);
//                }
//            }
//        };
//        MemoryStorageService storageService = new MemoryStorageService(null, null);
//        SessionsRepository sessionsRepository = new SessionsRepository(storageService.sessionsStore(), null);
//        subs.init(sessionsRepository);
//
//        // simulate a connect that register a clientID to an IoSession
//        m_processor.init(subs, m_messagesStore, m_sessionStore, null, true, new PermitAllAuthorizatorPolicy(),
//                NO_OBSERVERS_INTERCEPTOR, this.sessionsRepository, false);
//        connect_v3_1_asClient(PUBLISHER_ID);
//        publishToAs(PUBLISHER_ID, NEWS_TOPIC, AT_MOST_ONCE, true);
//        NettyUtils.cleanSession(m_channel, false);
//
//        // Exercise
//        subscribeAndNotReadResponse("#", AT_MOST_ONCE);
//
//        // Verify
//        verifyPublishIsReceived();
//    }

    @Test
    public void testRepublishAndConsumePersistedMessages_onReconnect() {
        ISubscriptionsDirectory subs = mock(ISubscriptionsDirectory.class);
        List<Subscription> emptySubs = Collections.emptyList();
        when(subs.matches(any(Topic.class))).thenReturn(emptySubs);

        StoredMessage retainedMessage = new StoredMessage("Hello".getBytes(UTF_8), EXACTLY_ONCE, "/topic");
        retainedMessage.setRetained(true);
        retainedMessage.setClientID(PUBLISHER_ID);
        m_messagesStore.storeRetained(new Topic("/topic"), retainedMessage);

        m_processor.init(subs, m_messagesStore, m_sessionStore, null, true, new PermitAllAuthorizatorPolicy(),
                NO_OBSERVERS_INTERCEPTOR, this.sessionsRepository, false);

        connect_v3_1_asClient(PUBLISHER_ID);

        // Verify no messages are still stored
        assertTrue(this.sessionsRepository.sessionForClient(PUBLISHER_ID).isEmptyQueue());
    }

//    Moved to PostOfficePublishTest
//    @Test
//    public void publishNoPublishToInactiveSession() {
//        // create an inactive session for Subscriber
//        ISubscriptionsDirectory mockedSubscriptions = mock(ISubscriptionsDirectory.class);
//        Subscription inactiveSub = new Subscription("Subscriber", new Topic("/topic"), MqttQoS.AT_LEAST_ONCE);
//        List<Subscription> inactiveSubscriptions = Collections.singletonList(inactiveSub);
//        when(mockedSubscriptions.matches(eq(new Topic("/topic")))).thenReturn(inactiveSubscriptions);
//        m_processor = new ProtocolProcessor();
//        m_processor.init(mockedSubscriptions, m_messagesStore, m_sessionStore, null, true, new PermitAllAuthorizatorPolicy(),
//                NO_OBSERVERS_INTERCEPTOR, this.sessionsRepository, false);
//
//        m_processor.sessionsRepository.createNewSession("Subscriber", false);
//
//        // Exercise
//        connectAsClient("Publisher");
//        publishToAs("Publisher", "/topic", AT_MOST_ONCE, true);
//
//        verifyNoPublishIsReceived();
//    }
//
//    /**
//     * Verify that receiving a publish with retained message and with Q0S = 0 clean the existing
//     * retained messages for that topic.
//     */
//    @Test
//    public void testCleanRetainedStoreAfterAQoS0AndRetainedTrue() {
//        // force a connect
//        connect_v3_1_asClient("Publisher");
//
//        // prepare and existing retained store
//        publishToAs("Publisher", NEWS_TOPIC, AT_LEAST_ONCE, 100, true);
//
//        Collection<IMessagesStore.StoredMessage> messages = m_messagesStore
//                .searchMatching(key -> key.match(new Topic(NEWS_TOPIC)));
//        assertFalse(messages.isEmpty());
//
//        // Exercise
//        // send a message that clean the previous retained publish
//        publishToAs("Publisher", NEWS_TOPIC, AT_MOST_ONCE, true);
//
//        // Verify
//        messages = m_messagesStore.searchMatching(key -> key.match(new Topic(NEWS_TOPIC)));
//        assertTrue(messages.isEmpty());
//    }
//
//    @Test
//    public void testLowerTheQosToTheRequestedBySubscription() {
//        Subscription subQos1 = new Subscription("Sub A", new Topic("a/b"), MqttQoS.AT_LEAST_ONCE);
//        assertEquals(MqttQoS.AT_LEAST_ONCE, lowerQosToTheSubscriptionDesired(subQos1, EXACTLY_ONCE));
//
//        Subscription subQos2 = new Subscription("Sub B", new Topic("a/+"), EXACTLY_ONCE);
//        assertEquals(EXACTLY_ONCE, lowerQosToTheSubscriptionDesired(subQos2, EXACTLY_ONCE));
//    }
//
//    @Test
//    public void testSubscribeReceivePublishedMessageAtSubscriberQoSAndNotPublisherQoS() {
//        ISubscriptionsDirectory subs = new CTrieSubscriptionDirectory();
//        MemoryStorageService storageService = new MemoryStorageService(null, null);
//        SessionsRepository sessionsRepository = new SessionsRepository(storageService.sessionsStore(), null);
//        subs.init(sessionsRepository);
//
//        // simulate a connect that register a clientID to an IoSession
//        m_processor.init(subs, m_messagesStore, m_sessionStore, null, true, new PermitAllAuthorizatorPolicy(),
//                         NO_OBSERVERS_INTERCEPTOR, this.sessionsRepository, false);
//        connect_v3_1_asClient(PUBLISHER_ID);
//        publishQoS2ToAs(this.m_channel, PUBLISHER_ID, NEWS_TOPIC, 1, true);
//        disconnect();
//
//        // connect from the subscriber
//        EmbeddedChannel subscriberChannel = new EmbeddedChannel();
//        NettyUtils.clientID(subscriberChannel, SUBSCRIBER_ID);
//        NettyUtils.cleanSession(subscriberChannel, false);
//        connectAsClient(subscriberChannel, SUBSCRIBER_ID);
//
//        // subscribe to the topic news with granted QoS0
//        subscribe(subscriberChannel, NEWS_TOPIC, MqttQoS.AT_MOST_ONCE);
//
//        // Verify the retained message arrives with QoS0 instead of QoS2
//        verifyPublishIsReceived(subscriberChannel, HELLO_WORLD_MQTT, MqttQoS.AT_MOST_ONCE);
//    }
}
