package org.dna.mqtt.moquette.server;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author andrea
 */
public class ServerRestartIntegrationTest {
    
    Server m_server;
    MQTT m_mqtt;
    BlockingConnection m_subscriber;
    BlockingConnection m_publisher;
    
    protected void startServer() throws IOException {
        m_server = new Server();
        m_server.startServer();
    }

    @Before
    public void setUp() throws Exception {
        startServer();

        m_mqtt = new MQTT();
        m_mqtt.setHost("localhost", 1883);
    }

    @After
    public void tearDown() throws Exception {
        if (m_subscriber != null) {
            m_subscriber.disconnect();
        }
        
        if (m_publisher != null) {
            m_publisher.disconnect();
        }

        m_server.stopServer();
        File dbFile = new File(Server.STORAGE_FILE_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }
    
    
    @Test
    public void checkRestartCleanSubscriptionTree() throws Exception {
        //subscribe to /topic
        m_mqtt.setHost("localhost", 1883); 
        m_mqtt.setCleanSession(false);
        m_mqtt.setClientId("Subscriber");
        m_subscriber = m_mqtt.blockingConnection();
        m_subscriber.connect();
        Topic[] topics = new Topic[]{new Topic("/topic", QoS.AT_LEAST_ONCE)};
        m_subscriber.subscribe(topics);
        m_subscriber.disconnect();
        
        //shutdown the server
        m_server.stopServer();
        File dbFile = new File(Server.STORAGE_FILE_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        
        //restart the server
        m_server.startServer();
        
        //reconnect the Subscriber subscribing to the same /topic but different QoS
        m_subscriber = m_mqtt.blockingConnection();
        m_subscriber.connect();
        topics = new Topic[]{new Topic("/topic", QoS.EXACTLY_ONCE)};
        m_subscriber.subscribe(topics);
        
        //shoud be just one registration so a publisher receive one notification
        MQTT mqtt = new MQTT();
        mqtt.setHost("localhost", 1883); 
        mqtt.setClientId("Publisher");
        m_publisher = mqtt.blockingConnection();
        m_publisher.connect();
        m_publisher.publish("/topic", "Hello world MQTT!!".getBytes(), QoS.AT_LEAST_ONCE, false);
        
        //read the messages
        Message msg = m_subscriber.receive();
        msg.ack();
        assertEquals("Hello world MQTT!!", new String(msg.getPayload()));
        //no more messages on the same topic will be received
        assertNull(m_subscriber.receive(1, TimeUnit.SECONDS));
    }
}