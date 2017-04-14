/*
 * Copyright (c) 2012-2016 The original author or authors
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

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.util.concurrent.*;

/**
 * Used in test to collect all messages received asynchronously by MqttClient.
 */
public class MessageCollector implements MqttCallback {

    private final class ReceivedMessage {

        private final MqttMessage message;
        private final String topic;

        private ReceivedMessage(MqttMessage message, String topic) {
            this.message = message;
            this.topic = topic;
        }
    }

    private BlockingQueue<ReceivedMessage> m_messages = new LinkedBlockingQueue<>();
    private boolean m_connectionLost;

    /**
     * Return the message from the queue if not empty, else return null with wait period.
     */
    public MqttMessage getMessageImmediate() {
        if (m_messages.isEmpty()) {
            return null;
        }
        try {
            return m_messages.take().message;
        } catch (InterruptedException e) {
            return null;
        }
    }

    public MqttMessage waitMessage(int delay) {
        try {
            ReceivedMessage msg = m_messages.poll(delay, TimeUnit.SECONDS);
            if (msg == null) {
                return null;
            }
            return msg.message;
        } catch (InterruptedException e) {
            return null;
        }
    }

    public String getTopic() {
        try {
            return m_messages.poll(5, TimeUnit.SECONDS).topic;
        } catch (InterruptedException e) {
            return null;
        }
    }

    void reinit() {
        m_messages = new LinkedBlockingQueue<>();
        m_connectionLost = false;
    }

    public boolean connectionLost() {
        return m_connectionLost;
    }

    @Override
    public void connectionLost(Throwable cause) {
        m_connectionLost = true;
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        m_messages.offer(new ReceivedMessage(message, topic));
    }

    /**
     * Invoked when the message sent to a server is ACKED (PUBACK or PUBCOMP by the server)
     */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // try {
        // m_messages.offer(new ReceivedMessage(token.waitMessage(), token.getTopics()[0]));
        // } catch (MqttException e) {
        // e.printStackTrace();
        // }
    }
}
