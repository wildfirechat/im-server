package org.dna.mqtt.moquette.messaging.spi.impl;

import java.util.List;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.junit.Test;
import static org.junit.Assert.*;

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
}
