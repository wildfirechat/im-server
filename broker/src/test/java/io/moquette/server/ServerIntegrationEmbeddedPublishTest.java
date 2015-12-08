/*
 * Copyright (c) 2012-2015 The original author or authors
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

import io.moquette.proto.messages.AbstractMessage.QOSType;
import io.moquette.proto.messages.PublishMessage;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.MemoryConfig;
import org.fusesource.mqtt.client.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * Created by andrea on 08/12/15.
 */
public class ServerIntegrationEmbeddedPublishTest {

    private static final Logger LOG = LoggerFactory.getLogger(ServerIntegrationEmbeddedPublishTest.class);

    Server m_server;
    MQTT m_mqtt;
    BlockingConnection m_subscriber;
    IConfig m_config;

    protected void startServer() throws IOException {
        m_server = new Server();
        final Properties configProps = IntegrationUtils.prepareTestPropeties();
        m_config = new MemoryConfig(configProps);
        m_server.startServer(m_config);
    }

    @Before
    public void setUp() throws Exception {
        startServer();

        m_mqtt = new MQTT();
        m_mqtt.setHost("localhost", 1883);
        m_mqtt.setClientId("Subscriber");
    }

    @After
    public void tearDown() throws Exception {
        if (m_subscriber != null) {
            m_subscriber.disconnect();
        }

        m_server.stopServer();
        IntegrationUtils.cleanPersistenceFile(m_config);
    }

    private void subscribeToWithQos(String topic, QoS qos) throws Exception {
        m_subscriber = m_mqtt.blockingConnection();
        m_subscriber.connect();
        Topic[] topics = new Topic[]{new Topic(topic, qos)};
        m_subscriber.subscribe(topics);
    }

    private void internalPublishToWithQosAndRetained(String topic, QOSType qos, boolean retained) {
        PublishMessage message = new PublishMessage();
        message.setTopicName(topic);
        message.setRetainFlag(retained);
        message.setQos(qos);
        message.setPayload(ByteBuffer.wrap("Hello world MQTT!!".getBytes()));
        m_server.internalPublish(message);
    }

    private void verifyNoMessageIsReceived() throws Exception {
        Message msg = m_subscriber.receive(1, TimeUnit.SECONDS);
        assertNull(msg);
    }

    private void verifyMessageIsReceivedSuccessfully() throws Exception {
        Message msg = m_subscriber.receive(2, TimeUnit.SECONDS);
        msg.ack();
        assertNotNull(msg);
        assertEquals("Hello world MQTT!!", new String(msg.getPayload()));
    }

    @Test
    public void testClientSubscribeAfterNotRetainedQoS0IsSent() throws Exception {
        LOG.info("*** testClientSubscribeAfterNotRetainedQoS0IsSent ***");

        //Exercise
        internalPublishToWithQosAndRetained("/topic", QOSType.MOST_ONE, false);
        subscribeToWithQos("/topic", QoS.AT_MOST_ONCE);

        //Verify
        verifyNoMessageIsReceived();
    }

    @Test
    public void testClientSubscribeBeforeNotRetainedQoS0IsSent() throws Exception {
        LOG.info("*** testClientSubscribeBeforeNotRetainedQoS0 ***");

        subscribeToWithQos("/topic", QoS.AT_MOST_ONCE);

        //Exercise
        internalPublishToWithQosAndRetained("/topic", QOSType.MOST_ONE, false);

        //Verify
        verifyMessageIsReceivedSuccessfully();
    }

    @Test
    public void testClientSubscribeBeforeRetainedQoS0IsSent() throws Exception {
        LOG.info("*** testClientSubscribeBeforeRetainedQoS0IsSent ***");

        subscribeToWithQos("/topic", QoS.AT_MOST_ONCE);
        //super ugly but we need the MQTT client lib finish it's job before us
        Thread.sleep(1000);

        //Exercise
        internalPublishToWithQosAndRetained("/topic", QOSType.MOST_ONE, true);

        //Verify
        verifyMessageIsReceivedSuccessfully();
    }

    @Test
    public void testClientSubscribeAfterRetainedQoS0IsSent() throws Exception {
        LOG.info("*** testClientSubscribeAfterRetainedQoS0IsSent ***");

        //Exercise
        internalPublishToWithQosAndRetained("/topic", QOSType.MOST_ONE, true);
        subscribeToWithQos("/topic", QoS.AT_MOST_ONCE);

        //Verify
        verifyNoMessageIsReceived();
    }

