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

import io.moquette.persistence.MemoryStorageService;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.security.PermitAllAuthorizatorPolicy;
import io.moquette.spi.impl.subscriptions.CTrieSubscriptionDirectory;
import io.moquette.spi.impl.subscriptions.ISubscriptionsDirectory;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.*;
import org.junit.Before;
import org.junit.Test;

import static io.moquette.spi.impl.NettyChannelAssertions.assertEqualsConnAck;
import static io.moquette.spi.impl.NettyChannelAssertions.assertEqualsSubAck;
import static io.moquette.spi.impl.ProtocolProcessorTest.*;
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.CONNECTION_ACCEPTED;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProtocolProcessor_CONNECT_Test {

    private EmbeddedChannel session;
    private ProtocolProcessor processor;
    private ISessionsStore sessionStore;

    @Before
    public void setUp() {
        session = new EmbeddedChannel();

        MemoryStorageService memStorage = new MemoryStorageService(null, null);
        IMessagesStore m_messagesStore = memStorage.messagesStore();
        sessionStore = memStorage.sessionsStore();

        MockAuthenticator mockAuthenticator = new MockAuthenticator(singleton(FAKE_CLIENT_ID), singletonMap(TEST_USER, TEST_PWD));

        ISubscriptionsDirectory subscriptions = new CTrieSubscriptionDirectory();
        SessionsRepository sessionsRepository = new SessionsRepository(sessionStore, null);
        subscriptions.init(sessionsRepository);
        processor = new ProtocolProcessor();
        processor.init(subscriptions, m_messagesStore, sessionStore, mockAuthenticator, true,
                         new PermitAllAuthorizatorPolicy(), NO_OBSERVERS_INTERCEPTOR,
                         new SessionsRepository(this.sessionStore, null), false);
    }

    @Test
    public void testMultipleReconnection() {
        // connect with clean a false and subscribe to a topic
        MqttConnectMessage msg = MqttMessageBuilders.connect().clientId(FAKE_CLIENT_ID)
                .protocolVersion(MqttVersion.MQTT_3_1_1).build();
        processor.processConnect(session, msg);
        assertEqualsConnAck(CONNECTION_ACCEPTED, session.readOutbound());
        assertTrue("Connection is accepted and therefore should remain open.", session.isOpen());

        // subscribe
        MqttSubscribeMessage subscribeMsg = MqttMessageBuilders.subscribe()
                .addSubscription(MqttQoS.AT_MOST_ONCE, ProtocolProcessorTest.NEWS_TOPIC).messageId(10).build();

        NettyUtils.clientID(session, FAKE_CLIENT_ID);
        NettyUtils.cleanSession(session, false);
        processor.processSubscribe(session, subscribeMsg);
        final Topic newTopic = new Topic(ProtocolProcessorTest.NEWS_TOPIC);
        Subscription expectedSubscription = new Subscription(FAKE_CLIENT_ID, newTopic, MqttQoS.AT_MOST_ONCE);
        verifySubscriptionExists(sessionStore, expectedSubscription);
        assertEqualsSubAck(session.readOutbound());

        // disconnect
        processor.processDisconnect(session);
        assertFalse(session.isOpen());

        // reconnect clean session a false
        session = new EmbeddedChannel();
        processor.processConnect(session, msg);
        assertEqualsConnAck(CONNECTION_ACCEPTED, session.readOutbound());
        assertTrue("Connection is accepted and therefore should remain open.", session.isOpen());

        // verify that the first subscription is still preserved
        verifySubscriptionExists(sessionStore, expectedSubscription);
    }
}
