package org.dna.mqtt.moquette.client;

import java.io.IOException;
import org.dna.mqtt.moquette.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    
    static boolean received;
    
    public static void main(String[] args) throws IOException {
        Client client = new Client("localhost", Server.PORT);
        client.connect();
        
        
        client.subscribe("/topic", new IPublishCallback() {

            public void published(String topic, byte[] message) {
                LOG.info("Received publush notification");
                received = true;
            }
        });
        
        client.publish("/topic", "Test my payload".getBytes());
        client.close();
        
        
        assert received;
    }
    
}
