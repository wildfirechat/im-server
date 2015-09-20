/*
 * Copyright (c) 2012-2015 The original author or authors
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
package org.eclipse.moquette.spi.impl;

import org.eclipse.moquette.proto.messages.AbstractMessage;
import org.eclipse.moquette.proto.messages.ConnAckMessage;
import org.eclipse.moquette.proto.messages.ConnectMessage;
import org.eclipse.moquette.proto.messages.DisconnectMessage;
import org.eclipse.moquette.server.netty.NettyChannel;
import org.eclipse.moquette.spi.IMessagesStore;
import org.eclipse.moquette.spi.ISessionsStore;
import org.eclipse.moquette.spi.impl.security.PermitAllAuthorizator;
import org.eclipse.moquette.spi.impl.subscriptions.SubscriptionsStore;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.eclipse.moquette.spi.impl.ProtocolProcessorTest.*;
import static org.eclipse.moquette.parser.netty.Utils.VERSION_3_1_1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author andrea
 */
public class ProtocolProcessor_CONNECT_Test {

    DummyChannel m_session;
    ConnectMessage connMsg;
    ProtocolProcessor m_processor;

    IMessagesStore m_storageService;
    ISessionsStore m_sessionStore;
    SubscriptionsStore subscriptions;
    MockAuthenticator m_mockAuthenticator;

    @Before
    public void setUp() throws InterruptedException {
        connMsg = new ConnectMessage();
        connMsg.setProtocolVersion((byte) 0x03);

        m_session = new DummyChannel();

        //sleep to let the messaging batch processor to process the initEvent
        Thread.sleep(300);
        MemoryStorageService memStorage = new MemoryStorageService();
        m_storageService = memStorage;
        m_sessionStore = memStorage;
        //m_storageService.initStore();

        Map<String, byte[]> users = new HashMap<>();
        users.put(TEST_USER, TEST_PWD);
        m_mockAuthenticator = new MockAuthenticator(users);

        subscriptions = new SubscriptionsStore();
        subscriptions.init(new MemoryStorageService());
        m_processor = new ProtocolProcessor();
        m_processor.init(subscriptions, m_storageService, m_sessionStore, m_mockAuthenticator, true,
                new PermitAllAuthorizator(), ProtocolProcessorTest.NO_OBSERVERS_INTERCEPTOR);
    }

    @Test
    public void testHandleConnect_BadProtocol() {
        connMsg.setProtocolVersion((byte) 0x02);

        //Exercise
        m_processor.processConnect(m_session, connMsg);

        //Verify
        assertEquals(ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION, m_session.getReturnCode());
    }

    @Test
    public void testConnect_badClientID() {
        connMsg.setClientID("extremely_long_clientID_greater_than_23");

        //Exercise
        m_processor.processConnect(m_session, connMsg);

        //Verify
        assertEquals(ConnAckMessage.CONNECTION_ACCEPTED, m_session.getReturnCode());
    }

    @Test
    public void testWill() {
        connMsg.setClientID("123");
        connMsg.setWillFlag(true);
        connMsg.setWillTopic("topic");
        connMsg.setWillMessage("Topic message".getBytes());


        //Exercise
        //m_handler.setMessaging(mockedMessaging);
        m_processor.processConnect(m_session, connMsg);

        //Verify
        assertEquals(ConnAckMessage.CONNECTION_ACCEPTED, m_session.getReturnCode());
        //TODO verify the call
        /*verify(mockedMessaging).publish(eq("topic"), eq("Topic message".getBytes()),
                any(AbstractMessage.QOSType.class), anyBoolean(), eq("123"), any(IoSession.class));*/
    }

    @Test
    public void validAuthentication() {
        connMsg.setClientID("123");
        connMsg.setUserFlag(true);
        connMsg.setPasswordFlag(true);
        connMsg.setUsername(TEST_USER);
        connMsg.setPassword(TEST_PWD);

        //Exercise
        m_processor.processConnect(m_session, connMsg);

        //Verify
        assertEquals(ConnAckMessage.CONNECTION_ACCEPTED, m_session.getReturnCode());
    }

    @Test
    public void noPasswdAuthentication() {
        connMsg.setClientID("123");
        connMsg.setUserFlag(true);
        connMsg.setPasswordFlag(false);
        connMsg.setUsername(TEST_USER);

        //Exercise
        m_processor.processConnect(m_session, connMsg);

        //Verify
        assertEquals(ConnAckMessage.BAD_USERNAME_OR_PASSWORD, m_session.getReturnCode());
    }

