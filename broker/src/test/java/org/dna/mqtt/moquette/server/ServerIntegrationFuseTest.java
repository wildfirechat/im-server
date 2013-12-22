package org.dna.mqtt.moquette.server;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.fusesource.mqtt.client.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
public class ServerIntegrationFuseTest {
    private static final Logger LOG = LoggerFactory.getLogger(ServerIntegrationPahoTest.class);

    Server m_server;
    MQTT m_mqtt;
    BlockingConnection m_subscriber;
    BlockingConnection m_publisher;
    
//mqtt.setHost("test.mosquitto.org", 1883);


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
//        if (m_mqtt.isConnected()) {
//            m_mqtt.disconnect();
//        }
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
    public void checkReplayofStoredPublishResumeAfter_a_disconnect_cleanSessionFalseQoS1() throws Exception {
        LOG.info("*** checkReplayofStoredPublishResumeAfter_a_disconnect_cleanSessionFalseQoS1 ***");
        MQTT mqtt = new MQTT();
        mqtt.setHost("localhost", 1883); 
        mqtt.setClientId("Publisher");
        m_publisher = mqtt.blockingConnection();
        m_publisher.connect();
        
        m_mqtt.setHost("localhost", 1883); 
        m_mqtt.setCleanSession(false);
        m_mqtt.setClientId("Subscriber");
        m_subscriber = m_mqtt.blockingConnection();
        m_subscriber.connect();
        Topic[] topics = new Topic[]{new Topic("/topic", QoS.AT_LEAST_ONCE)};
        m_subscriber.subscribe(topics);
        
        //force the publisher to send
        m_publisher.publish("/topic", "Hello world MQTT!!-1".getBytes(), QoS.AT_LEAST_ONCE, false);
        
        //read the first message and drop the connection
        Message msg = m_subscriber.receive();
        msg.ack();
        assertEquals("Hello world MQTT!!-1", new String(msg.getPayload()));
        m_subscriber.disconnect();
        
        m_publisher.publish("/topic", "Hello world MQTT!!-2".getBytes(), QoS.AT_LEAST_ONCE, false);
        m_publisher.publish("/topic", "Hello world MQTT!!-3".getBytes(), QoS.AT_LEAST_ONCE, false);
        
        //reconnect and expect to receive the hello 2 message
        m_subscriber = m_mqtt.blockingConnection();
        m_subscriber.connect();
        topics = new Topic[]{new Topic("/topic", QoS.AT_LEAST_ONCE)};
        m_subscriber.subscribe(topics);
        msg = m_subscriber.receive();
        msg.ack();
        assertEquals("Hello world MQTT!!-2", new String(msg.getPayload()));
        m_subscriber.disconnect();
        
        m_subscriber = m_mqtt.blockingConnection();
        m_subscriber.connect();
        topics = new Topic[]{new Topic("/topic", QoS.AT_LEAST_ONCE)};
        m_subscriber.subscribe(topics);
        msg = m_subscriber.receive();
        assertEquals("Hello world MQTT!!-3", new String(msg.getPayload()));
        msg.ack();
        //TODO check topic and content
    }
    
    @Test
    public void checkReplayStoredPublish_forNoCleanSession_qos1() throws Exception {
        LOG.info("*** checkReplayStoredPublish_forNoCleanSession_qos1 ***");
        MQTT mqtt = new MQTT();
        mqtt.setHost("localhost", 1883); 
        mqtt.setClientId("Publisher");
        m_publisher = mqtt.blockingConnection();
        m_publisher.connect();
        
        m_mqtt.setHost("localhost", 1883); 
        m_mqtt.setCleanSession(false);
        m_mqtt.setClientId("Subscriber");
        m_subscriber = m_mqtt.blockingConnection();
        m_subscriber.connect();
        Topic[] topics = new Topic[]{new Topic("/topic", QoS.AT_LEAST_ONCE)};
        m_subscriber.subscribe(topics);
        System.out.println("***Subscriber listen to /topic and disonnect");
        m_subscriber.disconnect();
        
        //force the publisher to send
        m_publisher.publish("/topic", "Hello world MQTT!!-1".getBytes(), QoS.AT_LEAST_ONCE, false);
        System.out.println("***Publisher published to /topic :: Hello world MQTT!!-1");
        
        //reconnect and expect to receive the hello 2 message
        m_subscriber = m_mqtt.blockingConnection();
        m_subscriber.connect();
        topics = new Topic[]{new Topic("/topic", QoS.AT_LEAST_ONCE)};
        m_subscriber.subscribe(topics);
        Message msg = m_subscriber.receive(1, TimeUnit.SECONDS);
        assertNotNull(msg);
        msg.ack();
        assertEquals("Hello world MQTT!!-1", new String(msg.getPayload()));
        System.out.println(String.format("***Subscriber received message <%s> from /topic", new String(msg.getPayload())));
//        m_subscriber.disconnect();
    }
}
