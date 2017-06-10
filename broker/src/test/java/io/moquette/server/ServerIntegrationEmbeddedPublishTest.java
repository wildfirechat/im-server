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
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import static org.junit.Assert.*;

public class ServerIntegrationEmbeddedPublishTest {

    private static final Logger LOG = LoggerFactory.getLogger(ServerIntegrationEmbeddedPublishTest.class);

    static MqttClientPersistence s_dataStore;
    static MqttClientPersistence s_pubDataStore;

    Server m_server;
    IMqttClient m_subscriber;
    MessageCollector m_callback;
    IConfig m_config;

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
    }

    @Before
    public void setUp() throws Exception {
        startServer();

        m_subscriber = new MqttClient("tcp://localhost:1883", "TestClient", s_dataStore);
        m_callback = new MessageCollector();
        m_subscriber.setCallback(m_callback);
    }

    @After
    public void tearDown() throws Exception {
        if (m_subscriber != null && m_subscriber.isConnected()) {
            m_subscriber.disconnect();
        }

        m_server.stopServer();
    }

    private void subscribeToWithQos(String topic, int qos) throws Exception {
        m_subscriber.connect();
        m_subscriber.subscribe(topic, qos);
    }

    private void internalPublishToWithQosAndRetained(String topic, MqttQoS qos, boolean retained) {
        MqttPublishMessage message = MqttMessageBuilders.publish().topicName(topic).retained(retained).qos(qos)
                .payload(Unpooled.copiedBuffer("Hello world MQTT!!".getBytes())).build();
        m_server.internalPublish(message, "INTRPUBL");
    }

    private void connectNoCleanSession() throws Exception {
        MqttConnectOptions opts = new MqttConnectOptions();
        opts.setCleanSession(false);
        m_subscriber.connect(opts);
    }

    private void subscribeToWithQosAndNoCleanSession(String topic, int qos) throws Exception {
        connectNoCleanSession();
        m_subscriber.subscribe(topic, qos);
    }

    private void verifyNoMessageIsReceived() throws Exception {
        MqttMessage msg = m_callback.getMessageImmediate();
        assertNull(msg);
    }

    private void verifyMessageIsReceivedSuccessfully() throws Exception {
        // check in 2 seconds
        MqttMessage msg = m_callback.waitMessage(2);
        assertNotNull(msg);
        assertEquals("Hello world MQTT!!", new String(msg.getPayload()));
    }

    @Test
    public void testClientSubscribeAfterNotRetainedQoS0IsSent() throws Exception {
        LOG.info("*** testClientSubscribeAfterNotRetainedQoS0IsSent ***");

        // Exercise
        internalPublishToWithQosAndRetained("/topic", MqttQoS.AT_MOST_ONCE, false);
        subscribeToWithQos("/topic", 0);

        // Verify
        verifyNoMessageIsReceived();
    }

    @Test
    public void testClientSubscribeBeforeNotRetainedQoS0IsSent() throws Exception {
        LOG.info("*** testClientSubscribeBeforeNotRetainedQoS0 ***");

        subscribeToWithQos("/topic", 0);

        // Exercise
        internalPublishToWithQosAndRetained("/topic", MqttQoS.AT_MOST_ONCE, false);

        // Verify
        verifyMessageIsReceivedSuccessfully();
    }

    @Test
    public void testClientSubscribeBeforeRetainedQoS0IsSent() throws Exception {
        LOG.info("*** testClientSubscribeBeforeRetainedQoS0IsSent ***");

        subscribeToWithQos("/topic", 0);

        // Exercise
        internalPublishToWithQosAndRetained("/topic", MqttQoS.AT_MOST_ONCE, true);

        // Verify
        verifyMessageIsReceivedSuccessfully();
    }

    @Test
    public void testClientSubscribeAfterRetainedQoS0IsSent() throws Exception {
        LOG.info("*** testClientSubscribeAfterRetainedQoS0IsSent ***");

        // Exercise
        internalPublishToWithQosAndRetained("/topic", MqttQoS.AT_MOST_ONCE, true);
        // LOG.info("** post internalPublish **");
        subscribeToWithQos("/topic", 0);

        // Verify
        verifyNoMessageIsReceived();
    }

    @Test
    public void testClientSubscribeBeforeNotRetainedQoS1IsSent() throws Exception {
        LOG.info("*** testClientSubscribeBeforeNotRetainedQoS1IsSent ***");

        subscribeToWithQos("/topic", 1);

        // Exercise
        internalPublishToWithQosAndRetained("/topic", MqttQoS.AT_LEAST_ONCE, false);

        // Verify
        verifyMessageIsReceivedSuccessfully();
    }

    @Test
    public void testClientSubscribeAfterNotRetainedQoS1IsSent() throws Exception {
        LOG.info("*** testClientSubscribeAfterNotRetainedQoS1IsSent ***");

        // Exercise
        internalPublishToWithQosAndRetained("/topic", MqttQoS.AT_LEAST_ONCE, false);
        subscribeToWithQos("/topic", 1);

        // Verify
        verifyNoMessageIsReceived();
    }

    @Test
    public void testClientSubscribeBeforeRetainedQoS1IsSent() throws Exception {
        LOG.info("*** testClientSubscribeBeforeRetainedQoS1IsSent ***");

        subscribeToWithQos("/topic", 1);

        // Exercise
        internalPublishToWithQosAndRetained("/topic", MqttQoS.AT_LEAST_ONCE, true);

        // Verify
        verifyMessageIsReceivedSuccessfully();
    }

    @Test
    public void testClientSubscribeAfterRetainedQoS1IsSent() throws Exception {
        LOG.info("*** testClientSubscribeAfterRetainedQoS0IsSent ***");

        // Exercise
        internalPublishToWithQosAndRetained("/topic", MqttQoS.AT_LEAST_ONCE, true);
        subscribeToWithQos("/topic", MqttQoS.AT_LEAST_ONCE.value());
        LOG.info("** After subscribe **");

        // Verify
        verifyMessageIsReceivedSuccessfully();
        LOG.info("** Post verify **");
    }

    @Test
    public void testClientSubscribeBeforeNotRetainedQoS2IsSent() throws Exception {
        LOG.info("*** testClientSubscribeBeforeNotRetainedQoS2IsSent ***");

        subscribeToWithQos("/topic", 2);

        // Exercise
        internalPublishToWithQosAndRetained("/topic", MqttQoS.EXACTLY_ONCE, false);

        // Verify
        verifyMessageIsReceivedSuccessfully();
    }

    @Test
    public void testClientSubscribeAfterNotRetainedQoS2IsSent() throws Exception {
        LOG.info("*** testClientSubscribeAfterNotRetainedQoS2IsSent ***");

        // Exercise
        internalPublishToWithQosAndRetained("/topic", MqttQoS.EXACTLY_ONCE, false);
        subscribeToWithQos("/topic", 2);

        // Verify
        verifyNoMessageIsReceived();
    }

    @Test
    public void testClientSubscribeBeforeRetainedQoS2IsSent() throws Exception {
        LOG.info("*** testClientSubscribeBeforeRetainedQoS2IsSent ***");

        subscribeToWithQos("/topic", 2);

        // Exercise
        internalPublishToWithQosAndRetained("/topic", MqttQoS.EXACTLY_ONCE, true);

        // Verify
        verifyMessageIsReceivedSuccessfully();
    }

    @Test
    public void testClientSubscribeAfterRetainedQoS2IsSent() throws Exception {
        LOG.info("*** testClientSubscribeAfterRetainedQoS2IsSent ***");

        // Exercise
        internalPublishToWithQosAndRetained("/topic", MqttQoS.EXACTLY_ONCE, true);
        subscribeToWithQos("/topic", 2);

        // Verify
        verifyMessageIsReceivedSuccessfully();
    }

    @Test
    public void testClientSubscribeAfterDisconnected() throws Exception {
        LOG.info("*** testClientSubscribeAfterDisconnected ***");

        subscribeToWithQos("foo", 0);
        m_subscriber.disconnect();

        internalPublishToWithQosAndRetained("foo", MqttQoS.AT_MOST_ONCE, false);

        // verifyMessageIsReceivedSuccessfully();
        // m_subscriber.receive(2, TimeUnit.MILLISECONDS);
        MqttMessage message = m_callback.waitMessage(1);
        assertTrue(message == null);
    }

    @Test
    public void testClientSubscribeWithoutCleanSession() throws Exception {
        LOG.info("*** testClientSubscribeWithoutCleanSession ***");
        subscribeToWithQosAndNoCleanSession("foo", 1);
        m_subscriber.disconnect();
        assertTrue(m_server.getSubscriptions().size() == 1);
        connectNoCleanSession();
        assertTrue(m_server.getSubscriptions().size() == 1);
        internalPublishToWithQosAndRetained("foo", MqttQoS.AT_MOST_ONCE, false);
        verifyMessageIsReceivedSuccessfully();
    }
}
