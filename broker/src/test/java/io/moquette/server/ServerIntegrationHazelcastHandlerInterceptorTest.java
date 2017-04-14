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

import io.moquette.interception.HazelcastInterceptHandler;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.MemoryConfig;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Properties;

import static io.moquette.BrokerConstants.*;
import static org.junit.Assert.assertEquals;

public class ServerIntegrationHazelcastHandlerInterceptorTest {

    private static final Logger LOG = LoggerFactory.getLogger(ServerIntegrationHazelcastHandlerInterceptorTest.class);

    static MqttClientPersistence s_dataStore;
    static MqttClientPersistence s_pubDataStore;

    Server server1883;
    Server server1884;
    IMqttClient m_listener;
    IMqttClient m_publisher;
    MessageCollector m_messagesCollector;
    IConfig m_config_1883;
    IConfig m_config_1884;

    @BeforeClass
    public static void beforeTests() throws NoSuchAlgorithmException, SQLException, ClassNotFoundException {
        String tmpDir = System.getProperty("java.io.tmpdir");
        s_dataStore = new MqttDefaultFilePersistence(tmpDir);
        s_pubDataStore = new MqttDefaultFilePersistence(tmpDir + File.separator + "publisher");
    }

    protected Server startServer(int port, IConfig m_config) throws IOException {
        Server m_server = new Server();
        m_config.setProperty(PORT_PROPERTY_NAME, Integer.toString(port));
        m_server.startServer(m_config);
        return m_server;
    }

    private void stopServer(Server m_server) {
        m_server.stopServer();
    }

    private Properties addHazelCastConf(Properties properties, int port, String hazelcastConfigurationFile) {
        properties.put(PORT_PROPERTY_NAME, port);
        properties.put(INTERCEPT_HANDLER_PROPERTY_NAME, HazelcastInterceptHandler.class.getCanonicalName());
        properties.put(HAZELCAST_CONFIGURATION, hazelcastConfigurationFile);
        return properties;
    }

    @Before
    public void setUp() throws Exception {
        final Properties configProps = addHazelCastConf(
                IntegrationUtils.prepareTestClusterProperties(1883),
                1883,
                "config/hazelcast.xml");
        m_config_1883 = new MemoryConfig(configProps);

        server1883 = startServer(1883, m_config_1883);

        final Properties configProps1884 = addHazelCastConf(
                IntegrationUtils.prepareTestClusterProperties(1884),
                1883,
                "config/hazelcast.xml");
        m_config_1884 = new MemoryConfig(configProps1884);
        server1884 = startServer(1884, m_config_1884);

        m_publisher = new MqttClient("tcp://localhost:1883", "Publisher", s_pubDataStore);
        m_publisher.connect();
        m_listener = new MqttClient("tcp://localhost:1884", "Listener", s_dataStore);
        m_messagesCollector = new MessageCollector();
        m_listener.setCallback(m_messagesCollector);

        m_listener.connect();
    }

    @After
    public void tearDown() throws Exception {
        if (m_listener != null && m_listener.isConnected()) {
            m_listener.disconnect();
        }

        if (m_publisher != null && m_publisher.isConnected()) {
            m_publisher.disconnect();
        }

        stopServer(server1883);
        stopServer(server1884);
    }

    @Test
    public void checkPublishPassThroughCluster_Qos0() throws Exception {
        LOG.info("*** checkPublishPassThroughCluster_Qos0 ***");
        m_listener.subscribe("/topic", 1);

        m_publisher.publish("/topic", "Hello world MQTT QoS0".getBytes(), 0, false);
        MqttMessage messageQos0 = m_messagesCollector.waitMessage(1);
        assertEquals("Hello world MQTT QoS0", messageQos0.toString());
        assertEquals(0, messageQos0.getQos());
    }

    @Test
    public void checkPublishPassThroughCluster_Qos1() throws Exception {
        LOG.info("*** checkPublishPassThroughCluster_Qos1 ***");
        m_listener.subscribe("/topic", 1);

        m_publisher.publish("/topic", "Hello world MQTT QoS1".getBytes(), 1, false);
        MqttMessage messageQos1 = m_messagesCollector.waitMessage(1);
        assertEquals("Hello world MQTT QoS1", messageQos1.toString());
        assertEquals(1, messageQos1.getQos());
    }

    @Test
    public void checkPublishPassThroughCluster_Qos2() throws Exception {
        LOG.info("*** checkPublishPassThroughCluster_Qos2 ***");
        m_listener.subscribe("/topic", 2);

        m_publisher.publish("/topic", "Hello world MQTT QoS2".getBytes(), 2, false);
        MqttMessage messageQos2 = m_messagesCollector.waitMessage(1);
        assertEquals("Hello world MQTT QoS2", messageQos2.toString());
        assertEquals(2, messageQos2.getQos());
    }
}
