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

import io.moquette.persistence.MemoryStorageService;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.ClientSession;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.security.PermitAllAuthorizator;
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
import static io.netty.handler.codec.mqtt.MqttConnectReturnCode.*;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProtocolProcessor_CONNECT_Test {

    EmbeddedChannel m_session;
    MqttMessageBuilders.ConnectBuilder connMsg;
    ProtocolProcessor m_processor;

    IMessagesStore m_messagesStore;
    ISessionsStore m_sessionStore;
    ISubscriptionsDirectory subscriptions;
    MockAuthenticator m_mockAuthenticator;

    @Before
    public void setUp() throws InterruptedException {
        connMsg = MqttMessageBuilders.connect().protocolVersion(MqttVersion.MQTT_3_1).cleanSession(true);

        m_session = new EmbeddedChannel();

        MemoryStorageService memStorage = new MemoryStorageService(null, null);
        m_messagesStore = memStorage.messagesStore();
        m_sessionStore = memStorage.sessionsStore();
        // m_messagesStore.initStore();

        m_mockAuthenticator = new MockAuthenticator(singleton(FAKE_CLIENT_ID), singletonMap(TEST_USER, TEST_PWD));

        subscriptions = new CTrieSubscriptionDirectory();
        SessionsRepository sessionsRepository = new SessionsRepository(m_sessionStore, null);
        subscriptions.init(sessionsRepository);
        m_processor = new ProtocolProcessor();
        m_processor.init(subscriptions, m_messagesStore, m_sessionStore, m_mockAuthenticator, true,
                new PermitAllAuthorizator(), NO_OBSERVERS_INTERCEPTOR, new SessionsRepository(this.m_sessionStore, null));
    }

    @Test
    public void testConnect_badClientID() {
        connMsg.clientId("extremely_long_clientID_greater_than_23").build();

        // Exercise
        m_processor.processConnect(m_session, connMsg.clientId("extremely_long_clientID_greater_than_23").build());

        // Verify
        assertEqualsConnAck(CONNECTION_ACCEPTED, m_session.readOutbound());
    }

    @Test
    public void testWill() {
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID).willFlag(true)
                .willTopic("topic").willMessage("Topic message").build();

        // Exercise
        // m_handler.setMessaging(mockedMessaging);
        m_processor.processConnect(m_session, msg);

        // Verify
        assertEqualsConnAck(CONNECTION_ACCEPTED, m_session.readOutbound());
        assertTrue("Connection is accepted and therefore should remain open.", m_session.isOpen());
        // TODO verify the call
        /*
         * verify(mockedMessaging).publish(eq("topic"), eq("Topic message".getBytes()),
         * any(AbstractMessage.QOSType.class), anyBoolean(), eq("123"), any(IoSession.class));
         */
    }

    @Test
    public void validAuthentication() {
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID)
                .username(TEST_USER).password(TEST_PWD).build();

        // Exercise
        m_processor.processConnect(m_session, msg);

        // Verify
        assertEqualsConnAck(CONNECTION_ACCEPTED, m_session.readOutbound());
        assertTrue("Connection is accepted and therefore should remain open.", m_session.isOpen());
    }

    @Test
    public void noPasswdAuthentication() {
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID)
            .username(TEST_USER)
            .build();

        // Exercise
        m_processor.processConnect(m_session, msg);

        // Verify
        assertEqualsConnAck(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, m_session.readOutbound());
        assertFalse("Connection should be closed by the broker.", m_session.isOpen());
    }

    @Test
    public void invalidAuthentication() {
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID)
            .username(TEST_USER + "_fake")
            .password(TEST_PWD)
            .build();

        // Exercise
        m_processor.processConnect(m_session, msg);

        // Verify
        assertEqualsConnAck(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, m_session.readOutbound());
        assertFalse("Connection should be closed by the broker.", m_session.isOpen());
    }

    @Test
    public void prohibitAnonymousClient() {
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID).build();
        reinitProcessorProhibitingAnonymousClients();

        // Exercise
        m_processor.processConnect(m_session, msg);

        // Verify
        assertEqualsConnAck(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, m_session.readOutbound());
        assertFalse("Connection should be closed by the broker.", m_session.isOpen());
    }

    protected void reinitProcessorProhibitingAnonymousClients() {
        m_processor.init(subscriptions, m_messagesStore, m_sessionStore, m_mockAuthenticator, false,
            new PermitAllAuthorizator(), NO_OBSERVERS_INTERCEPTOR, new SessionsRepository(this.m_sessionStore, null));
    }

    @Test
    public void prohibitAnonymousClient_providingUsername() {
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID)
            .username(TEST_USER + "_fake")
            .build();
        reinitProcessorProhibitingAnonymousClients();

        // Exercise
        m_processor.processConnect(m_session, msg);

        // Verify
        assertEqualsConnAck(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, m_session.readOutbound());
        assertFalse("Connection should be closed by the broker.", m_session.isOpen());
    }

    @Test
    public void acceptAnonymousClient() {
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID).build();

        // Exercise
        m_processor.processConnect(m_session, msg);

        // Verify
        assertEqualsConnAck(CONNECTION_ACCEPTED, m_session.readOutbound());
        assertTrue("Connection is accepted and therefore should remain open.", m_session.isOpen());
    }

    @Test
    public void connectWithCleanSessionUpdateClientSession() throws InterruptedException {
        // first connect with clean session true
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID).cleanSession(true).build();
        m_processor.processConnect(m_session, msg);
        assertEqualsConnAck(CONNECTION_ACCEPTED, m_session.readOutbound());
        m_processor.processDisconnect(m_session);
        assertFalse(m_session.isOpen());

        // second connect with clean session false
        m_session = new EmbeddedChannel();
        MqttConnectMessage secondConnMsg = MqttMessageBuilders.connect().clientId(FAKE_CLIENT_ID)
                .protocolVersion(MqttVersion.MQTT_3_1).build();

        m_processor.processConnect(m_session, secondConnMsg);
        assertEqualsConnAck(CONNECTION_ACCEPTED, m_session.readOutbound());

        // Verify client session is clean false
        ClientSession session = m_processor.sessionsRepository.sessionForClient(FAKE_CLIENT_ID);
        assertFalse(session.isCleanSession());

        // Verify
        // assertEqualsConnAck(CONNECTION_ACCEPTED, m_session.readOutbound());
    }

    @Test
    public void connectWithSameClientIDBadCredentialsDoesntDropExistingClient() {
        // Connect a client1
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID)
            .username(TEST_USER)
            .password(TEST_PWD)
            .build();
        m_processor.processConnect(m_session, msg);
        assertEqualsConnAck(CONNECTION_ACCEPTED, m_session.readOutbound());

        // create another connect same clientID but with bad credentials
        MqttConnectMessage evilClientConnMsg = MqttMessageBuilders.connect()
            .protocolVersion(MqttVersion.MQTT_3_1)
            .clientId(FAKE_CLIENT_ID)
            .username(ProtocolProcessorTest.EVIL_TEST_USER)
            .password(ProtocolProcessorTest.EVIL_TEST_PWD)
            .build();

        EmbeddedChannel evilSession = new EmbeddedChannel();

        // Exercise
        m_processor.processConnect(evilSession, evilClientConnMsg);

        // Verify
        // the evil client gets a not auth notification
        assertEqualsConnAck(CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, evilSession.readOutbound());
        // the good client remains connected
        assertTrue(m_session.isOpen());
        assertFalse(evilSession.isOpen());
    }

    @Test
    public void testConnAckContainsSessionPresentFlag() throws InterruptedException {
        MqttConnectMessage msg = connMsg.clientId(FAKE_CLIENT_ID)
                .protocolVersion(MqttVersion.MQTT_3_1_1).build();
        NettyUtils.clientID(m_session, FAKE_CLIENT_ID);
        NettyUtils.cleanSession(m_session, false);

        // Connect a first time
        m_processor.processConnect(m_session, msg);
        // disconnect
        m_processor.processDisconnect(m_session);

        // Exercise, reconnect
        EmbeddedChannel firstReceiverSession = new EmbeddedChannel();
        m_processor.processConnect(firstReceiverSession, msg);

        // Verify
        assertEqualsConnAck(CONNECTION_ACCEPTED, firstReceiverSession.readOutbound());
        assertTrue("Connection is accepted and therefore should remain open.", firstReceiverSession.isOpen());
    }

    @Test
    public void testMultipleReconnection() throws InterruptedException {
        // connect with clean a false and subscribe to a topic
        MqttConnectMessage msg = MqttMessageBuilders.connect().clientId(FAKE_CLIENT_ID)
                .protocolVersion(MqttVersion.MQTT_3_1_1).build();
        m_processor.processConnect(m_session, msg);
        assertEqualsConnAck(CONNECTION_ACCEPTED, m_session.readOutbound());
        assertTrue("Connection is accepted and therefore should remain open.", m_session.isOpen());

        // subscribe
        MqttSubscribeMessage subscribeMsg = MqttMessageBuilders.subscribe()
                .addSubscription(MqttQoS.AT_MOST_ONCE, ProtocolProcessorTest.FAKE_TOPIC).messageId(10).build();

        NettyUtils.clientID(m_session, FAKE_CLIENT_ID);
        NettyUtils.cleanSession(m_session, false);
        m_processor.processSubscribe(m_session, subscribeMsg);
        Subscription expectedSubscription = new Subscription(
                FAKE_CLIENT_ID,
                new Topic(ProtocolProcessorTest.FAKE_TOPIC),
                MqttQoS.AT_MOST_ONCE);
        verifySubscriptionExists(m_session, m_sessionStore, expectedSubscription);
        assertEqualsSubAck(m_session.readOutbound());

        // disconnect
        m_processor.processDisconnect(m_session);
        assertFalse(m_session.isOpen());

        // reconnect clean session a false
        m_session = new EmbeddedChannel();
        m_processor.processConnect(m_session, msg);
        assertEqualsConnAck(CONNECTION_ACCEPTED, m_session.readOutbound());
        assertTrue("Connection is accepted and therefore should remain open.", m_session.isOpen());

        // verify that the first subscription is still preserved
        verifySubscriptionExists(m_session, m_sessionStore, expectedSubscription);
    }

    @Test
    public void testZeroByteClientIdWithoutCleanSession() {
        // Allow zero byte client ids
        m_processor = new ProtocolProcessor();
        reinitProtocolProcessorWithZeroLengthClientIdAndAnonymousClients();

        // Connect message without clean session set to true but client id is still null
        MqttConnectMessage msg = MqttMessageBuilders.connect().clientId(null).protocolVersion(MqttVersion.MQTT_3_1_1)
                .build();

        m_processor.processConnect(m_session, msg);
        assertEqualsConnAck(
                "Identifier should be rejected due to having clean session set to false.",
                CONNECTION_REFUSED_IDENTIFIER_REJECTED,
                m_session.readOutbound());

        assertFalse("Connection should be closed by the broker.", m_session.isOpen());
    }

    protected void reinitProtocolProcessorWithZeroLengthClientIdAndAnonymousClients() {
        m_processor.init(subscriptions, m_messagesStore, m_sessionStore, m_mockAuthenticator, true, true,
            new PermitAllAuthorizator(), NO_OBSERVERS_INTERCEPTOR, new SessionsRepository(this.m_sessionStore, null));
    }

    @Test
    public void testZeroByteClientIdWithCleanSession() {
        // Allow zero byte client ids
        m_processor = new ProtocolProcessor();
        reinitProtocolProcessorWithZeroLengthClientIdAndAnonymousClients();

        // Connect message with clean session set to true and client id is null.
        MqttConnectMessage msg = connMsg.clientId(null)
            .protocolVersion(MqttVersion.MQTT_3_1_1)
            .cleanSession(true)
            .build();

        m_processor.processConnect(m_session, msg);
        assertEqualsConnAck("Connection MUST be accepted, unique clientid MUST be generated and clean session to true",
                CONNECTION_ACCEPTED, m_session.readOutbound());
        assertTrue("Connection should be valid and open,", m_session.isOpen());
    }

    @Test
    public void testZeroByteClientIdNotAllowed() {
        // Connect message with clean session set to true and client id is null.
        MqttConnectMessage msg = connMsg.clientId(null)
            .protocolVersion(MqttVersion.MQTT_3_1_1)
            .cleanSession(true)
            .build();

        m_processor.processConnect(m_session, msg);
        assertEqualsConnAck("Zero byte client identifiers are not allowed",
            CONNECTION_REFUSED_IDENTIFIER_REJECTED, m_session.readOutbound());

        assertFalse("Connection should closed.", m_session.isOpen());
    }
}
