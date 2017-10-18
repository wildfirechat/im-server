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
import io.moquette.spi.IMessagesStore.StoredMessage;
import io.moquette.spi.impl.security.PermitAllAuthorizator;
import io.moquette.spi.impl.subscriptions.CTrieSubscriptionDirectory;
import io.moquette.spi.impl.subscriptions.ISubscriptionsDirectory;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;
import io.moquette.spi.security.IAuthorizator;
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
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ProtocolProcessorTest extends AbstractProtocolProcessorCommonUtils {

    static final String FAKE_CLIENT_ID = "FAKE_123";
    static final String FAKE_CLIENT_ID2 = "FAKE_456";
    static final String FAKE_PUBLISHER_ID = "Publisher";
    static final String FAKE_TOPIC = "/news";
    static final String BAD_FORMATTED_TOPIC = "#MQTTClient";

    static final String TEST_USER = "fakeuser";
    static final String TEST_PWD = "fakepwd";
    static final String EVIL_TEST_USER = "eviluser";
    static final String EVIL_TEST_PWD = "unsecret";

    static final List<InterceptHandler> EMPTY_OBSERVERS = Collections.emptyList();
    static final BrokerInterceptor NO_OBSERVERS_INTERCEPTOR = new BrokerInterceptor(EMPTY_OBSERVERS);


    @Before
    public void setUp() throws InterruptedException {
        initializeProcessorAndSubsystems();
    }

    @Test
    public void testPublishToItself() throws InterruptedException {
        final Subscription subscription = new Subscription(FAKE_CLIENT_ID, new Topic(FAKE_TOPIC), AT_MOST_ONCE);

        // subscriptions.matches(topic) redefine the method to return true
        ISubscriptionsDirectory subs = new CTrieSubscriptionDirectory() {

            @Override
            public List<Subscription> matches(Topic topic) {
                if (topic.toString().equals(FAKE_TOPIC)) {
                    return Collections.singletonList(subscription);
                } else {
                    throw new IllegalArgumentException("Expected " + FAKE_TOPIC + " buf found " + topic);
                }
            }
        };

        // simulate a connect that register a clientID to an IoSession
        MemoryStorageService storageService = new MemoryStorageService(null, null);
        SessionsRepository sessionsRepository = new SessionsRepository(storageService.sessionsStore(), null);
        subs.init(sessionsRepository);
        m_processor.init(subs, m_messagesStore, m_sessionStore, null, true, new PermitAllAuthorizator(),
                NO_OBSERVERS_INTERCEPTOR, this.sessionsRepository);

        connect_v3_1();

        // Exercise
        publishToAs("FakeCLI", FAKE_TOPIC, AT_MOST_ONCE, false);

        // Verify
        verifyPublishIsReceived();
    }

    @Test
    public void testPublishToMultipleSubscribers() throws InterruptedException {
        final Subscription subscription = new Subscription(FAKE_CLIENT_ID, new Topic(FAKE_TOPIC), AT_MOST_ONCE);
        final Subscription subscriptionClient2 = new Subscription(
                FAKE_CLIENT_ID2,
                new Topic(FAKE_TOPIC),
                AT_MOST_ONCE);

        // subscriptions.matches(topic) redefine the method to return true
        ISubscriptionsDirectory subs = new CTrieSubscriptionDirectory() {

            @Override
            public List<Subscription> matches(Topic topic) {
                if (topic.toString().equals(FAKE_TOPIC)) {
                    return Arrays.asList(subscription, subscriptionClient2);
                } else {
                    throw new IllegalArgumentException("Expected " + FAKE_TOPIC + " buf found " + topic);
                }
            }
        };

        // simulate a connect that register a clientID to an IoSession
        MemoryStorageService storageService = new MemoryStorageService(null, null);
        SessionsRepository sessionsRepository = new SessionsRepository(storageService.sessionsStore(), null);
        subs.init(sessionsRepository);
        m_processor.init(subs, m_messagesStore, m_sessionStore, null, true, new PermitAllAuthorizator(),
                NO_OBSERVERS_INTERCEPTOR, this.sessionsRepository);

        EmbeddedChannel firstReceiverChannel = new EmbeddedChannel();
        MqttConnectMessage connectMessage = MqttMessageBuilders.connect().protocolVersion(MqttVersion.MQTT_3_1)
                .clientId(FAKE_CLIENT_ID).cleanSession(true).build();
        m_processor.processConnect(firstReceiverChannel, connectMessage);
        assertConnAckAccepted(firstReceiverChannel);

        // connect the second fake subscriber
        EmbeddedChannel secondReceiverChannel = new EmbeddedChannel();
        MqttConnectMessage connectMessage2 = MqttMessageBuilders.connect().protocolVersion(MqttVersion.MQTT_3_1)
                .clientId(FAKE_CLIENT_ID2).cleanSession(true).build();
        m_processor.processConnect(secondReceiverChannel, connectMessage2);
        assertConnAckAccepted(secondReceiverChannel);

        // Exercise
        MqttPublishMessage msg = MqttMessageBuilders.publish().topicName(FAKE_TOPIC).qos(AT_MOST_ONCE)
                .retained(false).payload(Unpooled.copiedBuffer("Hello".getBytes())).build();
        NettyUtils.userName(m_channel, "FakeCLI");
        m_processor.processPublish(m_channel, msg);

        // Verify
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
        connect();

        // Exercise & verify
        subscribe(FAKE_TOPIC, AT_MOST_ONCE);
    }

    @Test
    public void testSubscribedToNotAuthorizedTopic() {
        final String fakeUserName = "UnAuthUser";
        NettyUtils.userName(m_channel, fakeUserName);

        IAuthorizator mockAuthorizator = mock(IAuthorizator.class);
        when(mockAuthorizator.canRead(eq(new Topic(FAKE_TOPIC)), eq(fakeUserName), eq(FAKE_CLIENT_ID)))
            .thenReturn(false);

        m_processor.init(subscriptions, m_messagesStore, m_sessionStore, m_mockAuthenticator, true, mockAuthorizator,
                NO_OBSERVERS_INTERCEPTOR, this.sessionsRepository);

        connect();

        //Exercise
        MqttSubAckMessage subAckMsg = subscribeWithoutVerify(FAKE_TOPIC, AT_MOST_ONCE);

        verifyFailureQos(subAckMsg);
    }

    private void verifyFailureQos(MqttSubAckMessage subAckMsg) {
        List<Integer> grantedQoSes = subAckMsg.payload().grantedQoSLevels();
        assertEquals(1, grantedQoSes.size());
        assertTrue(grantedQoSes.contains(MqttQoS.FAILURE.value()));
    }

    @Test
    public void testDoubleSubscribe() {
        connect();
        assertEquals(0, subscriptions.size());
        subscribe(FAKE_TOPIC, AT_MOST_ONCE);
        assertEquals(1, subscriptions.size());

        //Exercise & verify
        subscribe(FAKE_TOPIC, AT_MOST_ONCE);
    }

    @Test
    public void testSubscribeWithBadFormattedTopic() {
        connect();
        assertEquals(0, subscriptions.size());

        //Exercise
        MqttSubAckMessage subAckMsg = subscribeWithoutVerify(BAD_FORMATTED_TOPIC, AT_MOST_ONCE);

        assertEquals(0, subscriptions.size());
        verifyFailureQos(subAckMsg);
    }

    @Test
    public void testUnsubscribeWithBadFormattedTopic() {
        // Exercise
        unsubscribe(BAD_FORMATTED_TOPIC);

        // Verify
        assertFalse("If client unsubscribe with bad topic than channel must be closed, (issue 68)", m_channel.isOpen());
    }

    @Test
    public void testPublishOfRetainedMessage_afterNewSubscription() throws Exception {
        // simulate a connect that register a clientID to an IoSession
        final Subscription subscription =
                new Subscription(FAKE_PUBLISHER_ID, new Topic(FAKE_TOPIC), AT_MOST_ONCE);

        // subscriptions.matches(topic) redefine the method to return true
        ISubscriptionsDirectory subs = new CTrieSubscriptionDirectory() {

            @Override
            public List<Subscription> matches(Topic topic) {
                if (topic.toString().equals(FAKE_TOPIC)) {
                    return Collections.singletonList(subscription);
                } else {
                    throw new IllegalArgumentException("Expected " + FAKE_TOPIC + " buf found " + topic);
                }
            }
        };
        MemoryStorageService storageService = new MemoryStorageService(null, null);
        SessionsRepository sessionsRepository = new SessionsRepository(storageService.sessionsStore(), null);
        subs.init(sessionsRepository);

        // simulate a connect that register a clientID to an IoSession
        m_processor.init(subs, m_messagesStore, m_sessionStore, null, true, new PermitAllAuthorizator(),
                NO_OBSERVERS_INTERCEPTOR, this.sessionsRepository);
        connect_v3_1_asClient(FAKE_PUBLISHER_ID);
        publishToAs(FAKE_PUBLISHER_ID, FAKE_TOPIC, AT_MOST_ONCE, true);
        NettyUtils.cleanSession(m_channel, false);

        // Exercise
        subscribeAndNotReadResponse("#", AT_MOST_ONCE);

        // Verify
        verifyPublishIsReceived();
    }

    @Test
    public void testRepublishAndConsumePersistedMessages_onReconnect() {
        ISubscriptionsDirectory subs = mock(ISubscriptionsDirectory.class);
        List<Subscription> emptySubs = Collections.emptyList();
        when(subs.matches(any(Topic.class))).thenReturn(emptySubs);

        StoredMessage retainedMessage = new StoredMessage("Hello".getBytes(), MqttQoS.EXACTLY_ONCE, "/topic");
        retainedMessage.setRetained(true);
        retainedMessage.setClientID(FAKE_PUBLISHER_ID);
        m_messagesStore.storeRetained(new Topic("/topic"), retainedMessage);

        m_processor.init(subs, m_messagesStore, m_sessionStore, null, true, new PermitAllAuthorizator(),
                NO_OBSERVERS_INTERCEPTOR, this.sessionsRepository);

        connect_v3_1_asClient(FAKE_PUBLISHER_ID);

        // Verify no messages are still stored
        assertTrue(this.sessionsRepository.sessionForClient(FAKE_PUBLISHER_ID).isEmptyQueue());
    }

    @Test
    public void publishNoPublishToInactiveSession() {
        // create an inactive session for Subscriber
        ISubscriptionsDirectory mockedSubscriptions = mock(ISubscriptionsDirectory.class);
        Subscription inactiveSub = new Subscription("Subscriber", new Topic("/topic"), MqttQoS.AT_LEAST_ONCE);
        List<Subscription> inactiveSubscriptions = Collections.singletonList(inactiveSub);
        when(mockedSubscriptions.matches(eq(new Topic("/topic")))).thenReturn(inactiveSubscriptions);
        m_processor = new ProtocolProcessor();
        m_processor.init(mockedSubscriptions, m_messagesStore, m_sessionStore, null, true, new PermitAllAuthorizator(),
                NO_OBSERVERS_INTERCEPTOR, this.sessionsRepository);

        m_processor.sessionsRepository.createNewSession("Subscriber", false);

        // Exercise
        connectAsClient("Publisher");
        publishToAs("Publisher", "/topic", AT_MOST_ONCE, true);

        verifyNoPublishIsReceived();
    }

    /**
     * Verify that receiving a publish with retained message and with Q0S = 0 clean the existing
     * retained messages for that topic.
     */
    @Test
    public void testCleanRetainedStoreAfterAQoS0AndRetainedTrue() {
        // force a connect
        connect_v3_1_asClient("Publisher");

        // prepare and existing retained store
        publishToAs("Publisher", FAKE_TOPIC, AT_LEAST_ONCE, 100, true);

        Collection<IMessagesStore.StoredMessage> messages = m_messagesStore
                .searchMatching(key -> key.match(new Topic(FAKE_TOPIC)));
        assertFalse(messages.isEmpty());

        // Exercise
        // send a message that clean the previous retained publish
        publishToAs("Publisher", FAKE_TOPIC, AT_MOST_ONCE, true);

        // Verify
        messages = m_messagesStore.searchMatching(key -> key.match(new Topic(FAKE_TOPIC)));
        assertTrue(messages.isEmpty());
    }

    @Test
    public void testLowerTheQosToTheRequestedBySubscription() {
        Subscription subQos1 = new Subscription("Sub A", new Topic("a/b"), MqttQoS.AT_LEAST_ONCE);
        assertEquals(MqttQoS.AT_LEAST_ONCE, lowerQosToTheSubscriptionDesired(subQos1, MqttQoS.EXACTLY_ONCE));

        Subscription subQos2 = new Subscription("Sub B", new Topic("a/+"), MqttQoS.EXACTLY_ONCE);
        assertEquals(MqttQoS.EXACTLY_ONCE, lowerQosToTheSubscriptionDesired(subQos2, MqttQoS.EXACTLY_ONCE));
    }
}
