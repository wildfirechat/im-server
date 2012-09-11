package org.dna.mqtt.moquette.client;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    
    static boolean received;
    
    public static void main(String[] args) throws IOException, InterruptedException {
        Client client = new Client("test.mosquitto.org", 1883);
        client.connect();
        
        client.subscribe("/topic", new IPublishCallback() {

            public void published(String topic, byte[] message) {
                received = true;
            }
        });
        
        Thread.sleep(5000);
        
        client.publish("/topic", "Test my payload".getBytes());
        
        client.close();
    }
    
}
