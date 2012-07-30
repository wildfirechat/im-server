package org.dna.mqtt.moquette.client;

import java.io.IOException;
import java.util.logging.Logger;
import org.dna.mqtt.moquette.server.Server;

/**
 *
 * @author andrea
 */
public class Main {
    private static final Logger LOG = Logger.getLogger(Main.class.getName());
    
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
