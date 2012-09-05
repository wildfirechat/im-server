package org.dna.mqtt.moquette.server;

import org.dna.mqtt.moquette.client.Client;
import org.dna.mqtt.moquette.client.IPublishCallback;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.fusesource.mqtt.client.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;


/**
 * Test the broker using the widely used FuseSource libraries
 * */
public class ServerIntegrationFuseTest {

    boolean received;
    Server server;

    protected void startServer() throws IOException {
        server = new Server();
        server.startServer();
    }

    @Before
    public void setUp() throws IOException {
        startServer();
    }

    @After
    public void tearDown() {
        server.stopServer();
        File dbFile = new File(Server.STORAGE_FILE_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }

    @Ignore
    public void testSubscribe() throws Exception {
//        startServer();
        MQTT mqtt = new MQTT();
        mqtt.setHost("localhost", Server.PORT);
        mqtt.setClientId("TestClient");
        FutureConnection connection = mqtt.futureConnection();
        connection.connect().await();

        Topic[] topics = {new Topic("/topic", QoS.AT_MOST_ONCE)};
        Future<byte[]> futSub = connection.subscribe(topics);

        connection.publish("/topic", "Test my payload".getBytes(), QoS.AT_MOST_ONCE, false).await();

        byte[] qoses = futSub.await();
        assertEquals(1, qoses.length);
        connection.disconnect().await();
    }

    @Ignore
    public void testPublishWithQoS1() throws Exception {
        MQTT mqtt = new MQTT();
        mqtt.setHost("localhost", Server.PORT);
        mqtt.setClientId("TestClient");
        FutureConnection connection = mqtt.futureConnection();
        connection.connect().await();

        //subscribe to a QoS 1 topic
        Topic[] topics = {new Topic("/topic", QoS.AT_LEAST_ONCE)};
        Future<byte[]> subscribeFuture = connection.subscribe(topics);
        byte[] qoses = subscribeFuture.await();

        Future<Message> receive = connection.receive();

        //publish a QoS 1 message
        Future<Void> f3 = connection.publish("/topic", "Hello MQTT".getBytes(), QoS.AT_LEAST_ONCE, false);

        //verify the reception
        Message message = receive.await();
        message.ack();

        assertEquals("Hello MQTT", new String(message.getPayload()));

        connection.disconnect().await();
    }

    @Test
    public void testPublishWithQoS1_notCleanSession() throws Exception {
        MQTT mqtt = new MQTT();
        mqtt.setHost("localhost", Server.PORT);
        mqtt.setClientId("TestClient");
        mqtt.setCleanSession(false);
        FutureConnection connection = mqtt.futureConnection() ;
        connection.connect().await();

        //subscribe to a QoS 1 topic
        Topic[] topics = {new Topic("/topic", QoS.AT_LEAST_ONCE)};
        Future<byte[]> subscribeFuture = connection.subscribe(topics);
        byte[] qoses = subscribeFuture.await();

        connection.disconnect().await();

        //publish a QoS 1 message another client publish a message on the topic
        publishFromAnotherClient("/topic", "Hello MQTT".getBytes(), QoS.AT_LEAST_ONCE);


        //Reconnect
        connection = mqtt.futureConnection();
        connection.connect().await();
        Future<Message> receive = connection.receive();

        //verify the reception
        Message message = receive.await();
        message.ack();

        assertEquals("Hello MQTT", new String(message.getPayload()));

        connection.disconnect().await();
    }

    private void publishFromAnotherClient(String topic, byte[] payload, QoS qos) throws Exception {
        MQTT mqttPub = new MQTT();
        mqttPub.setHost("localhost", Server.PORT);
        mqttPub.setClientId("TestClientPUB");
        FutureConnection connectionPub = mqttPub.futureConnection();
        connectionPub.connect().await();
        Future<Void> futurePublish = connectionPub.publish(topic, payload, qos, false);
        futurePublish.await();
        connectionPub.disconnect().await();
    }
}
