package org.dna.mqtt.bechnmark;

import java.net.URISyntaxException;
import org.fusesource.mqtt.client.Future;
import org.fusesource.mqtt.client.FutureConnection;
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
public class ConsumerFuture implements Runnable {
    
    private static final Logger LOG = LoggerFactory.getLogger(ConsumerFuture.class);
    
    private String m_clientID;

    public ConsumerFuture(String clientID) {
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
        FutureConnection connection = mqtt.futureConnection();
        Future<Void> futConn = connection.connect();
        
        try {
            futConn.await();
        } catch (Exception ex) {
            LOG.error("Cant't CONNECT to the server", ex);
            return;
        }

        
        Topic[] topics = {new Topic("/topic", QoS.AT_MOST_ONCE)};
        Future<byte[]> futSub = connection.subscribe(topics);
        try {
            byte[] qoses = futSub.await();
            LOG.info("Subscribed to topic");
        } catch (Exception ex) {
            LOG.error("Cant't PUSBLISH to the server", ex);
            return;
        }
            
        Message message = null;
        for (int i = 0; i < Producer.PUB_LOOP; i++) {
            Future<Message> futReceive = connection.receive();
            try {
                message = futReceive.await();
            } catch (Exception ex) {
                LOG.error(null, ex);
                return;
            }
            byte[] payload = message.getPayload();
            StringBuffer sb = new StringBuffer().append("Topic: ").append(message.getTopic())
                    .append(", payload: ").append(new String(payload));
            LOG.info(sb.toString());
        }
            
        Future<Void> f4 =  connection.disconnect();
        try {
            LOG.info("Disconneting");
            f4.await();
            LOG.info("Disconnected");
        } catch (Exception ex) {
            LOG.error("Cant't DISCONNECT to the server", ex);
        }
        
    }
    
}
