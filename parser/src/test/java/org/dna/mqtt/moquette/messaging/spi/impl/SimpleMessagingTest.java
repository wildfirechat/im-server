package org.dna.mqtt.moquette.messaging.spi.impl;

import org.mockito.ArgumentCaptor;
import java.util.List;
import org.dna.mqtt.moquette.messaging.spi.INotifier;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 * @author andrea
 */
public class SimpleMessagingTest {
    
//    public SimpleMessagingTest() {
//    }
//
//    @BeforeClass
//    public static void setUpClass() throws Exception {
//    }
//
//    @AfterClass
//    public static void tearDownClass() throws Exception {
//    }
//    
//    @Before
//    public void setUp() {
//    }
//    
//    @After
//    public void tearDown() {
//    }

    @Test
    public void testSubscribe() {
        String clientID = "FAKE_123";
        String topic = "/news";
        SimpleMessaging messaging = new SimpleMessaging();
        
        //Exercise
        messaging.subscribe(clientID, topic, QOSType.MOST_ONE);
        
        //Verify
        Subscription expectedSubscription = new Subscription(clientID, topic, QOSType.MOST_ONE);
        assertTrue(messaging.getSubscriptions().contains(expectedSubscription));
    }
    
    @Test
    public void testDoubleSubscribe() {
        String clientID = "FAKE_123";
        String topic = "/news";
        SimpleMessaging messaging = new SimpleMessaging();
        messaging.subscribe(clientID, topic, QOSType.MOST_ONE);
        
        //Exercise
        messaging.subscribe(clientID, topic, QOSType.MOST_ONE);
        
        //Verify
        Subscription subscription = new Subscription(clientID, topic, QOSType.MOST_ONE);
        assertEquals(1, countMatchingSubscriptions(messaging.getSubscriptions(), subscription));
    }
    
    private int countMatchingSubscriptions(List<Subscription> l, Subscription matchingSub) {
        int count = 0;
        for (Subscription s : l) {
            if (s.equals(matchingSub)) {
                count++;
            }
        }
        return count;
    }
    
    
    @Test
    public void testPublish() {
        String clientID = "FAKE_123";
        String topic = "/news";
        SimpleMessaging messaging = new SimpleMessaging();
        messaging.subscribe(clientID, topic, QOSType.MOST_ONE);
        INotifier notifier = mock(INotifier.class);
        messaging.setNotifier(notifier);
        
        //Exercise
        messaging.publish(topic, "Hello".getBytes(), QOSType.MOST_ONE, false);
        
        //Verify
        ArgumentCaptor<byte[]> argument = ArgumentCaptor.forClass(byte[].class);
        verify(notifier).notify(eq(clientID), eq(topic), any(QOSType.class), argument.capture());
        assertEquals("Hello", new String(argument.getValue()));
    }
}
