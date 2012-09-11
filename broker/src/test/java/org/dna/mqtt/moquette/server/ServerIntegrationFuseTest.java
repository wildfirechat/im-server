package org.dna.mqtt.moquette.server;

import org.fusesource.mqtt.client.*;
import org.dna.mqtt.commons.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;


/**
 * Test the broker using the widely used FuseSource libraries
 * */
public class ServerIntegrationFuseTest {

    Server m_server;
    MQTT m_mqtt;
    FutureConnection m_connection;

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
        m_connection.disconnect().await();

        m_server.stopServer();
        File dbFile = new File(Server.STORAGE_FILE_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @Test
    public void testSubscribe() throws Exception {
        m_connection = m_mqtt.futureConnection();
        m_connection.connect().await();

        Topic[] topics = {new Topic("/topic", QoS.AT_MOST_ONCE)};
        Future<byte[]> futSub = m_connection.subscribe(topics);

        m_connection.publish("/topic", "Test my payload".getBytes(), QoS.AT_MOST_ONCE, false).await();

        byte[] qoses = futSub.await();
        assertEquals(1, qoses.length);
    }

    @Test
    public void testPublishWithQoS1() throws Exception {
        m_mqtt.setClientId("TestClient");
        m_connection = m_mqtt.futureConnection();
        m_connection.connect().await();

        //subscribe to a QoS 1 topic
        Topic[] topics = {new Topic("/topic", QoS.AT_LEAST_ONCE)};
        Future<byte[]> subscribeFuture = m_connection.subscribe(topics);
        byte[] qoses = subscribeFuture.await();

        Future<Message> receive = m_connection.receive();

        //publish a QoS 1 message
        Future<Void> f3 = m_connection.publish("/topic", "Hello MQTT".getBytes(), QoS.AT_LEAST_ONCE, false);

        //verify the reception
        Message message = receive.await();
        message.ack();

        assertEquals("Hello MQTT", new String(message.getPayload()));
    }

    @Test
    public void testPublishWithQoS1_notCleanSession() throws Exception {
        m_mqtt.setCleanSession(false);
        m_connection = m_mqtt.futureConnection() ;
        m_connection.connect().await();

        //subscribe to a QoS 1 topic
        Topic[] topics = {new Topic("/topic", QoS.AT_LEAST_ONCE)};
        Future<byte[]> subscribeFuture = m_connection.subscribe(topics);
        byte[] qoses = subscribeFuture.await();

        m_connection.disconnect().await();

        //publish a QoS 1 message another client publish a message on the topic
        publishFromAnotherClient("/topic", "Hello MQTT".getBytes(), QoS.AT_LEAST_ONCE);


        //Reconnect
        m_connection = m_mqtt.futureConnection();
        m_connection.connect().await();
        Future<Message> receive = m_connection.receive();

        //verify the reception
        Message message = receive.await();
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
        m_connection = m_mqtt.futureConnection();
        m_connection.connect().await();

        //subscribe to a QoS 2 topic
        Topic[] topics = {new Topic("/topic", QoS.EXACTLY_ONCE)};
        Future<byte[]> subscribeFuture = m_connection.subscribe(topics);
        byte[] qoses = subscribeFuture.await();

        Future<Message> receive = m_connection.receive();

        //publish a QoS 2 message
        Future<Void> f3 = m_connection.publish("/topic", "Hello MQTT".getBytes(), QoS.EXACTLY_ONCE, false);

        //verify the reception
        Message message = receive.await();
        message.ack();

        assertEquals("Hello MQTT", new String(message.getPayload()));
    }

    @Test
    public void testPublishReceiveWithQoS2() throws Exception {
        m_mqtt.setCleanSession(false);
        m_connection = m_mqtt.futureConnection();
        m_connection.connect().await();

        //subscribe to a QoS 2 topic
        Topic[] topics = {new Topic("/topic", QoS.EXACTLY_ONCE)};
        Future<byte[]> subscribeFuture = m_connection.subscribe(topics);
        byte[] qoses = subscribeFuture.await();

//        m_connection.disconnect().await();

        //publish a QoS 1 message another client publish a message on the topic
        publishFromAnotherClient("/topic", "Hello MQTT".getBytes(), QoS.EXACTLY_ONCE);


        //Reconnect
        /*m_connection = m_mqtt.futureConnection();
        m_connection.connect().await();*/
        Future<Message> receive = m_connection.receive();

        //verify the reception
        Message message = receive.await();
        message.ack();

        assertEquals("Hello MQTT", new String(message.getPayload()));
    }

}
