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

import io.moquette.server.netty.NettyUtils;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.junit.Before;
import org.junit.Test;

import static io.netty.handler.codec.mqtt.MqttQoS.*;
import static org.junit.Assert.assertEquals;

public class ProtocolProcessor_InternalPublish_Test extends AbstractProtocolProcessorCommonUtils {

    @Before
    public void setUp() {
        initializeProcessorAndSubsystems();
        connect();
    }

    @Test
    public void testClientSubscribeAfterNotRetainedQoS0IsSent() {
        // Exercise
        final String topic = "/topic";
        internalPublishNotRetainedTo(topic);

        subscribe(topic, AT_MOST_ONCE);

        // Verify
        verifyNoPublishIsReceived();
    }

    @Test
    public void testClientSubscribeAfterRetainedQoS0IsSent() {
        // Exercise
        final String topic = "/topic";
        internalPublishRetainedTo(topic);

        subscribe(topic, AT_MOST_ONCE);

        // Verify
        verifyNoPublishIsReceived();
    }

    @Test
    public void testClientSubscribeBeforeNotRetainedQoS0IsSent() {
        subscribe("/topic", AT_MOST_ONCE);

        // Exercise
        internalPublishNotRetainedTo("/topic");

        // Verify
        //verifyMessageIsReceivedSuccessfully();
        verifyPublishIsReceived();
    }

    @Test
    public void testClientSubscribeBeforeRetainedQoS0IsSent() {
        subscribe("/topic", AT_MOST_ONCE);

        // Exercise
        internalPublishRetainedTo("/topic");

        // Verify
        verifyPublishIsReceived();
    }

    @Test
    public void testClientSubscribeBeforeNotRetainedQoS1IsSent() {
        subscribe("/topic", AT_LEAST_ONCE);

        // Exercise
        internalPublishTo("/topic", AT_LEAST_ONCE, false);

        // Verify
        verifyPublishIsReceived();
    }

    @Test
    public void testClientSubscribeAfterNotRetainedQoS1IsSent() {
        // Exercise
        internalPublishTo("/topic", AT_LEAST_ONCE, false);
        subscribe("/topic", AT_LEAST_ONCE);

        // Verify
        verifyNoPublishIsReceived();
    }

    @Test
    public void testClientSubscribeBeforeRetainedQoS1IsSent() {
        subscribe("/topic", AT_LEAST_ONCE);

        // Exercise
        internalPublishTo("/topic", AT_LEAST_ONCE, true);

        // Verify
        verifyPublishIsReceived();
    }

    @Test
    public void testClientSubscribeAfterRetainedQoS1IsSent() {
        // Exercise
        internalPublishTo("/topic", AT_LEAST_ONCE, true);
        subscribe("/topic", AT_LEAST_ONCE);

        // Verify
        verifyPublishIsReceived();
    }

    @Test
    public void testClientSubscribeBeforeNotRetainedQoS2IsSent() {
        subscribe("/topic", EXACTLY_ONCE);

        // Exercise
        internalPublishTo("/topic", EXACTLY_ONCE, false);

        // Verify
        verifyPublishIsReceived();
    }

    @Test
    public void testClientSubscribeAfterNotRetainedQoS2IsSent() {
        // Exercise
        internalPublishTo("/topic", EXACTLY_ONCE, false);
        subscribe("/topic", EXACTLY_ONCE);

        // Verify
        verifyNoPublishIsReceived();
    }

    @Test
    public void testClientSubscribeBeforeRetainedQoS2IsSent() {
        subscribe("/topic", EXACTLY_ONCE);

        // Exercise
        internalPublishTo("/topic", EXACTLY_ONCE, true);

        // Verify
        verifyPublishIsReceived();
    }

    @Test
    public void testClientSubscribeAfterRetainedQoS2IsSent() {
        // Exercise
        internalPublishTo("/topic", EXACTLY_ONCE, true);
        subscribe("/topic", EXACTLY_ONCE);

        // Verify
        verifyPublishIsReceived();
    }

    @Test
    public void testClientSubscribeAfterDisconnected() throws InterruptedException {
        subscribe("foo", AT_MOST_ONCE);
        disconnect();

        internalPublishTo("foo", AT_MOST_ONCE, false);

        verifyNoPublishIsReceived();
    }

    @Test
    public void testClientSubscribeWithoutCleanSession() throws Exception {
        subscribe("foo", AT_MOST_ONCE);
        disconnect();
        assertEquals(1, m_sessionStore.subscriptionStore().listAllSubscriptions().size());

        m_channel = new EmbeddedChannel();
        NettyUtils.clientID(m_channel, FAKE_CLIENT_ID);

        connectNoCleanSession();
        assertEquals(1, m_sessionStore.subscriptionStore().listAllSubscriptions().size());
        internalPublishTo("foo", MqttQoS.AT_MOST_ONCE, false);
        verifyPublishIsReceived();
    }

}
