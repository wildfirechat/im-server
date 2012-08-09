package org.dna.mqtt.moquette.server;

import java.io.IOException;
import org.dna.mqtt.moquette.client.Client;
import org.dna.mqtt.moquette.client.IPublishCallback;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

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
        
        assertTrue(received);
    }
    
}
