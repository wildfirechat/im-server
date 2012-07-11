package org.dna.mqtt.moquette.messaging.spi.impl;

import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
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
        assertTrue(messaging.getSubscriptions().contains(new Subscription(clientID, topic, QOSType.MOST_ONE)));
    }
}
