/*
 * Copyright (c) 2012-2017 The original author or authors
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
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ServerRestartIntegrationTest {

    static MqttClientPersistence s_dataStore;
    static MqttClientPersistence s_pubDataStore;
    static MqttConnectOptions CLEAN_SESSION_OPT = new MqttConnectOptions();

    Server m_server;
    IMqttClient m_subscriber;
    IMqttClient m_publisher;
    IConfig m_config;
    MessageCollector m_messageCollector;

    protected void startServer() throws IOException {
        m_server = new Server();
        final Properties configProps = IntegrationUtils.prepareTestProperties();
        m_config = new MemoryConfig(configProps);
        m_server.startServer(m_config);
    }

    @BeforeClass
    public static void beforeTests() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        s_dataStore = new MqttDefaultFilePersistence(tmpDir);
        s_pubDataStore = new MqttDefaultFilePersistence(tmpDir + File.separator + "publisher");
        CLEAN_SESSION_OPT.setCleanSession(false);
    }

    @Before
    public void setUp() throws Exception {
        startServer();

        m_subscriber = new MqttClient("tcp://localhost:1883", "Subscriber", s_dataStore);
        m_messageCollector = new MessageCollector();
        m_subscriber.setCallback(m_messageCollector);

        m_publisher = new MqttClient("tcp://localhost:1883", "Publisher", s_pubDataStore);
    }

    @After
    public void tearDown() throws Exception {
        if (m_subscriber != null && m_subscriber.isConnected()) {
            m_subscriber.disconnect();
        }

        if (m_publisher != null && m_publisher.isConnected()) {
            m_publisher.disconnect();
        }

        m_server.stopServer();
    }

    @Test
    public void checkRestartCleanSubscriptionTree() throws Exception {
        // subscribe to /topic
        m_subscriber.connect(CLEAN_SESSION_OPT);
        m_subscriber.subscribe("/topic", 1);
        m_subscriber.disconnect();

        // shutdown the server
        m_server.stopServer();

        // restart the server
        m_server.startServer(IntegrationUtils.prepareTestProperties());

        // reconnect the Subscriber subscribing to the same /topic but different QoS
        m_subscriber.connect(CLEAN_SESSION_OPT);
        m_subscriber.subscribe("/topic", 2);

        // should be just one registration so a publisher receive one notification
        m_publisher.connect(CLEAN_SESSION_OPT);
        m_publisher.publish("/topic", "Hello world MQTT!!".getBytes(), 1, false);

        // read the messages
        MqttMessage msg = m_messageCollector.waitMessage(1);
        assertEquals("Hello world MQTT!!", new String(msg.getPayload()));
        // no more messages on the same topic will be received
        assertNull(m_messageCollector.waitMessage(1));
    }

    @Test
    public void checkDontPublishInactiveClientsAfterServerRestart() throws Exception {
        IMqttClient conn = subscribeAndPublish("/topic");
        conn.disconnect();

        // shutdown the server
        m_server.stopServer();

        // restart the server
        m_server.startServer(IntegrationUtils.prepareTestProperties());

        m_publisher.connect();
        m_publisher.publish("/topic", "Hello world MQTT!!".getBytes(), 0, false);
    }

    @Test
    public void testClientDoesntRemainSubscribedAfterASubscriptionAndServerRestart() throws Exception {
        // subscribe to /topic
        m_subscriber.connect();
        // subscribe /topic
        m_subscriber.subscribe("/topic", 0);
        // unsubscribe from /topic
        m_subscriber.unsubscribe("/topic");
        m_subscriber.disconnect();

        // shutdown the server
        m_server.stopServer();

        // restart the server
        m_server.startServer(IntegrationUtils.prepareTestProperties());
        // subscriber reconnects
        m_subscriber = new MqttClient("tcp://localhost:1883", "Subscriber", s_dataStore);
        m_subscriber.setCallback(m_messageCollector);
        m_subscriber.connect();

        // publisher publishes on /topic
        m_publisher = new MqttClient("tcp://localhost:1883", "Publisher", s_pubDataStore);
        m_publisher.connect();
        m_publisher.publish("/topic", "Hello world MQTT!!".getBytes(), 1, false);

        // Expected
        // the subscriber doesn't get notified (it's fully unsubscribed)
        assertNull(m_messageCollector.waitMessage(1));
    }

    /**
     * Connect subscribe to topic and publish on the same topic
     */
    private IMqttClient subscribeAndPublish(String topic) throws Exception {
        IMqttClient client = new MqttClient("tcp://localhost:1883", "SubPub");
        MessageCollector collector = new MessageCollector();
        client.setCallback(collector);
        client.connect();
        client.subscribe(topic, 1);
        client.publish(topic, "Hello world MQTT!!".getBytes(), 0, false);
        MqttMessage msg = collector.waitMessage(1);
        assertEquals("Hello world MQTT!!", new String(msg.getPayload()));
        return client;
    }
}
