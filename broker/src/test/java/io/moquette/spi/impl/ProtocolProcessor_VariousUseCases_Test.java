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
package io.moquette.spi.impl;

import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static io.netty.handler.codec.mqtt.MqttQoS.*;
import static org.junit.Assert.assertFalse;

public class ProtocolProcessor_VariousUseCases_Test extends AbstractProtocolProcessorCommonUtils {

    @Before
    public void setUp() {
        initializeProcessorAndSubsystems();
    }

//    @Test
//    public void testCleanSession_maintainClientSubscriptions() {
//        connectAsClient(this.m_channel, "TestClient");
//        subscribe("/topic", AT_MOST_ONCE);
//        disconnect();
//
//        // reconnect and publish
//        EmbeddedChannel anotherChannel = new EmbeddedChannel();
//        connectAsClient(anotherChannel, "TestClient");
//
//        publishToAs(anotherChannel, "TestClient", "/topic", AT_MOST_ONCE, false);
//
//        verifyPublishIsReceived(anotherChannel);
//    }
//
//    @Ignore("At the end it's equal to testCleanSession_maintainClientSubscriptions")
//    @Test
//    public void testCleanSession_maintainClientSubscriptions_againstClientDestruction() {
//        connectAsClient("TestClient");
//        subscribe("/topic", AT_MOST_ONCE);
//        disconnect();
//
//        this.m_channel = new EmbeddedChannel();
//        // reconnect and publish
//        connectAsClient("TestClient");
//
//        publishToAs("TestClient", "/topic", AT_MOST_ONCE, false);
//
//        verifyPublishIsReceived();
//    }

//    /**
//     * Check that after a client has connected with clean session false, subscribed to some topic
//     * and exited, if it reconnects with clean session true, the broker correctly cleanup every
//     * previous subscription
//     */
//    @Test
//    public void testCleanSession_correctlyClientSubscriptions() {
//        connectAsClient("TestClient");
//        subscribe("/topic", AT_MOST_ONCE);
//        disconnect();
//
//        this.m_channel = new EmbeddedChannel();
//        // reconnect and publish
//        connectWithCleanSession("TestClient");
//
//        publishToAs("TestClient", "/topic", AT_MOST_ONCE, false);
//
//        verifyNoPublishIsReceived();
//    }

// Duplicated, just verify retain works correctly
//    @Test
//    public void testRetain_maintainMessage_againstClientDestruction() {
//        connect();
//        publishToAs(FAKE_CLIENT_ID, "/topic", AT_LEAST_ONCE, 66, true);
//        disconnect();
//
//        // reconnect and publish
//        this.m_channel = new EmbeddedChannel();
//        connect();
//        subscribe("/topic", AT_MOST_ONCE);
//
//        verifyPublishIsReceived();
//    }

//    @Test
//    public void testUnsubscribe_do_not_notify_anymore_same_session() {
//        connect();
//        subscribe("/topic", AT_MOST_ONCE);
//        publishToAs(FAKE_CLIENT_ID, "/topic", AT_MOST_ONCE, false);
//        verifyPublishIsReceived();
//
//        unsubscribeAndVerifyAck("/topic");
//        publishToAs(FAKE_CLIENT_ID, "/topic", AT_MOST_ONCE, false);
//        verifyNoPublishIsReceived();
//    }

//    @Test
//    public void testUnsubscribe_do_not_notify_anymore_new_session() {
//        connect();
//        subscribe("/topic", AT_MOST_ONCE);
//        publishToAs(FAKE_CLIENT_ID, "/topic", AT_MOST_ONCE, false);
//        verifyPublishIsReceived();
//
//        unsubscribeAndVerifyAck("/topic");
//        disconnect();
//
//        this.m_channel = new EmbeddedChannel();
//        connect();
//        publishToAs(FAKE_CLIENT_ID, "/topic", AT_MOST_ONCE, false);
//        verifyNoPublishIsReceived();
//    }
// Moved to PostOfficePublishTest
//    @Test
//    public void testPublishWithQoS1() {
//        connect();
//        subscribe("/topic", AT_LEAST_ONCE);
//        publishToAs(FAKE_CLIENT_ID, "/topic", AT_LEAST_ONCE, 66, false);
//        disconnect();
//
//        verifyPublishIsReceived(AT_LEAST_ONCE);
//    }

