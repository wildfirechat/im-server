package org.dna.mqtt.moquette.messaging.spi.impl;

import org.dna.mqtt.moquette.messaging.spi.INotifier;
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
        messaging.subscribe(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE);

        //Verify
        Subscription expectedSubscription = new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE);
        assertTrue(messaging.getSubscriptions().contains(expectedSubscription));
    }

    @Test
    public void testDoubleSubscribe() {
        messaging.subscribe(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE);

        //Exercise
        messaging.subscribe(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE);

        //Verify
        Subscription subscription = new Subscription(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE);
        assertEquals(1, messaging.getSubscriptions().size());
    }

    @Test
    public void testPublish() {
        messaging.subscribe(FAKE_CLIENT_ID, FAKE_TOPIC, QOSType.MOST_ONE);
        INotifier notifier = mock(INotifier.class);
        messaging.setNotifier(notifier);

        //Exercise
        messaging.publish(FAKE_TOPIC, "Hello".getBytes(), QOSType.MOST_ONE, false);

        //Verify
        ArgumentCaptor<byte[]> argument = ArgumentCaptor.forClass(byte[].class);
        verify(notifier).notify(eq(FAKE_CLIENT_ID), eq(FAKE_TOPIC), any(QOSType.class), argument.capture());
        assertEquals("Hello", new String(argument.getValue()));
    }
}
