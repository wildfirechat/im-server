/*
 * Copyright (c) 2012-2018 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package io.moquette.server;

import io.moquette.server.config.IConfig;
import io.moquette.server.config.MemoryConfig;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class ServerIntegrationPahoTest {

    private static final Logger LOG = LoggerFactory.getLogger(ServerIntegrationPahoTest.class);

    static MqttClientPersistence s_dataStore;
    static MqttClientPersistence s_pubDataStore;

    Server m_server;
    IMqttClient m_client;
    IMqttClient m_publisher;
    MessageCollector m_messagesCollector;
    IConfig m_config;

    @BeforeClass
    public static void beforeTests() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        s_dataStore = new MqttDefaultFilePersistence(tmpDir);
        s_pubDataStore = new MqttDefaultFilePersistence(tmpDir + File.separator + "publisher");
    }

    protected void startServer() throws IOException {
        m_server = new Server();
        final Properties configProps = IntegrationUtils.prepareTestProperties();
        m_config = new MemoryConfig(configProps);
        m_server.startServer(m_config);
    }

    @Before
    public void setUp() throws Exception {
        startServer();

        m_client = new MqttClient("tcp://localhost:1883", "TestClient", s_dataStore);
        m_messagesCollector = new MessageCollector();
        m_client.setCallback(m_messagesCollector);

        m_publisher = new MqttClient("tcp://localhost:1883", "Publisher", s_pubDataStore);
    }

    @After
    public void tearDown() throws Exception {
        if (m_client != null && m_client.isConnected()) {
            m_client.disconnect();
        }

        if (m_publisher != null && m_publisher.isConnected()) {
            m_publisher.disconnect();
        }

        stopServer();
    }

    private void stopServer() {
        m_server.stopServer();
    }

    @Ignore("This test hasn't any meaning using in memory storage service")
    @Test
    public void testCleanSession_maintainClientSubscriptions_withServerRestart() throws Exception {
        LOG.info("*** testCleanSession_maintainClientSubscriptions_withServerRestart ***");
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);
        m_client.connect(options);
        m_client.subscribe("/topic", 0);
        m_client.disconnect();

        m_server.stopServer();

        m_server.startServer(IntegrationUtils.prepareTestProperties());

        // reconnect and publish
        m_client.connect(options);
        m_client.publish("/topic", "Test my payload".getBytes(UTF_8), 0, false);

        assertEquals("/topic", m_messagesCollector.getTopic());
    }

    /**
     * subscriber A connect and subscribe on "a/b" QoS 1 subscriber B connect and subscribe on "a/+"
     * BUT with QoS 2 publisher connects and send a message "hello" on "a/b" subscriber A must
     * receive a notification with QoS1 subscriber B must receive a notification with QoS2
     */
    @Test
    public void checkSubscribersGetCorrectQosNotifications() throws Exception {
        LOG.info("*** checkSubscribersGetCorrectQosNotifications ***");
        String tmpDir = System.getProperty("java.io.tmpdir");

        MqttClientPersistence dsSubscriberA = new MqttDefaultFilePersistence(tmpDir + File.separator + "subscriberA");

        MqttClient subscriberA = new MqttClient("tcp://localhost:1883", "SubscriberA", dsSubscriberA);
        MessageCollector cbSubscriberA = new MessageCollector();
        subscriberA.setCallback(cbSubscriberA);
        subscriberA.connect();
        subscriberA.subscribe("a/b", 1);

        MqttClientPersistence dsSubscriberB = new MqttDefaultFilePersistence(tmpDir + File.separator + "subscriberB");

        MqttClient subscriberB = new MqttClient("tcp://localhost:1883", "SubscriberB", dsSubscriberB);
        MessageCollector cbSubscriberB = new MessageCollector();
        subscriberB.setCallback(cbSubscriberB);
        subscriberB.connect();
        subscriberB.subscribe("a/+", 2);

        m_client.connect();
        m_client.publish("a/b", "Hello world MQTT!!".getBytes(UTF_8), 2, false);

        MqttMessage messageOnA = cbSubscriberA.waitMessage(1);
        assertEquals("Hello world MQTT!!", new String(messageOnA.getPayload(), UTF_8));
        assertEquals(1, messageOnA.getQos());
        subscriberA.disconnect();

        MqttMessage messageOnB = cbSubscriberB.waitMessage(1);
        assertEquals("Hello world MQTT!!", new String(messageOnB.getPayload(), UTF_8));
        assertEquals(2, messageOnB.getQos());
        subscriberB.disconnect();
    }

    @Test
    public void testSubcriptionDoesntStayActiveAfterARestart() throws Exception {
        LOG.info("*** testSubcriptionDoesntStayActiveAfterARestart ***");
        String tmpDir = System.getProperty("java.io.tmpdir");
        // clientForSubscribe1 connect and subscribe to /topic QoS2
        MqttClientPersistence dsSubscriberA = new MqttDefaultFilePersistence(
                tmpDir + File.separator + "clientForSubscribe1");

        MqttClient clientForSubscribe1 = new MqttClient("tcp://localhost:1883", "clientForSubscribe1", dsSubscriberA);
        MessageCollector cbSubscriber1 = new MessageCollector();
        clientForSubscribe1.setCallback(cbSubscriber1);
        clientForSubscribe1.connect();
        clientForSubscribe1.subscribe("topic", 0);

        // server stop
        m_server.stopServer();
        System.out.println("\n\n SEVER REBOOTING \n\n");
        // server start
        startServer();

        // clientForSubscribe2 connect and subscribe to /topic QoS2
        MqttClientPersistence dsSubscriberB = new MqttDefaultFilePersistence(
                tmpDir + File.separator + "clientForSubscribe2");
        MqttClient clientForSubscribe2 = new MqttClient("tcp://localhost:1883", "clientForSubscribe2", dsSubscriberB);
        MessageCollector cbSubscriber2 = new MessageCollector();
        clientForSubscribe2.setCallback(cbSubscriber2);
        clientForSubscribe2.connect();
        clientForSubscribe2.subscribe("topic", 0);

        // clientForPublish publish on /topic with QoS2 a message
        MqttClientPersistence dsSubscriberPUB = new MqttDefaultFilePersistence(
                tmpDir + File.separator + "clientForPublish");
        MqttClient clientForPublish = new MqttClient("tcp://localhost:1883", "clientForPublish", dsSubscriberPUB);
        clientForPublish.connect();
        clientForPublish.publish("topic", "Hello".getBytes(UTF_8), 2, true);

        // verify clientForSubscribe1 doesn't receive a notification but clientForSubscribe2 yes
        LOG.info("Before waiting to receive 1 sec from {}", clientForSubscribe1.getClientId());
        assertFalse(clientForSubscribe1.isConnected());
        assertTrue(clientForSubscribe2.isConnected());
        LOG.info("Waiting to receive 1 sec from {}", clientForSubscribe2.getClientId());
        MqttMessage messageOnB = cbSubscriber2.waitMessage(1);
        assertEquals("Hello", new String(messageOnB.getPayload(), UTF_8));
    }

