/*
 * Copyright (c) 2012-2014 The original author or authors
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
package org.eclipse.moquette.server;


import java.io.File;
import java.io.IOException;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
public class ServerIntegrationQoSValidationTest {
    
    private static final Logger LOG = LoggerFactory.getLogger(ServerIntegrationPahoTest.class);

    Server m_server;
    static MqttClientPersistence s_subDataStore;
    static MqttClientPersistence s_pubDataStore;
    
    IMqttClient m_subscriber;
    IMqttClient m_publisher;
    TestCallback m_callback;

    @BeforeClass
    public static void beforeTests() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        s_subDataStore = new MqttDefaultFilePersistence(tmpDir + File.separator + "subscriber");
        s_pubDataStore = new MqttDefaultFilePersistence(tmpDir + File.separator + "publisher");
    }

    protected void startServer() throws IOException {
        m_server = new Server();
        m_server.startServer();
    }

    @Before
    public void setUp() throws Exception {
        startServer();

        m_subscriber = new MqttClient("tcp://localhost:1883", "Subscriber", s_subDataStore);
        m_callback = new TestCallback();
        m_subscriber.setCallback(m_callback);
        m_subscriber.connect();
        
        m_publisher = new MqttClient("tcp://localhost:1883", "Publisher", s_pubDataStore);
//        m_callback = new TestCallback();
//        m_subscriber.setCallback(m_callback);
        m_publisher.connect();
    }

    @After
    public void tearDown() throws Exception {
        if (m_publisher.isConnected()) {
            m_publisher.disconnect();
        }
        
        if (m_subscriber.isConnected()) {
            m_subscriber.disconnect();
        }

        m_server.stopServer();
        File dbFile = new File(Server.STORAGE_FILE_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
    }
    
    @Test
    public void checkSubscriberQoS0ReceiveQoS0publishes() throws Exception {
        LOG.info("*** checkSubscriberQoS0ReceiveQoS0publishes ***");
        m_subscriber.subscribe("/topic", 0);
        
        m_publisher.publish("/topic", "Hello world MQTT QoS0".getBytes(), 0, false);
        MqttMessage message = m_callback.getMessage(true);
        assertEquals("Hello world MQTT QoS0", message.toString());
        assertEquals(0, message.getQos());
    }
    
    @Test
    public void checkSubscriberQoS0ReceiveQoS1publishes_downgrade() throws Exception {
        LOG.info("*** checkSubscriberQoS0ReceiveQoS1publishes_downgrade ***");
        m_subscriber.subscribe("/topic", 0);
        
        m_publisher.publish("/topic", "Hello world MQTT QoS1".getBytes(), 1, false);
        MqttMessage message = m_callback.getMessage(true);
        assertEquals("Hello world MQTT QoS1", message.toString());
        assertEquals(0, message.getQos());
    }
    
    @Test
    public void checkSubscriberQoS0ReceiveQoS2publishes_downgrade() throws Exception {
        LOG.info("*** checkSubscriberQoS0ReceiveQoS2publishes_downgrade ***");
        m_subscriber.subscribe("/topic", 0);
        
        m_publisher.publish("/topic", "Hello world MQTT QoS2".getBytes(), 2, false);
        MqttMessage message = m_callback.getMessage(true);
        assertEquals("Hello world MQTT QoS2", message.toString());
        assertEquals(0, message.getQos());
    }
    
    @Test
    public void checkSubscriberQoS1ReceiveQoS0publishes() throws Exception {
        LOG.info("*** checkSubscriberQoS1ReceiveQoS0publishes ***");
        m_subscriber.subscribe("/topic", 1);
        
        m_publisher.publish("/topic", "Hello world MQTT QoS0".getBytes(), 0, false);
        MqttMessage message = m_callback.getMessage(true);
        assertEquals("Hello world MQTT QoS0", message.toString());
        assertEquals(0, message.getQos());
    }
    
    @Test
    public void checkSubscriberQoS1ReceiveQoS1publishes() throws Exception {
        LOG.info("*** checkSubscriberQoS1ReceiveQoS1publishes ***");
        m_subscriber.subscribe("/topic", 1);
        
        m_publisher.publish("/topic", "Hello world MQTT QoS1".getBytes(), 1, false);
        MqttMessage message = m_callback.getMessage(true);
        assertEquals("Hello world MQTT QoS1", message.toString());
        assertEquals(1, message.getQos());
    }
    
    @Test
    public void checkSubscriberQoS1ReceiveQoS2publishes_downgrade() throws Exception {
        LOG.info("*** checkSubscriberQoS1ReceiveQoS2publishes_downgrade ***");
        m_subscriber.subscribe("/topic", 1);
        
        m_publisher.publish("/topic", "Hello world MQTT QoS2".getBytes(), 2, false);
        MqttMessage message = m_callback.getMessage(true);
        assertEquals("Hello world MQTT QoS2", message.toString());
        assertEquals(1, message.getQos());
    }
    
    @Test
    public void checkSubscriberQoS2ReceiveQoS0publishes() throws Exception {
        LOG.info("*** checkSubscriberQoS2ReceiveQoS0publishes ***");
        m_subscriber.subscribe("/topic", 2);
        
        m_publisher.publish("/topic", "Hello world MQTT QoS2".getBytes(), 0, false);
        MqttMessage message = m_callback.getMessage(true);
        assertEquals("Hello world MQTT QoS2", message.toString());
        assertEquals(0, message.getQos());
    }
    
    @Test
    public void checkSubscriberQoS2ReceiveQoS1publishes() throws Exception {
        LOG.info("*** checkSubscriberQoS2ReceiveQoS1publishes ***");
        m_subscriber.subscribe("/topic", 2);
        
        m_publisher.publish("/topic", "Hello world MQTT QoS2".getBytes(), 1, false);
        MqttMessage message = m_callback.getMessage(true);
        assertEquals("Hello world MQTT QoS2", message.toString());
        assertEquals(1, message.getQos());
    }
    
    @Test
    public void checkSubscriberQoS2ReceiveQoS2publishes() throws Exception {
        LOG.info("*** checkSubscriberQoS2ReceiveQoS2publishes ***");
        m_subscriber.subscribe("/topic", 2);
        
        m_publisher.publish("/topic", "Hello world MQTT QoS2".getBytes(), 2, false);
        MqttMessage message = m_callback.getMessage(true);
        assertEquals("Hello world MQTT QoS2", message.toString());
        assertEquals(2, message.getQos());
    }
    
}
