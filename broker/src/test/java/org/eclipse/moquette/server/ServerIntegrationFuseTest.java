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
import java.util.concurrent.TimeUnit;
import org.fusesource.mqtt.client.BlockingConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.Message;
import org.fusesource.mqtt.client.QoS;
import org.fusesource.mqtt.client.Topic;
import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
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
    public void connectWithCredentials() throws Exception {
        LOG.info("*** connectWithCredetials ***");
        MQTT mqtt = new MQTT();
        mqtt.setHost("localhost", 1883); 
        mqtt.setClientId("Publisher");
        mqtt.setUserName("testuser");
        mqtt.setPassword("passwd");
        
        m_publisher = mqtt.blockingConnection();
        m_publisher.connect();
        
        assertTrue(true);
    }
    
    @Test
    public void checkWillTestmaentIsPublishedOnConnectionKill_noRetain() throws Exception {
        LOG.info("checkWillTestmaentIsPublishedOnConnectionKill");
        
        String willTestamentTopic = "/will/test";
        String willTestamentMsg = "Bye bye";
        
        MQTT mqtt = new MQTT();
        mqtt.setHost("localhost", 1883); 
        mqtt.setClientId("WillTestamentPublisher");
        mqtt.setWillRetain(false);
        mqtt.setWillMessage(willTestamentMsg);
        mqtt.setWillTopic(willTestamentTopic);
        m_publisher = mqtt.blockingConnection();
        m_publisher.connect();
        
        m_mqtt.setHost("localhost", 1883); 
        m_mqtt.setCleanSession(false);
        m_mqtt.setClientId("Subscriber");
        m_subscriber = m_mqtt.blockingConnection();
        m_subscriber.connect();
//        Topic[] topics = new Topic[]{new Topic(willTestamentTopic, QoS.AT_LEAST_ONCE)};
        Topic[] topics = new Topic[]{new Topic(willTestamentTopic, QoS.AT_MOST_ONCE)};
        m_subscriber.subscribe(topics);
        
        //Exercise, kill the publisher connection
        m_publisher.kill();
        
        //Verify, that the testament is fired
        Message msg = m_subscriber.receive(500, TimeUnit.MILLISECONDS);
        assertNotNull("We should get notified with 'Will' message", msg);
        msg.ack();
        assertEquals(willTestamentMsg, new String(msg.getPayload()));
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
    
    /**
     * subscriber connect and subscribe on "topic"
     * subscriber disconnects
     * publisher connects and send two message "hello1" "hello2" to "topic"
     * subscriber connects again and receive "hello1" "hello2"
     */
    @Test
    public void checkQoS2SuscriberDisconnectReceivePersistedPublishes() throws Exception {
        LOG.info("*** checkQoS2SuscriberDisconnectReceivePersistedPublishes ***");
        m_mqtt.setHost("localhost", 1883); 
        m_mqtt.setCleanSession(false);
        m_mqtt.setClientId("Subscriber");
        m_subscriber = m_mqtt.blockingConnection();
        m_subscriber.connect();
        Topic[] topics = new Topic[]{new Topic("topic", QoS.EXACTLY_ONCE)};
        m_subscriber.subscribe(topics);
        m_subscriber.disconnect();
        System.out.println("***Subscriber listen to topic and disconnects");
        
        MQTT mqtt = new MQTT();
        mqtt.setHost("localhost", 1883); 
        mqtt.setClientId("Publisher");
        m_publisher = mqtt.blockingConnection();
        m_publisher.connect();
        m_publisher.publish("topic", "Hello1".getBytes(), QoS.EXACTLY_ONCE, true);
        m_publisher.publish("topic", "Hello2".getBytes(), QoS.EXACTLY_ONCE, true);
        m_publisher.disconnect();
        System.out.println("***Publisher published 2 messges to topic and disconnects");
        
        //subscriber reconnects
        m_mqtt = new MQTT();
        m_mqtt.setHost("localhost", 1883); 
        m_mqtt.setClientId("Subscriber");
        m_mqtt.setCleanSession(false);
        m_subscriber = m_mqtt.blockingConnection();
        System.out.println("***Subscriber reconnects");
        m_subscriber.connect();
        //subscriber should receive the 2 messages missed
        Message msg = m_subscriber.receive(1, TimeUnit.SECONDS);
        assertNotNull(msg);
        msg.ack();
        assertEquals("Hello1", new String(msg.getPayload()));
        msg = m_subscriber.receive(1, TimeUnit.SECONDS);
        assertNotNull(msg);
        msg.ack();
        assertEquals("Hello2", new String(msg.getPayload()));
    }
}
