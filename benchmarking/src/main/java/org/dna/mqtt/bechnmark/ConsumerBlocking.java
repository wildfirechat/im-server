package org.dna.mqtt.bechnmark;

import java.net.URISyntaxException;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
public class ConsumerBlocking implements Runnable {
    
    private static final Logger LOG = LoggerFactory.getLogger(ConsumerBlocking.class);
    
    private String m_clientID;

    public ConsumerBlocking(String clientID) {
        m_clientID = clientID;
    }
    
    public void run() {
        MQTT mqtt = new MQTT();
        try {
//            mqtt.setHost("test.mosquitto.org", 1883);
            mqtt.setHost("localhost", 1883);
        } catch (URISyntaxException ex) {
            LOG.error(null, ex);
            return;
        }
        
        mqtt.setClientId(m_clientID);
        BlockingConnection connection = mqtt.blockingConnection();
        try {
            connection.connect();
        } catch (Exception ex) {
            LOG.error("Cant't CONNECT to the server", ex);
            return;
        }
        
        try {
            Topic[] topics = {new Topic("/topic", QoS.AT_MOST_ONCE)};
            byte[] qoses = connection.subscribe(topics);
            LOG.info("Subscribed to topic");
        } catch (Exception ex) {
            LOG.error("Cant't PUSBLISH to the server", ex);
            return;
        }
            
        Message message = null;
        for (int i = 0; i < Producer.PUB_LOOP; i++) {
            try {
                message = connection.receive();
            } catch (Exception ex) {
                LOG.error(null, ex);
                return;
            }
            byte[] payload = message.getPayload();
            StringBuffer sb = new StringBuffer().append("Topic: ").append(message.getTopic())
                    .append(", payload: ").append(new String(payload));
            LOG.info(sb.toString());
        }
            
        try {
            LOG.info("Disconneting");
            connection.disconnect();
            LOG.info("Disconnected");
        } catch (Exception ex) {
            LOG.error("Cant't DISCONNECT to the server", ex);
        }
        
    }
    
}
