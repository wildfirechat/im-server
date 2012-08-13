package org.dna.mqtt.moquette.server;

import java.io.File;
import java.io.IOException;
import org.dna.mqtt.moquette.client.Client;
import org.dna.mqtt.moquette.client.IPublishCallback;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
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
        Client client = new Client("localhost", Server.PORT);
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
        Client client = new Client("localhost", Server.PORT/*, "CLID_123"*/);
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
        Client client = new Client("localhost", Server.PORT, "CLID_123");
        client.connect(false); //without session cleanup
        
        client.subscribe("/topic", new IPublishCallback() {

            public void published(String topic, byte[] message) {
                received = true;
            }
        });
        client.close();
        client.shutdown();
        
        //Exercise that the client maintain the subscriptions
        client = new Client("localhost", Server.PORT, "CLID_123");
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
     * to some topic and exited, if it reconnect with clean session true, the server
     * correctly cleanup every previous subscription
     */
    @Test
    public void testCleanSession_correctlyClientSubscriptions() {
        Client client = new Client("localhost", Server.PORT, "CLID_123");
        client.connect(false); //without session cleanup
        
        client.subscribe("/topic", new IPublishCallback() {

            public void published(String topic, byte[] message) {
                received = true;
            }
        });
        client.close();
        client.shutdown();
        
        //Exercise that the client maintain the subscriptions
        client = new Client("localhost", Server.PORT, "CLID_123");
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
    public void testCleanSession_maintainClientSubscriptions_withServerRestart() throws IOException {
        Client client = new Client("localhost", Server.PORT, "CLID_123");
        client.connect(false); //without session cleanup
        
        client.subscribe("/topic", new IPublishCallback() {

            public void published(String topic, byte[] message) {
                received = true;
            }
        });
        client.close();
        client.shutdown();
        server.stopServer();
        
        server.startServer();
        
        //Exercise that the client maintain the subscriptions
        client = new Client("localhost", Server.PORT, "CLID_123");
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
    
}
