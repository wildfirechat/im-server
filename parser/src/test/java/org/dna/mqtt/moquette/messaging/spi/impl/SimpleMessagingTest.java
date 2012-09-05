package org.dna.mqtt.moquette.messaging.spi.impl;

import org.apache.mina.core.filterchain.IoFilterAdapter;
import org.apache.mina.core.session.DummySession;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteRequest;
import org.dna.mqtt.moquette.messaging.spi.impl.events.*;
import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.Subscription;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import static org.junit.Assert.*;

import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.dna.mqtt.moquette.proto.messages.SubscribeMessage;
import org.dna.mqtt.moquette.server.ConnectionDescriptor;
import org.dna.mqtt.moquette.server.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author andrea
 */
public class SimpleMessagingTest {

    final static String FAKE_CLIENT_ID = "FAKE_123";
    final static String FAKE_TOPIC = "/news";
    SimpleMessaging messaging;

    byte m_returnCode;
    IoSession m_session;
//    MQTTHandler m_handler;
    ConnectMessage connMsg;

    AbstractMessage m_receivedMessage;

    @Before
    public void setUp() throws InterruptedException {
        messaging = SimpleMessaging.getInstance();
        messaging.init();
        connMsg = new ConnectMessage();
        connMsg.setProcotolVersion((byte) 0x03);

        m_session = new DummySession();

        m_session.getFilterChain().addFirst("MessageCatcher", new IoFilterAdapter() {

            @Override
            public void filterWrite(NextFilter nextFilter, IoSession session,
                                    WriteRequest writeRequest) throws Exception {
                try {
                    m_receivedMessage = (AbstractMessage) writeRequest.getMessage();
                    if (m_receivedMessage instanceof ConnAckMessage) {
                        ConnAckMessage buf = (ConnAckMessage) m_receivedMessage;
                        m_returnCode = buf.getReturnCode();
                    }
                } catch (Exception ex) {
                    throw new AssertionError("Wrong return code");
                }
            }
        });

        //sleep to let the messaging batch processor to process the initEvent
        Thread.sleep(300);
    }

    @After
    public void tearDown() {
        m_receivedMessage = null;
        messaging.stop();
    }

    @Test
    public void testSubscribe() {
        //Exercise
        SubscribeMessage msg = new SubscribeMessage();
        msg.addSubscription(new SubscribeMessage.Couple((byte)QOSType.MOST_ONE.ordinal(), FAKE_TOPIC));
        messaging.processSubscribe(m_session, msg, FAKE_CLIENT_ID, false);

        //Verify
        Subscription expectedSubscription = new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE, false);
        assertTrue(messaging.getSubscriptions().contains(expectedSubscription));
    }

    @Test
    public void testDoubleSubscribe() {
//        SubscribeEvent evt = new SubscribeEvent(new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE, false), 0);
        SubscribeMessage msg = new SubscribeMessage();
        msg.addSubscription(new SubscribeMessage.Couple((byte)QOSType.MOST_ONE.ordinal(), FAKE_TOPIC));

        messaging.processSubscribe(m_session, msg, FAKE_CLIENT_ID, false);

        //Exercise
        messaging.processSubscribe(m_session, msg, FAKE_CLIENT_ID, false);

        //Verify
        Subscription subscription = new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE, false);
        assertEquals(1, messaging.getSubscriptions().size());
    }

    @Test
    public void testPublish() throws InterruptedException {
        //simulate a connect that register a clientID to an IoSession
        ConnectionDescriptor connDescr = new ConnectionDescriptor(FAKE_CLIENT_ID, m_session, true);
        messaging.m_clientIDs.put(FAKE_CLIENT_ID, connDescr);

//        SubscribeEvent evt = new SubscribeEvent(new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE, false), 0);
        SubscribeMessage msg = new SubscribeMessage();
        msg.addSubscription(new SubscribeMessage.Couple((byte)QOSType.MOST_ONE.ordinal(), FAKE_TOPIC));

        messaging.processSubscribe(m_session, msg, FAKE_CLIENT_ID, false);

        //Exercise
        PublishEvent pubEvt = new PublishEvent(FAKE_TOPIC, QOSType.MOST_ONE, "Hello".getBytes(), false, "FakeCLI", null);
        messaging.processPublish(pubEvt);

        //Verify
        assertNotNull(m_receivedMessage);
        //TODO check received message attributes
    }

    @Test
    public void testHandleConnect_BadProtocol() {
        connMsg.setProcotolVersion((byte) 0x02);

        //Exercise
        messaging.processConnect(m_session, connMsg);

        //Verify
        assertEquals(ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION, m_returnCode);
    }

    @Test
    public void testConnect_badClientID() {
        connMsg.setClientID("extremely_long_clientID_greater_than_23");

        //Exercise
        messaging.processConnect(m_session, connMsg);

        //Verify
        assertEquals(ConnAckMessage.IDENTIFIER_REJECTED, m_returnCode);
    }

    @Test
    public void testWill() {
        connMsg.setClientID("123");
        connMsg.setWillFlag(true);
        connMsg.setWillTopic("topic");
        connMsg.setWillMessage("Topic message");
        //IMessaging mockedMessaging = mock(IMessaging.class);

        //Exercise
        //m_handler.setMessaging(mockedMessaging);
        messaging.processConnect(m_session, connMsg);

        //Verify
        assertEquals(ConnAckMessage.CONNECTION_ACCEPTED, m_returnCode);
        //TODO verify the call
        /*verify(mockedMessaging).publish(eq("topic"), eq("Topic message".getBytes()),
                any(AbstractMessage.QOSType.class), anyBoolean(), eq("123"), any(IoSession.class));*/
    }


    @Test
    public void testHandleSubscribe() {
        String topicName = "/news";
        SubscribeMessage subscribeMsg = new SubscribeMessage();
        subscribeMsg.setMessageID(0x555);
        subscribeMsg.addSubscription(new SubscribeMessage.Couple(
                (byte)AbstractMessage.QOSType.EXACTLY_ONCE.ordinal(), topicName));
        //IMessaging mockedMessaging = mock(IMessaging.class);

        m_session.setAttribute(Constants.ATTR_CLIENTID, "fakeID");
        m_session.setAttribute(Constants.CLEAN_SESSION, false);
        Subscription subscription = new Subscription("fakeID", "/news", QOSType.EXACTLY_ONCE, true);

        //Exercise
        //m_handler.setMessaging(mockedMessaging);
//        SubscribeEvent pubEvt = new SubscribeEvent(subscription, 0);
        SubscribeMessage msg = new SubscribeMessage();
        msg.addSubscription(new SubscribeMessage.Couple((byte)QOSType.EXACTLY_ONCE.ordinal(), "/news"));
        messaging.processSubscribe(m_session, msg, "fakeID", false);

        //Verify
        /*ArgumentCaptor<AbstractMessage.QOSType> argument = ArgumentCaptor.forClass(AbstractMessage.QOSType.class);
        verify(mockedMessaging).subscribe(eq("fakeID"), eq(topicName), argument.capture(), eq(false));
        assertEquals(AbstractMessage.QOSType.EXACTLY_ONCE, argument.getValue());*/
        //TODO verify the message ACK and corresponding QoS!!
    }
}
