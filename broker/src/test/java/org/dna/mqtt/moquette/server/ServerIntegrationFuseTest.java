package org.dna.mqtt.moquette.server;

import org.fusesource.mqtt.client.*;
import org.dna.mqtt.commons.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;


/**
 * Test the broker using the widely used FuseSource libraries
 * */
public class ServerIntegrationFuseTest {

    Server m_server;
    MQTT m_mqtt;
    BlockingConnection m_connection;

    protected void startServer() throws IOException {
        m_server = new Server();
        m_server.startServer();
    }

    @Before
    public void setUp() throws Exception {
        startServer();

        m_mqtt = new MQTT();
        m_mqtt.setHost("localhost", Constants.PORT);
        m_mqtt.setClientId("TestClient");
    }

    @After
    public void tearDown() throws Exception {
        if (m_connection != null && m_connection.isConnected()) {
            m_connection.disconnect();
        }

        m_server.stopServer();
        File dbFile = new File(Server.STORAGE_FILE_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @Test
    public void testSubscribe() throws Exception {
        m_connection = m_mqtt.blockingConnection();
        m_connection.connect();

        Topic[] topics = {new Topic("/topic", QoS.AT_MOST_ONCE)};
        byte[] futSub = m_connection.subscribe(topics);

        m_connection.publish("/topic", "Test my payload".getBytes(), QoS.AT_MOST_ONCE, false);

        Message msg = m_connection.receive();
        msg.ack();
        assertEquals("/topic", msg.getTopic());
    }
    
    @Test
    public void testCleanSession_maintainClientSubscriptions() throws Exception {
        MQTT localMqtt = new MQTT(m_mqtt);
        localMqtt.setCleanSession(false);
        BlockingConnection connection = localMqtt.blockingConnection();
        connection.connect();
        
        Topic[] topics = {new Topic("/topic", QoS.AT_MOST_ONCE)};
        connection.subscribe(topics);
        connection.disconnect();
        
        //reconnect and publish
        connection = localMqtt.blockingConnection();
        connection.connect();
        connection.publish("/topic", "Test my payload".getBytes(), QoS.AT_MOST_ONCE, false);
        
        Message msg = connection.receive();
        msg.ack();
        assertEquals("/topic", msg.getTopic());
    }
    
    @Test
    public void testCleanSession_maintainClientSubscriptions_againstClientDestruction() throws Exception {
        MQTT localMqtt = new MQTT(m_mqtt);
        localMqtt.setCleanSession(false);
        BlockingConnection connection = localMqtt.blockingConnection();
        connection.connect();
        
        Topic[] topics = {new Topic("/topic", QoS.AT_MOST_ONCE)};
        connection.subscribe(topics);
        connection.disconnect();
        
        connection = localMqtt.blockingConnection();
        connection.connect();
        
        connection.publish("/topic", "Test my payload".getBytes(), QoS.AT_MOST_ONCE, false);
        
        //Verify
        Message msg = connection.receive();
        msg.ack();
        assertEquals("/topic", msg.getTopic());
    }
    
    /**
     * Check that after a client has connected with clean session false, subscribed
     * to some topic and exited, if it reconnect with clean session true, the m_server
     * correctly cleanup every previous subscription
     */
    @Test
    public void testCleanSession_correctlyClientSubscriptions() throws Exception {
        MQTT localMqtt = new MQTT(m_mqtt);
        localMqtt.setCleanSession(false);
        BlockingConnection connection = localMqtt.blockingConnection();
        connection.connect();
        Topic[] topics = {new Topic("/topic", QoS.AT_MOST_ONCE)};
        connection.subscribe(topics);
        connection.disconnect();
        
        //the client reconnects but with cleanSession = true and publish
        m_connection = m_mqtt.blockingConnection();
        m_connection.connect();
        m_connection.publish("/topic", "Test my payload".getBytes(), QoS.AT_MOST_ONCE, false);
        
        //Verify that no publish reach the previous subscription
        Message msg = m_connection.receive(1, TimeUnit.SECONDS);
        assertNull(msg);
    }
    
    @Test
    public void testCleanSession_maintainClientSubscriptions_withServerRestart() throws Exception {
        MQTT localMqtt = new MQTT(m_mqtt);
        localMqtt.setCleanSession(false);
        BlockingConnection connection = localMqtt.blockingConnection();
        connection.connect();
        Topic[] topics = {new Topic("/topic", QoS.AT_MOST_ONCE)};
        connection.subscribe(topics);
        connection.disconnect();
        
        m_server.stopServer();
        
        m_server.startServer();
        
        //the client reconnects but with cleanSession = true and publish
        MQTT anotherLocalMqtt = new MQTT(m_mqtt);
        anotherLocalMqtt.setCleanSession(false);
        BlockingConnection anotherconnection = localMqtt.blockingConnection();
        anotherconnection.connect();
        anotherconnection.publish("/topic", "Test my payload".getBytes(), QoS.AT_MOST_ONCE, false);
        
        //Verify that the message is published due to prevoius subscription
        Message msg = anotherconnection.receive(1, TimeUnit.SECONDS);
        assertEquals("/topic", msg.getTopic());
    }
    
    @Test
    public void testRetain_maintainMessage_againstClientDestruction() throws Exception {
        m_connection = m_mqtt.blockingConnection();
        m_connection.connect(); 
        m_connection.publish("/topic", "Test my payload".getBytes(), QoS.AT_MOST_ONCE, true);
        m_connection.disconnect();
     
        BlockingConnection anotherConnection = m_mqtt.blockingConnection();
        anotherConnection.connect(); 
        anotherConnection.subscribe(new Topic[] {new Topic("/topic", QoS.AT_MOST_ONCE)});
        Message msg = anotherConnection.receive();
        msg.ack();
        
        assertEquals("/topic", msg.getTopic());
    }
    
    @Test
    public void testPublishWithQoS1() throws Exception {
        m_connection = m_mqtt.blockingConnection();
        m_connection.connect();

        //subscribe to a QoS 1 topic
        Topic[] topics = {new Topic("/topic", QoS.AT_LEAST_ONCE)};
        byte[] qoses = m_connection.subscribe(topics);

        //publish a QoS 1 message
        m_connection.publish("/topic", "Hello MQTT".getBytes(), QoS.AT_LEAST_ONCE, false);

        //verify the reception
        Message message = m_connection.receive();
        message.ack();

        assertEquals("Hello MQTT", new String(message.getPayload()));
    }

    @Test
    public void testPublishWithQoS1_notCleanSession() throws Exception {
        MQTT mqtt = new MQTT(m_mqtt);
        mqtt.setCleanSession(false);
        m_connection = mqtt.blockingConnection();
        m_connection.connect();

        //subscribe to a QoS 1 topic
        Topic[] topics = {new Topic("/topic", QoS.AT_LEAST_ONCE)};
        byte[] qoses = m_connection.subscribe(topics);

        m_connection.disconnect();

        //publish a QoS 1 message another client publish a message on the topic
        publishFromAnotherClient("/topic", "Hello MQTT".getBytes(), QoS.AT_LEAST_ONCE);

        //Reconnect
        m_connection = mqtt.blockingConnection();
        m_connection.connect();

        //verify the reception
        Message message = m_connection.receive();
        message.ack();

        assertEquals("Hello MQTT", new String(message.getPayload()));
    }

    private void publishFromAnotherClient(String topic, byte[] payload, QoS qos) throws Exception {
        MQTT mqttPub = new MQTT();
        mqttPub.setHost("localhost", Constants.PORT);
        mqttPub.setClientId("TestClientPUB");
        FutureConnection connectionPub = mqttPub.futureConnection();
        connectionPub.connect().await();
        Future<Void> futurePublish = connectionPub.publish(topic, payload, qos, false);
        futurePublish.await();
        connectionPub.disconnect().await();
    }


    @Test
    public void testPublishWithQoS2() throws Exception {
        m_mqtt.setClientId("TestClient");
        m_connection = m_mqtt.blockingConnection();
        m_connection.connect();

        //subscribe to a QoS 2 topic
        Topic[] topics = {new Topic("/topic", QoS.EXACTLY_ONCE)};
        byte[] qoses = m_connection.subscribe(topics);

        //publish a QoS 2 message
        m_connection.publish("/topic", "Hello MQTT".getBytes(), QoS.EXACTLY_ONCE, false);

        //verify the reception
        Message message = m_connection.receive();
        message.ack();

        assertEquals("Hello MQTT", new String(message.getPayload()));
    }

    @Test
    public void testPublishReceiveWithQoS2() throws Exception {
        m_mqtt.setCleanSession(false);
        m_connection = m_mqtt.blockingConnection();
        m_connection.connect();

        //subscribe to a QoS 2 topic
        Topic[] topics = {new Topic("/topic", QoS.EXACTLY_ONCE)};
        byte[] qoses = m_connection.subscribe(topics);

//        m_connection.disconnect().await();

        //publish a QoS 1 message another client publish a message on the topic
        publishFromAnotherClient("/topic", "Hello MQTT".getBytes(), QoS.EXACTLY_ONCE);


        //Reconnect
        /*m_connection = m_mqtt.futureConnection();
        m_connection.connect().await();*/

        //verify the reception
        Message message = m_connection.receive();
        message.ack();

        assertEquals("Hello MQTT", new String(message.getPayload()));
    }

}