//    @Test
//    public void testForceClientDisconnection_issue116() throws Exception {
//        LOG.info("*** testForceClientDisconnection_issue118 ***");
//        MessageCollector cbSubscriber1 = new MessageCollector();
//        MqttClient clientXA = createClient("subscriber", "X", cbSubscriber1);
//        LOG.info("Connected 'subscriber' first time");
//        clientXA.subscribe("topic", 0);
//        LOG.info("Subscribed 'topic' from 'subscriber' first time");
//
//        MqttClient clientXB = createClient("publisher", "X");
//        LOG.info("Connected 'publisher' first time");
//        clientXB.publish("topic", "Hello".getBytes(), 2, false);
//        LOG.info("Published on 'topic' from 'publisher' first time");
//
//        LOG.info("Creating second new 'subscriber'");
//        MessageCollector cbSubscriber2 = new MessageCollector();
//        MqttClient clientYA = createClient("subscriber", "Y", cbSubscriber2);
//        LOG.info("Connected 'subscriber' second time");
//        clientYA.subscribe("topic", 0);
//
//        MqttClient clientYB = createClient("publisher", "Y");
//        clientYB.publish("topic", "Hello 2".getBytes(), 2, true);
//
//        // Verify that the second subscriber client get notified and not the first.
//        assertTrue(cbSubscriber1.connectionLost());
//        assertEquals("Hello 2", new String(cbSubscriber2.waitMessage(1).getPayload()));
//    }

//    @Test
//    public void testConnectSubPub_cycle_getTimeout_on_second_disconnect_issue142() throws Exception {
//        LOG.info("*** testConnectSubPub_cycle_getTimeout_on_second_disconnect_issue142 ***");
//        MqttConnectOptions options = new MqttConnectOptions();
//        options.setCleanSession(false);
//        m_client.connect(options);
//        m_client.subscribe("/topic", 0);
//        m_client.publish("/topic", "Hello".getBytes(), 0, true);
//        m_client.disconnect();
//
//        // second loop
//        m_client.connect(options);
//        m_client.subscribe("/topic", 0);
//        m_client.publish("/topic", "Hello".getBytes(), 0, true);
//        m_client.disconnect(); // this should give timeout
//
//        assertFalse("after a disconnect the client should be disconnected", m_client.isConnected());
//    }

//    @Test
//    public void checkReplayofStoredPublishResumeAfter_a_disconnect_cleanSessionFalseQoS1() throws Exception {
//        LOG.info("*** checkReplayofStoredPublishResumeAfter_a_disconnect_cleanSessionFalseQoS1 ***");
//        // MessageCollector m_messagesCollector = new MessageCollector();
//        // m_client.setCallback(m_messagesCollector);
//        m_publisher.connect();
//
//        MqttConnectOptions options = new MqttConnectOptions();
//        options.setCleanSession(false);
//        m_client.connect(options);
//        m_client.subscribe("/topic", 1);
//
//        // force the publisher to send
//        m_publisher.publish("/topic", "Hello world MQTT!!-1".getBytes(), 1, false);
//
//        // read the first message and drop the connection
//        MqttMessage msg = m_messagesCollector.waitMessage(1);
//        assertEquals("Hello world MQTT!!-1", new String(msg.getPayload()));
//        m_client.disconnect();
//
//        m_publisher.publish("/topic", "Hello world MQTT!!-2".getBytes(), 1, false);
//        m_publisher.publish("/topic", "Hello world MQTT!!-3".getBytes(), 1, false);
//
//        // reconnect and expect to receive the hello 2 message
//        m_client.connect(options);
//        msg = m_messagesCollector.waitMessage(1);
//        assertEquals("Hello world MQTT!!-2", new String(msg.getPayload()));
//
//        msg = m_messagesCollector.waitMessage(1);
//        assertEquals("Hello world MQTT!!-3", new String(msg.getPayload()));
//        m_client.disconnect();
//    }