    @Test
    public void testClientSubscribeBeforeNotRetainedQoS1IsSent() throws Exception {
        LOG.info("*** testClientSubscribeBeforeNotRetainedQoS1IsSent ***");

        subscribeToWithQos("/topic", QoS.AT_LEAST_ONCE);
        //super ugly but we need the MQTT client lib finish it's job before us
        Thread.sleep(1000);

        //Exercise
        internalPublishToWithQosAndRetained("/topic", QOSType.LEAST_ONE, false);

        //Verify
        verifyMessageIsReceivedSuccessfully();
    }

    @Test
    public void testClientSubscribeAfterNotRetainedQoS1IsSent() throws Exception {
        LOG.info("*** testClientSubscribeAfterNotRetainedQoS1IsSent ***");

        //Exercise
        internalPublishToWithQosAndRetained("/topic", QOSType.LEAST_ONE, false);
        subscribeToWithQos("/topic", QoS.AT_LEAST_ONCE);

        //Verify
        verifyNoMessageIsReceived();
    }

    @Test
    public void testClientSubscribeBeforeRetainedQoS1IsSent() throws Exception {
        LOG.info("*** testClientSubscribeBeforeRetainedQoS1IsSent ***");

        subscribeToWithQos("/topic", QoS.AT_LEAST_ONCE);
        //super ugly but we need the MQTT client lib finish it's job before us
        Thread.sleep(1000);

        //Exercise
        internalPublishToWithQosAndRetained("/topic", QOSType.LEAST_ONE, true);

        //Verify
        verifyMessageIsReceivedSuccessfully();
    }

    @Test
    public void testClientSubscribeAfterRetainedQoS1IsSent() throws Exception {
        LOG.info("*** testClientSubscribeAfterRetainedQoS0IsSent ***");

        //Exercise
        internalPublishToWithQosAndRetained("/topic", QOSType.LEAST_ONE, true);
        subscribeToWithQos("/topic", QoS.AT_LEAST_ONCE);

        //Verify
        verifyMessageIsReceivedSuccessfully();
    }

    @Test
    public void testClientSubscribeBeforeNotRetainedQoS2IsSent() throws Exception {
        LOG.info("*** testClientSubscribeBeforeNotRetainedQoS2IsSent ***");

        subscribeToWithQos("/topic", QoS.EXACTLY_ONCE);
        //super ugly but we need the MQTT client lib finish it's job before us
        Thread.sleep(1000);

        //Exercise
        internalPublishToWithQosAndRetained("/topic", QOSType.EXACTLY_ONCE, false);

        //Verify
        verifyMessageIsReceivedSuccessfully();
    }

    @Test
    public void testClientSubscribeAfterNotRetainedQoS2IsSent() throws Exception {
        LOG.info("*** testClientSubscribeAfterNotRetainedQoS2IsSent ***");

        //Exercise
        internalPublishToWithQosAndRetained("/topic", QOSType.EXACTLY_ONCE, false);
        subscribeToWithQos("/topic", QoS.EXACTLY_ONCE);

        //Verify
        verifyNoMessageIsReceived();
    }

    @Test
    public void testClientSubscribeBeforeRetainedQoS2IsSent() throws Exception {
        LOG.info("*** testClientSubscribeBeforeRetainedQoS2IsSent ***");

        subscribeToWithQos("/topic", QoS.EXACTLY_ONCE);
        //super ugly but we need the MQTT client lib finish it's job before us
        Thread.sleep(1000);

        //Exercise
        internalPublishToWithQosAndRetained("/topic", QOSType.EXACTLY_ONCE, true);

        //Verify
        verifyMessageIsReceivedSuccessfully();
    }

    @Test
    public void testClientSubscribeAfterRetainedQoS2IsSent() throws Exception {
        LOG.info("*** testClientSubscribeAfterRetainedQoS2IsSent ***");

        //Exercise
        internalPublishToWithQosAndRetained("/topic", QOSType.EXACTLY_ONCE, true);
        subscribeToWithQos("/topic", QoS.EXACTLY_ONCE);

        //Verify
        verifyMessageIsReceivedSuccessfully();
    }
}
