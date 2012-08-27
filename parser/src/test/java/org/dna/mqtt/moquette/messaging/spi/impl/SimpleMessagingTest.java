package org.dna.mqtt.moquette.messaging.spi.impl;

import org.dna.mqtt.moquette.messaging.spi.INotifier;
import org.dna.mqtt.moquette.messaging.spi.impl.events.PublishEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.events.SubscribeEvent;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 *
 * @author andrea
 */
public class SimpleMessagingTest {

    final static String FAKE_CLIENT_ID = "FAKE_123";
    final static String FAKE_TOPIC = "/news";
    SimpleMessaging messaging;

    @Before
    public void setUp() {
        messaging = new SimpleMessaging();
    }

    @Test
    public void testSubscribe() {
        //Exercise
        SubscribeEvent evt = new SubscribeEvent(new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE));
        messaging.processSubscribe(evt);

        //Verify
        Subscription expectedSubscription = new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE);
        assertTrue(messaging.getSubscriptions().contains(expectedSubscription));
    }

    @Test
    public void testDoubleSubscribe() {
        SubscribeEvent evt = new SubscribeEvent(new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE));
        messaging.processSubscribe(evt);

        //Exercise
        messaging.processSubscribe(evt);

        //Verify
        Subscription subscription = new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE);
        assertEquals(1, messaging.getSubscriptions().size());
    }

    @Test
    public void testPublish() {
        SubscribeEvent evt = new SubscribeEvent(new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE));
        messaging.processSubscribe(evt);
        INotifier notifier = mock(INotifier.class);
        messaging.setNotifier(notifier);

        //Exercise
        PublishEvent pubEvt = new PublishEvent(FAKE_TOPIC, QOSType.MOST_ONE, "Hello".getBytes(), false);
        messaging.processPublish(pubEvt);

        //Verify
        ArgumentCaptor<byte[]> argument = ArgumentCaptor.forClass(byte[].class);
        verify(notifier).notify(eq(FAKE_CLIENT_ID), eq(FAKE_TOPIC), any(QOSType.class), argument.capture(), eq(false));
        assertEquals("Hello", new String(argument.getValue()));
    }
    
    @Test
    public void testMatchTopics_simple() {
        assertTrue(messaging.matchTopics("/", "/"));
        assertTrue(messaging.matchTopics("/finance", "/finance"));
    }
    
    @Test
    public void testMatchTopics_multi() {
        assertTrue(messaging.matchTopics("finance", "#"));
        assertTrue(messaging.matchTopics("finance", "finance/#"));
        assertTrue(messaging.matchTopics("finance/stock", "finance/#"));
        assertTrue(messaging.matchTopics("finance/stock/ibm", "finance/#"));
    }
    
    
    @Test
    public void testMatchTopics_single() {
        assertTrue(messaging.matchTopics("finance", "+"));
        assertTrue(messaging.matchTopics("finance/stock", "finance/+"));
        assertTrue(messaging.matchTopics("/finance", "/+"));
        assertFalse(messaging.matchTopics("/finance", "+"));
        assertTrue(messaging.matchTopics("/finance", "+/+"));
        assertTrue(messaging.matchTopics("/finance/stock/ibm", "/finance/+/ibm"));
        assertFalse(messaging.matchTopics("/finance/stock", "+"));
    }
}
