package org.dna.mqtt.moquette.server;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.dna.mqtt.moquette.client.Client;
import org.dna.mqtt.moquette.client.IPublishCallback;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.commons.Constants;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author andrea
 */
public class ServerIntegrationTest {
    
    boolean received;
    Server server;
    
    protected void startServer() throws IOException {
        server = new Server();
        server.startServer();
    }
    
    @Before
    public void setUp() throws IOException {
        startServer();
    }
    
    @After
    public void tearDown() {
        server.stopServer();
        File dbFile = new File(Server.STORAGE_FILE_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }
    
    @Test
    public void testSubscribe() throws IOException, InterruptedException {
//        startServer();
        Client client = new Client("localhost", Constants.PORT);
//        Client client = new Client("test.mosquitto.org", 1883);
        client.connect();


        client.subscribe("/topic", new IPublishCallback() {

            public void published(String topic, byte[] message) {
                received = true;
            }
        });

        client.publish("/topic", "Test my payload".getBytes());

        client.close();
        client.shutdown();

        assertTrue(received);
    }


    @Test
    public void testCleanSession_maintainClientSubscriptions() {
        Client client = new Client("localhost", Constants.PORT/*, "CLID_123"*/);
        client.connect(false); //without session cleanup

        client.subscribe("/topic", new IPublishCallback() {

            public void published(String topic, byte[] message) {
                received = true;
            }
        });
        client.close();

        //Exercise that the client maintain the subscriptions
        client.connect(false);
        client.publish("/topic", "Test my payload".getBytes());
        client.close();

        //Verify
        assertTrue(received);

        //TearDown
        client.shutdown();
    }

    @Test
    public void testCleanSession_maintainClientSubscriptions_againstClientDestruction() {
        Client client = new Client("localhost", Constants.PORT, "CLID_123");
        client.connect(false); //without session cleanup

        client.subscribe("/topic", new IPublishCallback() {

            public void published(String topic, byte[] message) {
                received = true;
            }
        });
        client.close();
        client.shutdown();

        //Exercise that the client maintain the subscriptions
        client = new Client("localhost", Constants.PORT, "CLID_123");
        client.register("/topic", new IPublishCallback() {

            public void published(String topic, byte[] message) {
                received = true;
            }
        });
        client.connect(false);
        client.publish("/topic", "Test my payload".getBytes());
        client.close();

        //Verify
        assertTrue(received);

        //TearDown
        client.shutdown();
    }


    /**
     * Check that after a client has connected with clean session false, subscribed
     * to some topic and exited, if it reconnect with clean session true, the m_server
     * correctly cleanup every previous subscription
     */
    @Test
    public void testCleanSession_correctlyClientSubscriptions() {
        Client client = new Client("localhost", Constants.PORT, "CLID_123");
        client.connect(false); //without session cleanup

        client.subscribe("/topic", new IPublishCallback() {

            public void published(String topic, byte[] message) {
                received = true;
            }
        });
        client.close();
        client.shutdown();

        //Exercise that the client maintain the subscriptions
        client = new Client("localhost", Constants.PORT, "CLID_123");
        client.register("/topic", new IPublishCallback() {

            public void published(String topic, byte[] message) {
                received = true;
            }
        });
        client.connect(true);
        client.publish("/topic", "Test my payload".getBytes());
        client.close();

        //Verify
        assertFalse(received);

        //TearDown
        client.shutdown();
    }
    
    
    @Test
    public void testCleanSession_maintainClientSubscriptions_withServerRestart() throws IOException, InterruptedException {
        final CountDownLatch barrier = new CountDownLatch(1);
        Client client = new Client("localhost", Constants.PORT, "CLID_123");
        client.connect(false); //without session cleanup
        
        client.subscribe("/topic", new IPublishCallback() {

            public void published(String topic, byte[] message) {
                received = true;
                barrier.countDown();
            }
        });
        //never called because no notification is pending
        assertFalse(barrier.await(1, TimeUnit.SECONDS));

        client.close();
        client.shutdown();
        server.stopServer();
        
        server.startServer();

        final CountDownLatch barrier2 = new CountDownLatch(1);

        //Exercise that the client maintain the subscriptions
        client = new Client("localhost", Constants.PORT, "CLID_123");
        client.register("/topic", new IPublishCallback() {

            public void published(String topic, byte[] message) {
                received = true;
                barrier2.countDown();
            }
        });
        client.connect(false); 
        client.publish("/topic", "Test my payload".getBytes());
        client.close();
        
        //Verify
        assertTrue(barrier2.await(1, TimeUnit.SECONDS));
        assertTrue(received);
        
        //TearDown 
        client.shutdown();
    }
    
    
    @Test
    public void testRetain_maintainMessage_againstClientDestruction() throws InterruptedException {
        final CountDownLatch barrier = new CountDownLatch(1);
        Client client = new Client("localhost", Constants.PORT, "CLID_123");
        client.connect();

        client.publish("/topic", "Test my payload".getBytes(), true);
        client.close();
        client.shutdown();

        //Exercise that the client maintain the subscriptions
        client = new Client("localhost", Constants.PORT, "CLID_123");
        client.connect();
        client.subscribe("/topic", new IPublishCallback() {

            public void published(String topic, byte[] message) {
                received = true;
                barrier.countDown();
            }
        });
        client.close();

        //Verify
        barrier.await(1, TimeUnit.SECONDS);
        assertTrue(received);

        //TearDown
        client.shutdown();
    }


    @Ignore
    public void testUnsubscribe_do_not_notify_anymore_same_session() throws InterruptedException {
        Client client = new Client("localhost", Constants.PORT, "CLID_123");
//        Client client = new Client("test.mosquitto.org", 1883);
        client.connect();

        final CountDownLatch barrier = new CountDownLatch(1);

        client.subscribe("/topic", new IPublishCallback() {

            public void published(String topic, byte[] message) {
                received = true;
                barrier.countDown();
            }
        });

        client.publish("/topic", "Test my payload".getBytes());

        //wait 1 second to receive the published message
        boolean unlocked = barrier.await(1000, TimeUnit.MILLISECONDS);
        assertTrue(unlocked); //we were unlocked by the message reception
        assertTrue(received);

        //reinit the flag
        received = false;
        //unsubscrbe and republish to check no notification is raised up
        client.unsubscribe("/topic");
        client.publish("/topic", "Test my payload".getBytes());
        assertFalse(received);

        client.close();
        client.shutdown();
    }

    @Test
    public void testUnsubscribe_do_not_notify_anymore_new_session() throws InterruptedException {
        Client client = new Client("localhost", Constants.PORT, "CLID_123");
//        Client client = new Client("test.mosquitto.org", 1883);
        client.connect();

        final CountDownLatch barrier = new CountDownLatch(1);

        client.subscribe("/topic", new IPublishCallback() {

            public void published(String topic, byte[] message) {
                received = true;
                barrier.countDown();
            }
        });

        client.publish("/topic", "Test my payload".getBytes());

        //wait 1 second to receive the published message
        boolean unlocked = barrier.await(1000, TimeUnit.MILLISECONDS);
        assertTrue(unlocked); //we were unlocked by the message reception
        assertTrue(received);

        //unsubscrbe and republish to check no notification is raised up
        client.unsubscribe("/topic");

        client.close();
        client.shutdown();

        //Exercise that the client maintain the subscriptions
        client = new Client("localhost", Constants.PORT, "CLID_123");
        client.connect();

        //reinit the flag
        received = false;
        final CountDownLatch barrier2 = new CountDownLatch(1);

        client.publish("/topic", "Test my payload".getBytes());
        unlocked = barrier2.await(1000, TimeUnit.MILLISECONDS);
        assertFalse(unlocked); //we were unlocked by the timeout exipration, no message received
        assertFalse(received);

        client.close();
        client.shutdown();
    }

    /*-------------- QoS 1 tests ----*/
    @Test
    public void testPublishWithQoS1() throws IOException, InterruptedException {
//        startServer();
        Client client = new Client("localhost", Constants.PORT);
//        Client client = new Client("test.mosquitto.org", 1883);
        client.connect();


        client.subscribe("/topic", new IPublishCallback() {

            public void published(String topic, byte[] message) {
                received = true;
            }
        });

        client.publish("/topic", "Test my payload".getBytes(), AbstractMessage.QOSType.LEAST_ONE, false);

        client.close();
        client.shutdown();

        assertTrue(received);
    }
    
}
