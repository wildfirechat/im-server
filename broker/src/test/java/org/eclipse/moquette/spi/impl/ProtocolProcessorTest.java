/*
 * Copyright (c) 2012-2014 The original author or authors
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

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.eclipse.moquette.spi.IMatchingCondition;
import org.eclipse.moquette.spi.IMessagesStore;
import org.eclipse.moquette.spi.ISessionsStore;
import org.eclipse.moquette.spi.impl.events.LostConnectionEvent;
import org.eclipse.moquette.spi.impl.subscriptions.Subscription;
import org.eclipse.moquette.spi.impl.subscriptions.SubscriptionsStore;
import static org.eclipse.moquette.parser.netty.Utils.VERSION_3_1_1;
import org.eclipse.moquette.proto.messages.AbstractMessage;
import org.eclipse.moquette.proto.messages.AbstractMessage.QOSType;
import org.eclipse.moquette.proto.messages.ConnAckMessage;
import org.eclipse.moquette.proto.messages.ConnectMessage;
import org.eclipse.moquette.proto.messages.DisconnectMessage;
import org.eclipse.moquette.proto.messages.PublishMessage;
import org.eclipse.moquette.proto.messages.SubAckMessage;
import org.eclipse.moquette.proto.messages.SubscribeMessage;
import org.eclipse.moquette.server.Constants;
import org.eclipse.moquette.server.ServerChannel;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
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
    
    final static String TEST_USER = "fakeuser";
    final static String TEST_PWD = "fakepwd";
    
    ServerChannel m_session;
    byte m_returnCode;
    ConnectMessage connMsg;
    ProtocolProcessor m_processor;
    
    IMessagesStore m_storageService;
    ISessionsStore m_sessionStore;
    SubscriptionsStore subscriptions;
    AbstractMessage m_receivedMessage;
    MockAuthenticator m_mockAuthenticator;
    
    class DummyChannel implements ServerChannel {
        
        private Map<Object, Object> m_attributes = new HashMap<Object, Object>();

        public Object getAttribute(Object key) {
            return m_attributes.get(key);
        }

        public void setAttribute(Object key, Object value) {
            m_attributes.put(key, value);
        }

        public void setIdleTime(int idleTime) {
        }

        public void close(boolean immediately) {
            
        }

        public void write(Object value) {
            try {
                m_receivedMessage = (AbstractMessage) value;
                if (m_receivedMessage instanceof ConnAckMessage) {
                    ConnAckMessage buf = (ConnAckMessage) m_receivedMessage;
                    m_returnCode = buf.getReturnCode();
                }
            } catch (Exception ex) {
                throw new AssertionError("Wrong return code");
            }
        }
    } 
    
    /**
     * This a synchronous channel that avoid output ring buffer from Processor
     */
    class MockReceiverChannel implements ServerChannel {
//        byte m_returnCode;
        AbstractMessage m_receivedMessage;
        private Map<Object, Object> m_attributes = new HashMap<Object, Object>();

        public Object getAttribute(Object key) {
            return m_attributes.get(key);
        }

        public void setAttribute(Object key, Object value) {
            m_attributes.put(key, value);
        }

        public void setIdleTime(int idleTime) {
        }

        public void close(boolean immediately) {
        }
        
        public AbstractMessage getMessage() {
            return this.m_receivedMessage;
        }
        
//        public byte getReturnCode() {
//            return this.m_returnCode;
//        }

        public void write(Object value) {
            try {
                this.m_receivedMessage = (AbstractMessage) value;
//                if (this.m_receivedMessage instanceof PublishMessage) {
//                    T buf = (T) this.m_receivedMessage;
//                }
            } catch (Exception ex) {
                throw new AssertionError("Wrong return code");
            }
        }
    } 
    
    @Before
    public void setUp() throws InterruptedException {
        connMsg = new ConnectMessage();
        connMsg.setProcotolVersion((byte) 0x03);

        m_session = new DummyChannel();

        //sleep to let the messaging batch processor to process the initEvent
        Thread.sleep(300);
        MemoryStorageService memStorage = new MemoryStorageService();
        m_storageService = memStorage;
        m_sessionStore = memStorage;
        //m_storageService.initStore();
        
        Map<String, String> users = new HashMap<String, String>();
        users.put(TEST_USER, TEST_PWD);
        m_mockAuthenticator = new MockAuthenticator(users);

        subscriptions = new SubscriptionsStore();
        subscriptions.init(new MemoryStorageService());
        m_processor = new ProtocolProcessor();
        m_processor.init(subscriptions, m_storageService, m_sessionStore, m_mockAuthenticator);
    }
    
    @Test
    public void testHandleConnect_BadProtocol() {
        connMsg.setProcotolVersion((byte) 0x02);

        //Exercise
        m_processor.processConnect(m_session, connMsg);

        //Verify
        assertEquals(ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION, m_returnCode);
    }
    
    @Test
    public void testConnect_badClientID() {
        connMsg.setClientID("extremely_long_clientID_greater_than_23");

        //Exercise
        m_processor.processConnect(m_session, connMsg);

        //Verify
        assertEquals(ConnAckMessage.CONNECTION_ACCEPTED, m_returnCode);
    }

    @Test
    public void testWill() {
        connMsg.setClientID("123");
        connMsg.setWillFlag(true);
        connMsg.setWillTopic("topic");
        connMsg.setWillMessage("Topic message");
        

        //Exercise
        //m_handler.setMessaging(mockedMessaging);
        m_processor.processConnect(m_session, connMsg);

        //Verify
        assertEquals(ConnAckMessage.CONNECTION_ACCEPTED, m_returnCode);
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
        assertEquals(ConnAckMessage.CONNECTION_ACCEPTED, m_returnCode);
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
        assertEquals(ConnAckMessage.BAD_USERNAME_OR_PASSWORD, m_returnCode);
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
        assertEquals(ConnAckMessage.BAD_USERNAME_OR_PASSWORD, m_returnCode);
    }
    
    
    @Test
    public void testConnAckContainsSessionPresentFlag() throws InterruptedException {
        connMsg = new ConnectMessage();
        connMsg.setProcotolVersion(VERSION_3_1_1);
        connMsg.setClientID("CliID");
        connMsg.setCleanSession(false);
        m_session.setAttribute(Constants.ATTR_CLIENTID, "CliID");
        m_session.setAttribute(Constants.CLEAN_SESSION, false);

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

    @Test
    public void testPublish() throws InterruptedException {
        final Subscription subscription = new Subscription(FAKE_CLIENT_ID, 
                FAKE_TOPIC, AbstractMessage.QOSType.MOST_ONE, true);

        //subscriptions.matches(topic) redefine the method to return true
        SubscriptionsStore subs = new SubscriptionsStore() {
            @Override
            public List<Subscription> matches(String topic) {
                if (topic.equals(FAKE_TOPIC)) {
                    return Arrays.asList(subscription);
                } else {
                    throw new IllegalArgumentException("Expected " + FAKE_TOPIC + " buf found " + topic);
                }
            }
        };
        
        //simulate a connect that register a clientID to an IoSession
        subs.init(new MemoryStorageService());
        m_processor.init(subs, m_storageService, m_sessionStore, null);
        ConnectMessage connectMessage = new ConnectMessage();
        connectMessage.setProcotolVersion((byte)3);
        connectMessage.setClientID(FAKE_CLIENT_ID);
        connectMessage.setCleanSession(subscription.isCleanSession());
        m_processor.processConnect(m_session, connectMessage);
        
        
        //Exercise
        ByteBuffer buffer = ByteBuffer.allocate(5).put("Hello".getBytes());
        PublishMessage msg = new PublishMessage();
        msg.setTopicName(FAKE_TOPIC);
        msg.setQos(QOSType.MOST_ONE);
        msg.setPayload(buffer);
        msg.setRetainFlag(false);
        m_session.setAttribute(Constants.ATTR_CLIENTID, "FakeCLI");
        m_processor.processPublish(m_session, msg);

        //Verify
        assertNotNull(m_receivedMessage);
        //TODO check received message attributes
    }
    
    @Test
    public void testPublishToMultipleSubscribers() throws InterruptedException {
        final Subscription subscription = new Subscription(FAKE_CLIENT_ID, 
                FAKE_TOPIC, AbstractMessage.QOSType.MOST_ONE, true);
        final Subscription subscriptionClient2 = new Subscription(FAKE_CLIENT_ID2, 
                FAKE_TOPIC, AbstractMessage.QOSType.MOST_ONE, true);

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
        subs.init(new MemoryStorageService());
        m_processor.init(subs, m_storageService, m_sessionStore, null);
        
        MockReceiverChannel firstReceiverSession = new MockReceiverChannel();
        ConnectMessage connectMessage = new ConnectMessage();
        connectMessage.setProcotolVersion((byte)3);
        connectMessage.setClientID(FAKE_CLIENT_ID);
        connectMessage.setCleanSession(subscription.isCleanSession());
        m_processor.processConnect(firstReceiverSession, connectMessage);
        
        //connect the second fake subscriber
        MockReceiverChannel secondReceiverSession = new MockReceiverChannel();
        ConnectMessage connectMessage2 = new ConnectMessage();
        connectMessage2.setProcotolVersion((byte)3);
        connectMessage2.setClientID(FAKE_CLIENT_ID2);
        connectMessage2.setCleanSession(subscription.isCleanSession());
        m_processor.processConnect(secondReceiverSession, connectMessage2);
        
        //Exercise
        ByteBuffer buffer = ByteBuffer.allocate(5).put("Hello".getBytes());
        buffer.rewind();
        PublishMessage msg = new PublishMessage();
        msg.setTopicName(FAKE_TOPIC);
        msg.setQos(QOSType.MOST_ONE);
        msg.setPayload(buffer);
        msg.setRetainFlag(false);
        m_session.setAttribute(Constants.ATTR_CLIENTID, "FakeCLI");
        m_processor.processPublish(m_session, msg);

        //Verify
        Thread.sleep(100); //ugly but we depend on the asynch that pull data from back disruptor
        PublishMessage pub2FirstSubscriber = (PublishMessage) firstReceiverSession.getMessage();
        assertNotNull(pub2FirstSubscriber);
        String firstMessageContent = DebugUtils.payload2Str(pub2FirstSubscriber.getPayload());
        assertEquals("Hello", firstMessageContent);
        
        PublishMessage pub2SecondSubscriber = (PublishMessage) secondReceiverSession.getMessage();
        assertNotNull(pub2SecondSubscriber);
        String secondMessageContent = DebugUtils.payload2Str(pub2SecondSubscriber.getPayload());
        assertEquals("Hello", secondMessageContent);
    }
    
    @Test
    public void testSubscribe() {
        //Exercise
        SubscribeMessage msg = new SubscribeMessage();
        msg.addSubscription(new SubscribeMessage.Couple((byte)AbstractMessage.QOSType.MOST_ONE.ordinal(), FAKE_TOPIC));
        m_session.setAttribute(Constants.ATTR_CLIENTID, FAKE_CLIENT_ID);
        m_session.setAttribute(Constants.CLEAN_SESSION, false);
        m_processor.processSubscribe(m_session, msg/*, FAKE_CLIENT_ID, false*/);

        //Verify
        assertTrue(m_receivedMessage instanceof SubAckMessage);
        Subscription expectedSubscription = new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, AbstractMessage.QOSType.MOST_ONE, false);
        assertTrue(subscriptions.contains(expectedSubscription));
    }
    
    @Test
    public void testDoubleSubscribe() {
        SubscribeMessage msg = new SubscribeMessage();
        msg.addSubscription(new SubscribeMessage.Couple((byte)AbstractMessage.QOSType.MOST_ONE.ordinal(), FAKE_TOPIC));
        m_session.setAttribute(Constants.ATTR_CLIENTID, FAKE_CLIENT_ID);
        m_session.setAttribute(Constants.CLEAN_SESSION, false);
        subscriptions.clearAllSubscriptions();
        assertEquals(0, subscriptions.size());
        
        m_processor.processSubscribe(m_session, msg/*, FAKE_CLIENT_ID, false*/);
                
        //Exercise
        m_processor.processSubscribe(m_session, msg/*, FAKE_CLIENT_ID, false*/);

        //Verify
        assertEquals(1, subscriptions.size());
        Subscription expectedSubscription = new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, AbstractMessage.QOSType.MOST_ONE, false);
        assertTrue(subscriptions.contains(expectedSubscription));
    }

    
    @Test
    public void testPublishOfRetainedMessage_afterNewSubscription() throws Exception {
        final CountDownLatch publishRecvSignal = new CountDownLatch(1);
        /*ServerChannel */m_session = new DummyChannel() {
            @Override
            public void write(Object value) {
                try {
                    System.out.println("filterReceived class " + value.getClass().getName());
                    if (value instanceof PublishMessage) {
                        m_receivedMessage = (AbstractMessage) value;
                        publishRecvSignal.countDown();
                    }
                    
                    if (m_receivedMessage instanceof ConnAckMessage) {
                        ConnAckMessage buf = (ConnAckMessage) m_receivedMessage;
                        m_returnCode = buf.getReturnCode();
                    }
                } catch (Exception ex) {
                    throw new AssertionError("Wrong return code");
                }
            }   
        };
        
        //simulate a connect that register a clientID to an IoSession
        final Subscription subscription = new Subscription(FAKE_PUBLISHER_ID, 
                FAKE_TOPIC, AbstractMessage.QOSType.MOST_ONE, true);

        //subscriptions.matches(topic) redefine the method to return true
        SubscriptionsStore subs = new SubscriptionsStore() {
            @Override
            public List<Subscription> matches(String topic) {
                if (topic.equals(FAKE_TOPIC)) {
                    return Arrays.asList(subscription);
                } else {
                    throw new IllegalArgumentException("Expected " + FAKE_TOPIC + " buf found " + topic);
                }
            }
        };
        subs.init(new MemoryStorageService());
        
        //simulate a connect that register a clientID to an IoSession
        m_processor.init(subs, m_storageService, m_sessionStore, null);
        ConnectMessage connectMessage = new ConnectMessage();
        connectMessage.setClientID(FAKE_PUBLISHER_ID);
        connectMessage.setProcotolVersion((byte)3);
        connectMessage.setCleanSession(subscription.isCleanSession());
        m_processor.processConnect(m_session, connectMessage);
        ByteBuffer buffer = ByteBuffer.allocate(5).put("Hello".getBytes());
        PublishMessage pubmsg = new PublishMessage();
        pubmsg.setTopicName(FAKE_TOPIC);
        pubmsg.setQos(QOSType.MOST_ONE);
        pubmsg.setPayload(buffer);
        pubmsg.setRetainFlag(true);
        m_session.setAttribute(Constants.ATTR_CLIENTID, FAKE_PUBLISHER_ID);
        m_processor.processPublish(m_session, pubmsg);
        m_session.setAttribute(Constants.CLEAN_SESSION, false);
        
        //Exercise
        SubscribeMessage msg = new SubscribeMessage();
        msg.addSubscription(new SubscribeMessage.Couple((byte)QOSType.MOST_ONE.ordinal(), "#"));
        m_processor.processSubscribe(m_session, msg/*, FAKE_PUBLISHER_ID, false*/);
        
        //Verify
        //wait the latch
        assertTrue(publishRecvSignal.await(1, TimeUnit.SECONDS)); //no timeout
        assertNotNull(m_receivedMessage); 
        assertTrue(m_receivedMessage instanceof PublishMessage);
        PublishMessage pubMessage = (PublishMessage) m_receivedMessage;
        assertEquals(FAKE_TOPIC, pubMessage.getTopicName());
    }
    
    @Test
    public void publishNoPublishToInactiveSubscriptions() {
        SubscriptionsStore mockedSubscriptions = mock(SubscriptionsStore.class);
        Subscription inactiveSub = new Subscription("Subscriber", "/topic", QOSType.LEAST_ONE, false); 
        inactiveSub.setActive(false);
        List<Subscription> inactiveSubscriptions = Arrays.asList(inactiveSub);
        when(mockedSubscriptions.matches(eq("/topic"))).thenReturn(inactiveSubscriptions);
        m_processor = new ProtocolProcessor();
        m_processor.init(mockedSubscriptions, m_storageService, m_sessionStore, null);
        
        //Exercise
        ByteBuffer buffer = ByteBuffer.allocate(5).put("Hello".getBytes());
        PublishMessage msg = new PublishMessage();
        msg.setTopicName("/topic");
        msg.setQos(QOSType.MOST_ONE);
        msg.setPayload(buffer);
        msg.setRetainFlag(true);
        m_session.setAttribute(Constants.ATTR_CLIENTID, "Publisher");
        m_processor.processPublish(m_session, msg);

        //Verify no message is received
        assertNull(m_receivedMessage); 
    }
    
    
    @Test
    public void publishToAnInactiveSubscriptionsCleanSession() {
        SubscriptionsStore mockedSubscriptions = mock(SubscriptionsStore.class);
        Subscription inactiveSub = new Subscription("Subscriber", "/topic", QOSType.LEAST_ONE, true); 
        inactiveSub.setActive(false);
        List<Subscription> inactiveSubscriptions = Arrays.asList(inactiveSub);
        when(mockedSubscriptions.matches(eq("/topic"))).thenReturn(inactiveSubscriptions);
        m_processor = new ProtocolProcessor();
        m_processor.init(mockedSubscriptions, m_storageService, m_sessionStore, null);
        
        //Exercise
        ByteBuffer buffer = ByteBuffer.allocate(5).put("Hello".getBytes());
        PublishMessage msg = new PublishMessage();
        msg.setTopicName("/topic");
        msg.setQos(QOSType.MOST_ONE);
        msg.setPayload(buffer);
        msg.setRetainFlag(true);
        m_session.setAttribute(Constants.ATTR_CLIENTID, "Publisher");
        m_processor.processPublish(m_session, msg);

        //Verify no message is received
        assertNull(m_receivedMessage); 
    }
    
    
    /**
     * Verify that receiving a publish with retained message and with Q0S = 0 
     * clean the existing retained messages for that topic.
     */
    @Test
    public void testCleanRetainedStoreAfterAQoS0AndRetainedTrue() {
        //force a connect
        connMsg.setClientID("Publisher");
        m_processor.processConnect(m_session, connMsg);
        //prepare and existing retained store
        m_session.setAttribute(Constants.ATTR_CLIENTID, "Publisher");
        ByteBuffer payload = ByteBuffer.allocate(5).put("Hello".getBytes());
        PublishMessage msg = new PublishMessage();
        msg.setTopicName(FAKE_TOPIC);
        msg.setQos(QOSType.LEAST_ONE);
        msg.setPayload(payload);
        msg.setRetainFlag(true);
        msg.setMessageID(100);
        m_processor.processPublish(m_session, msg);
        
        //Exercise
        PublishMessage cleanPubMsg = new PublishMessage();
        cleanPubMsg.setTopicName(FAKE_TOPIC);
        cleanPubMsg.setPayload(payload);
        cleanPubMsg.setQos(QOSType.MOST_ONE);
        cleanPubMsg.setRetainFlag(true);
        m_processor.processPublish(m_session, cleanPubMsg);
        
        //Verify
        Collection<IMessagesStore.StoredMessage> messages = m_storageService.searchMatching(new IMatchingCondition() {
            public boolean match(String key) {
                return  SubscriptionsStore.matchTopics(key, FAKE_TOPIC);
            }
        });
        assertTrue(messages.isEmpty());
    }


    /**
     * Simulate a client1 (FAKE_CLIENT_ID) bound with session1, an event of connection lost
     * for client2 (FAKE_CLIENT_ID, same of the client1) but from another session, verify
     * the session of client1 is not closed.
     * */
    @Test
    public void testConnectionLostClosesTheCorrectSession() {
        MockReceiverChannel channel1 = new MockReceiverChannel();

        //init the processor
        /*SubscriptionsStore subs = new SubscriptionsStore();
        subs.init(new MemoryStorageService());*/
        SubscriptionsStore subs = mock(SubscriptionsStore.class);
        m_processor.init(subs, m_storageService, m_sessionStore, null);

        //simulate a connect from client1 FAKE_CLIENT_ID from channel1
        ConnectMessage connectMessage = new ConnectMessage();
        connectMessage.setProcotolVersion((byte) 3);
        connectMessage.setClientID(FAKE_CLIENT_ID);
        m_processor.processConnect(channel1, connectMessage);
        assertConnectReturnCode(ConnAckMessage.CONNECTION_ACCEPTED, channel1);
        //ConnAck received

        //send a connection lost event from an already disconnected client, but with same clientID (FAKE_CLIENT_ID)
        //Exercise
        DummyChannel channel2 = new DummyChannel();
        LostConnectionEvent lostConnectionEvent = new LostConnectionEvent(channel2, FAKE_CLIENT_ID);
        m_processor.processConnectionLost(lostConnectionEvent);

        //Verify no subscriptions were deactivated for the client on session1
        verify(subs, never()).deactivate(anyString());
    }

    private void assertConnectReturnCode(byte expectedReturnCode, MockReceiverChannel receiverSession) {
        AbstractMessage recvMsg = receiverSession.getMessage();
        assertTrue(recvMsg instanceof ConnAckMessage);
        ConnAckMessage connAckMsg = (ConnAckMessage) recvMsg;
        assertEquals(expectedReturnCode, connAckMsg.getReturnCode());
    }
}
