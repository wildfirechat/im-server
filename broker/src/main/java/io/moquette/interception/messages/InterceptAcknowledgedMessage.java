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

package io.moquette.interception.messages;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.io.Serializable;

public class InterceptAcknowledgedMessage implements InterceptMessage {

    class StoredMessage implements Serializable {

        private static final long serialVersionUID = 1755296138639817304L;
        private MqttQoS m_qos;
        final byte[] m_payload;
        final String m_topic;
        private boolean m_retained;
        private String m_clientID;

        public StoredMessage(byte[] message, MqttQoS qos, String topic) {
            m_qos = qos;
            m_payload = message;
            m_topic = topic;
        }

        public void setQos(MqttQoS qos) {
            this.m_qos = qos;
        }

        public MqttQoS getQos() {
            return m_qos;
        }

        public String getTopic() {
            return m_topic;
        }

        public String getClientID() {
            return m_clientID;
        }

        public void setClientID(String m_clientID) {
            this.m_clientID = m_clientID;
        }

        public ByteBuf getPayload() {
            return Unpooled.copiedBuffer(m_payload);
        }

        public void setRetained(boolean retained) {
            this.m_retained = retained;
        }

        public boolean isRetained() {
            return m_retained;
        }

        @Override
        public String toString() {
            return "PublishEvent{clientID='" + m_clientID + '\'' + ", m_retain="
                + m_retained + ", m_qos=" + m_qos + ", m_topic='" + m_topic + '\'' + '}';
        }
    }

    private final StoredMessage msg;
    private final String username;
    private final String topic;
    private final int packetID;

    public InterceptAcknowledgedMessage(StoredMessage msg, String topic, String username, int packetID) {
        this.msg = msg;
        this.username = username;
        this.topic = topic;
        this.packetID = packetID;
    }

    public StoredMessage getMsg() {
        return msg;
    }

    public String getUsername() {
        return username;
    }

    public String getTopic() {
        return topic;
    }

    public int getPacketID() {
        return packetID;
    }
}
