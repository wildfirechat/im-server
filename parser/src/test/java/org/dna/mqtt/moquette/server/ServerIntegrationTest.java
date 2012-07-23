package org.dna.mqtt.moquette.server;

import java.io.IOException;
import org.apache.mina.core.service.IoConnector;
import org.dna.mqtt.moquette.client.Client;
import org.junit.Test;

/**
 *
 * @author andrea
 */
public class ServerIntegrationTest {
    
    IoConnector connector;
    
    protected void startServer() throws IOException {
        new Server().startServer();
    }
    
    @Test
    public void testSubscribe() throws IOException, InterruptedException {
        startServer();
        Client client = new Client("localhost", Server.PORT);
        client.connect();
        
        client.publish("/topic", "Test my payload".getBytes());
        //TODO client.subscribe("/topic", callback)
        
        client.close();
    }
    
}