    @Test
    public void invalidAuthentication() {
        connMsg.setClientID("123");
        connMsg.setUserFlag(true);
        connMsg.setPasswordFlag(true);
        connMsg.setUsername(TEST_USER + "_fake");
        connMsg.setPassword(TEST_PWD);

        //Exercise
        m_processor.processConnect(m_session, connMsg);

        //Verify
        assertEquals(ConnAckMessage.BAD_USERNAME_OR_PASSWORD, m_session.getReturnCode());
    }

    @Test
    public void prohibitAnonymousClient() {
        connMsg.setClientID("123");
        m_processor.init(subscriptions, m_storageService, m_sessionStore, m_mockAuthenticator, false, new PermitAllAuthorizator(), NO_OBSERVERS_INTERCEPTOR);

        //Exercise
        m_processor.processConnect(m_session, connMsg);

        //Verify
        assertEquals(ConnAckMessage.BAD_USERNAME_OR_PASSWORD, m_session.getReturnCode());
    }

    @Test
    public void prohibitAnonymousClient_providingUsername() {
        connMsg.setClientID("123");
        connMsg.setUserFlag(true);
        connMsg.setUsername(TEST_USER + "_fake");
        m_processor.init(subscriptions, m_storageService, m_sessionStore, m_mockAuthenticator, false, new PermitAllAuthorizator(), NO_OBSERVERS_INTERCEPTOR);

        //Exercise
        m_processor.processConnect(m_session, connMsg);

        //Verify
        assertEquals(ConnAckMessage.BAD_USERNAME_OR_PASSWORD, m_session.getReturnCode());
    }

    @Test
    public void acceptAnonymousClient() {
        connMsg.setClientID("123");
        m_processor.init(subscriptions, m_storageService, m_sessionStore, m_mockAuthenticator, true, new PermitAllAuthorizator(), NO_OBSERVERS_INTERCEPTOR);

        //Exercise
        m_processor.processConnect(m_session, connMsg);

        //Verify
        assertEquals(ConnAckMessage.CONNECTION_ACCEPTED, m_session.getReturnCode());
    }

    @Test
    public void connectWithSameClientIDBadCredentialsDoesntDropExistingClient() {
        //Connect a client1
        connMsg.setClientID("Client1");
        connMsg.setUserFlag(true);
        connMsg.setPasswordFlag(true);
        connMsg.setUsername(TEST_USER);
        connMsg.setPassword(TEST_PWD);
        m_processor.processConnect(m_session, connMsg);
        assertEquals(ConnAckMessage.CONNECTION_ACCEPTED, m_session.getReturnCode());

        //create another connect same clientID but with bad credentials
        ConnectMessage evilClientConnMsg = new ConnectMessage();
        evilClientConnMsg.setProtocolVersion((byte) 0x03);
        evilClientConnMsg.setClientID("Client1");
        evilClientConnMsg.setUserFlag(true);
        evilClientConnMsg.setPasswordFlag(true);
        evilClientConnMsg.setUsername(EVIL_TEST_USER);
        evilClientConnMsg.setPassword(EVIL_TEST_PWD);
        DummyChannel evilSession = new DummyChannel();

        //Exercise
        m_processor.processConnect(evilSession, evilClientConnMsg);

        //Verify
        //the evil client gets a not auth notification
        assertEquals(ConnAckMessage.BAD_USERNAME_OR_PASSWORD, evilSession.getReturnCode());
        //the good client remains connected
        assertFalse(m_session.isClosed());
        assertTrue(evilSession.isClosed());
    }


    @Test
    public void testConnAckContainsSessionPresentFlag() throws InterruptedException {
        connMsg = new ConnectMessage();
        connMsg.setProtocolVersion(VERSION_3_1_1);
        connMsg.setClientID("CliID");
        connMsg.setCleanSession(false);
        m_session.setAttribute(NettyChannel.ATTR_KEY_CLIENTID, "CliID");
        m_session.setAttribute(NettyChannel.ATTR_KEY_CLEANSESSION, false);

        //Connect a first time
        m_processor.processConnect(m_session, connMsg);
        //disconnect
        m_processor.processDisconnect(m_session, new DisconnectMessage());

        //Exercise, reconnect
        MockReceiverChannel firstReceiverSession = new MockReceiverChannel();
        m_processor.processConnect(firstReceiverSession, connMsg);

        //Verify
        AbstractMessage recvMsg = firstReceiverSession.getMessage();
        assertTrue(recvMsg instanceof ConnAckMessage);
        ConnAckMessage connAckMsg = (ConnAckMessage) recvMsg;
        assertTrue(connAckMsg.isSessionPresent());
        assertEquals(ConnAckMessage.CONNECTION_ACCEPTED, connAckMsg.getReturnCode());
    }

}