    // Moved to PostOfficePublishTest.forwardQoS1PublishesWhenNotCleanSessionReconnects
//    @Test
//    public void testPublishWithQoS1_notCleanSession() {
//        connect();
//        subscribe("/topic", AT_LEAST_ONCE);
//        disconnect();
//
//        // publish a QoS 1 message another client publish a message on the topic
//        publishFromAnotherClient("/topic", AT_LEAST_ONCE);
//
//        this.m_channel = new EmbeddedChannel();
//        connect();
//        verifyPublishIsReceived();
//    }

//    private void publishFromAnotherClient(String topic, MqttQoS qos) {
//        EmbeddedChannel anotherChannel = new EmbeddedChannel();
//        connectAsClient(anotherChannel, "AnotherClient");
//        publishToAs(anotherChannel, "AnotherClient", topic, qos, 67, false);
//        disconnect(anotherChannel);
//    }

//    private void publishFromAnotherClient(String topic, String payload, MqttQoS qos) {
//        EmbeddedChannel anotherChannel = new EmbeddedChannel();
//        connectAsClient(anotherChannel, "AnotherClient");
//        publishToAs(anotherChannel, "AnotherClient", topic, payload, qos, 68, false);
//        disconnect(anotherChannel);
//    }

//    private void publishQoS2FromAnotherClient(String topic) {
//        EmbeddedChannel anotherChannel = new EmbeddedChannel();
//        connectAsClient(anotherChannel, "AnotherClient");
//        publishQoS2ToAs(anotherChannel, "AnotherClient", topic, 67, false);
//        disconnect(anotherChannel);
//    }

//    @Test
//    public void checkReceivePublishedMessage_after_a_reconnect_with_notCleanSession() {
//        connect();
//        subscribe("/topic", AT_LEAST_ONCE);
//        disconnect();
//
//        this.m_channel = new EmbeddedChannel();
//        connect();
//        subscribe("/topic", AT_LEAST_ONCE);
//
//        // publish a QoS 1 message another client publish a message on the topic
//        publishFromAnotherClient("/topic", AT_LEAST_ONCE);
//
//        // Verify that after a reconnection the client receive the message
//        verifyPublishIsReceived();
//    }

//    @Test
//    public void testPublishWithQoS2() {
//        connect();
//        subscribe("/topic", EXACTLY_ONCE);
//        disconnect();
//
//        publishQoS2FromAnotherClient("/topic");
//
//        this.m_channel = new EmbeddedChannel();
//        connect();
//
//        verifyPublishIsReceived(EXACTLY_ONCE);
//    }
//
//    @Test
//    public void avoidMultipleNotificationsAfterMultipleReconnection_cleanSessionFalseQoS1() {
//        connect();
//        subscribe("/topic", AT_LEAST_ONCE);
//        disconnect();
//
//        publishFromAnotherClient("/topic", "Hello MQTT 1", AT_LEAST_ONCE);
//
//        this.m_channel = new EmbeddedChannel();
//        connect();
//        verifyPublishIsReceived("Hello MQTT 1", AT_LEAST_ONCE);
//        disconnect();
//
//        // publish other message
//        publishFromAnotherClient("/topic", "Hello MQTT 2", AT_LEAST_ONCE);
//
//        this.m_channel = new EmbeddedChannel();
//        connect();
//        verifyPublishIsReceived("Hello MQTT 2", AT_LEAST_ONCE);
//    }

//    @Test
//    public void testConnectSubPub_cycle_getTimeout_on_second_disconnect_issue142() {
//        connect();
//        subscribe("/topic", AT_MOST_ONCE);
//        publishToAs(FAKE_CLIENT_ID, "/topic", AT_MOST_ONCE, true);
//        disconnect();
//
//        // second loop
//        this.m_channel = new EmbeddedChannel();
//        connect();
//        subscribe("/topic", AT_MOST_ONCE);
//        publishToAs(FAKE_CLIENT_ID, "/topic", AT_MOST_ONCE, true);
//        disconnect();
//
//        assertFalse("after a disconnect the client should be disconnected", this.m_channel.isOpen());
//    }
//
//    @Test
//    public void checkReplayofStoredPublishResumeAfter_a_disconnect_cleanSessionFalseQoS1() {
//        EmbeddedChannel publisher = new EmbeddedChannel();
//        connectAsClient(publisher, "Publisher");
//
//        connect();
//        subscribe("/topic", AT_LEAST_ONCE);
//
//        publishToAs(publisher, "Publisher", "/topic", "Hello world MQTT!!-1", AT_LEAST_ONCE, 99, false);
//        verifyPublishIsReceived("Hello world MQTT!!-1", AT_LEAST_ONCE);
//        disconnect();
//
//        publishToAs(publisher, "Publisher", "/topic", "Hello world MQTT!!-2", AT_LEAST_ONCE, 100, false);
//        publishToAs(publisher, "Publisher", "/topic", "Hello world MQTT!!-3", AT_LEAST_ONCE, 101, false);
//
//        this.m_channel = new EmbeddedChannel();
//        connect();
//        verifyPublishIsReceived("Hello world MQTT!!-2", AT_LEAST_ONCE);
//        verifyPublishIsReceived("Hello world MQTT!!-3", AT_LEAST_ONCE);
//    }
//
//    @Test
//    public void checkReplayStoredPublish_forNoCleanSession_qos1() {
//        EmbeddedChannel publisher = new EmbeddedChannel();
//        connectAsClient(publisher, "Publisher");
//
//        connect();
//        subscribe("/topic", AT_LEAST_ONCE);
//        disconnect();
//
//        publishToAs(publisher, "Publisher", "/topic", "Hello world MQTT!!-1", AT_LEAST_ONCE, 99, false);
//
//        this.m_channel = new EmbeddedChannel();
//        connect();
//        subscribeAndNotReadResponse("/topic", AT_LEAST_ONCE);
//        verifyPublishIsReceived("Hello world MQTT!!-1", AT_LEAST_ONCE);
//    }
//
//    /**
//     * subscriber connect and subscribe on "topic" subscriber disconnects publisher connects and
//     * send two message "hello1" "hello2" to "topic" subscriber connects again and receive "hello1"
//     * "hello2"
//     */
//    @Test
//    public void checkQoS2SubscriberDisconnectReceivePersistedPublishes() {
//        connect();
//        subscribe("topic", EXACTLY_ONCE);
//        disconnect();
//
//        EmbeddedChannel publisher = new EmbeddedChannel();
//        connectAsClient(publisher, "Publisher");
//        publishQoS2ToAs(publisher, "Publisher", "topic", "Hello1", 101, true);
//        publishQoS2ToAs(publisher, "Publisher", "topic", "Hello2", 102, true);
//        disconnect(publisher);
//
//        this.m_channel = new EmbeddedChannel();
//        connect();
//        verifyPublishIsReceived("Hello1", EXACTLY_ONCE);
//        verifyPublishIsReceived("Hello2", EXACTLY_ONCE);
//    }

//    /**
//     * subscriber connect and subscribe on "a/b" QoS 1 and "a/+" QoS 2 publisher connects and send a
//     * message "hello" on "a/b" subscriber must receive only a single message not twice
//     */
//    @Test
//    public void checkSinglePublishOnOverlappingSubscriptions() {
//        EmbeddedChannel publisher = new EmbeddedChannel();
//        connectAsClient(publisher, "Publisher");
//
//        connect();
//        subscribe("a/+", EXACTLY_ONCE);
//        subscribe("a/b", AT_LEAST_ONCE);
//
//        // force the publisher to send
//        publishToAs(publisher, "Publisher", "a/b", "Hello world MQTT!!", AT_LEAST_ONCE, 60, false);
//
//        verifyPublishIsReceived("Hello world MQTT!!", AT_LEAST_ONCE);
//        // try to listen a second publish
//        verifyNoPublishIsReceived();
//    }

//    @Test
//    public void testForceClientDisconnection_issue116() {
//        EmbeddedChannel clientXA = new EmbeddedChannel();
//        connectAsClient(clientXA, "subscriber");
//        subscribe(clientXA, "topic", AT_MOST_ONCE);
//
//        EmbeddedChannel clientXB = new EmbeddedChannel();
//        connectAsClient(clientXB, "publisher");
//        publishQoS2ToAs(clientXB, "publisher", "topic", "Hello", 20, false);
//
//        EmbeddedChannel clientYA = new EmbeddedChannel();
//        connectAsClient(clientYA, "subscriber");
//        subscribe(clientYA, "topic", AT_MOST_ONCE);
//
//        EmbeddedChannel clientYB = new EmbeddedChannel();
//        connectAsClient(clientYB, "publisher");
//        publishQoS2ToAs(clientYB, "publisher", "topic", "Hello 2", 20, true);
//
//        assertFalse("First 'subscriber' channel MUST be closed by the broker", clientXA.isOpen());
//        verifyPublishIsReceived(clientYA, "Hello 2", AT_MOST_ONCE);
//    }

//    @Test
//    public void testPublishToEmptyTopic() {
//        EmbeddedChannel subscriber = new EmbeddedChannel();
//        connectAsClient(subscriber, "Subscriber");
//
//        subscribe(subscriber, "#", AT_LEAST_ONCE);
//
//        // Exercise
//        connect();
//        publishToAs(FAKE_CLIENT_ID, "", AT_LEAST_ONCE, 1, false);
//
//        // Verify
//        verifyNoPublishIsReceived(subscriber);
//        assertFalse("On empty topic subscription the channel MUST be closed", m_channel.isOpen());
//    }

}