//    @Test
//    public void checkReplayStoredPublish_forNoCleanSession_qos1() throws Exception {
//        LOG.info("*** checkReplayStoredPublish_forNoCleanSession_qos1 ***");
//        MessageCollector messagesCollector = new MessageCollector();
//        m_client.setCallback(messagesCollector);
//        m_publisher.connect();
//
//        MqttConnectOptions options = new MqttConnectOptions();
//        options.setCleanSession(false);
//        m_client.connect(options);
//        m_client.subscribe("/topic", 1);
//        m_client.disconnect();
//
//        // force the publisher to send
//        m_publisher.publish("/topic", "Hello world MQTT!!-1".getBytes(), 1, false);
//
//        // reconnect and expect to receive the hello 2 message
//        m_client.connect(options);
//        m_client.subscribe("/topic", 1);
//        MqttMessage msg = messagesCollector.waitMessage(1);
//        assertNotNull(msg);
//        assertEquals("Hello world MQTT!!-1", new String(msg.getPayload()));
//    }

//    /**
//     * subscriber connect and subscribe on "topic" subscriber disconnects publisher connects and
//     * send two message "hello1" "hello2" to "topic" subscriber connects again and receive "hello1"
//     * "hello2"
//     */
//    @Test
//    public void checkQoS2SubscriberDisconnectReceivePersistedPublishes() throws Exception {
//        LOG.info("*** checkQoS2SubscriberDisconnectReceivePersistedPublishes ***");
//        MessageCollector messagesCollector = new MessageCollector();
//        m_client.setCallback(messagesCollector);
//        MqttConnectOptions options = new MqttConnectOptions();
//        options.setCleanSession(false);
//        m_client.connect(options);
//        m_client.subscribe("topic", 2);
//        m_client.disconnect();
//
//        m_publisher.connect();
//        m_publisher.publish("topic", "Hello1".getBytes(), 2, true);
//        m_publisher.publish("topic", "Hello2".getBytes(), 2, true);
//        m_publisher.disconnect();
//
//        // subscriber reconnects
//        m_client.connect(options);
//        // subscriber should receive the 2 messages missed
//        MqttMessage msg = messagesCollector.waitMessage(1);
//        assertNotNull(msg);
//        assertEquals("Hello1", new String(msg.getPayload()));
//        msg = messagesCollector.waitMessage(1);
//        assertNotNull(msg);
//        assertEquals("Hello2", new String(msg.getPayload()));
//    }
//
//    /**
//     * subscriber connect and subscribe on "a/b" QoS 1 and "a/+" QoS 2 publisher connects and send a
//     * message "hello" on "a/b" subscriber must receive only a single message not twice
//     */
//    @Test
//    public void checkSinglePublishOnOverlappingSubscriptions() throws Exception {
//        LOG.info("*** checkSinglePublishOnOverlappingSubscriptions ***");
//        MessageCollector messagesCollector = new MessageCollector();
//        m_client.setCallback(messagesCollector);
//        m_publisher.connect();
//
//        MqttConnectOptions options = new MqttConnectOptions();
//        options.setCleanSession(false);
//        m_client.connect(options);
//        m_client.subscribe("a/+", 2);
//        m_client.subscribe("a/b", 1);
//
//        // force the publisher to send
//        m_publisher.publish("a/b", "Hello world MQTT!!".getBytes(), 1, false);
//
//        // reconnect and expect to receive the hello 2 message
//        MqttMessage msg = messagesCollector.waitMessage(1);
//        assertEquals("Hello world MQTT!!", new String(msg.getPayload()));
//
//        // try to listen a second publish
//        msg = messagesCollector.waitMessage(1);
//        assertNull(msg);
//    }

    protected MqttClient createClient(String clientName, String storeSuffix) throws MqttException {
        return createClient(clientName, storeSuffix, null);
    }

    protected MqttClient createClient(String clientName, String storeSuffix, MessageCollector cb) throws MqttException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        // clientX connect and subscribe to /topic QoS2
        MqttClientPersistence dsClient = new MqttDefaultFilePersistence(
                tmpDir + File.separator + storeSuffix + clientName);
        MqttClient client = new MqttClient("tcp://localhost:1883", clientName, dsClient);
        if (cb != null) {
            client.setCallback(cb);
        }
        client.connect();
        return client;
    }

}
