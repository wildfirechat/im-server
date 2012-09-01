package org.dna.mqtt.moquette.messaging.spi.impl;

import java.util.concurrent.BlockingQueue;

import org.dna.mqtt.moquette.messaging.spi.INotifier;
import org.dna.mqtt.moquette.messaging.spi.impl.events.MessagingEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.events.NotifyEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.events.PublishEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.events.SubscribeEvent;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
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
        SubscribeEvent evt = new SubscribeEvent(new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE, false));
        messaging.processSubscribe(evt);

        //Verify
        Subscription expectedSubscription = new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE, false);
        assertTrue(messaging.getSubscriptions().contains(expectedSubscription));
    }

    @Test
    public void testDoubleSubscribe() {
        SubscribeEvent evt = new SubscribeEvent(new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE, false));
        messaging.processSubscribe(evt);

        //Exercise
        messaging.processSubscribe(evt);

        //Verify
        Subscription subscription = new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE, false);
        assertEquals(1, messaging.getSubscriptions().size());
    }

    @Test
    public void testPublish() throws InterruptedException {
        SubscribeEvent evt = new SubscribeEvent(new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE, false));
        messaging.processSubscribe(evt);
        INotifier notifier = mock(INotifier.class);
        messaging.setNotifier(notifier);

        //Exercise
        PublishEvent pubEvt = new PublishEvent(FAKE_TOPIC, QOSType.MOST_ONE, "Hello".getBytes(), false, "FakeCLI", null);
        messaging.processPublish(pubEvt);

        //Verify
        ArgumentCaptor<NotifyEvent> argument = ArgumentCaptor.forClass(NotifyEvent.class);
        verify(notifier).notify(argument.capture());
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
