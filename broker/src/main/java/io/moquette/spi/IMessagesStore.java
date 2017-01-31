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

package io.moquette.spi;

import io.moquette.spi.impl.subscriptions.Topic;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttQoS;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Collection;

/**
 * Defines the SPI to be implemented by a StorageService that handle persistence of messages
 */
public interface IMessagesStore {

    class StoredMessage implements Serializable {

        private static final long serialVersionUID = 1755296138639817304L;
        final MqttQoS m_qos;
        final byte[] m_payload;
        final String m_topic;
        private boolean m_retained;
        private String m_clientID;
        // Optional attribute, available only fo QoS 1 and 2
        private Integer m_msgID;
        private MessageGUID m_guid;

        public StoredMessage(byte[] message, MqttQoS qos, String topic) {
            m_qos = qos;
            m_payload = message;
            m_topic = topic;
        }

        public MqttQoS getQos() {
            return m_qos;
        }

        public ByteBuffer getPayload() {
            return (ByteBuffer) ByteBuffer.allocate(m_payload.length).put(m_payload).flip();
        }

        public String getTopic() {
            return m_topic;
        }

        public void setGuid(MessageGUID guid) {
            this.m_guid = guid;
        }

        public MessageGUID getGuid() {
            return m_guid;
        }

        public String getClientID() {
            return m_clientID;
        }

        public void setClientID(String m_clientID) {
            this.m_clientID = m_clientID;
        }

        public void setMessageID(Integer messageID) {
            this.m_msgID = messageID;
        }

        public Integer getMessageID() {
            return m_msgID;
        }

        public ByteBuf getMessage() {
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
            return "PublishEvent{" + "m_msgID=" + m_msgID + ", clientID='" + m_clientID + '\'' + ", m_retain="
                    + m_retained + ", m_qos=" + m_qos + ", m_topic='" + m_topic + '\'' + '}';
        }
    }

    /**
     * Used to initialize all persistent store structures
     */
    void initStore();

    /**
     * Persist the message. If the message is empty then the topic is cleaned, else it's stored.
     *
     * @param topic
     *            for the retained.
     * @param guid
     *            of the message to mark as retained.
     */
    void storeRetained(Topic topic, MessageGUID guid);

    /**
     * Return a list of retained messages that satisfy the condition.
     *
     * @param condition
     *            the condition to match during the search.
     * @return the collection of matching messages.
     */
    Collection<StoredMessage> searchMatching(IMatchingCondition condition);

    /**
     * Persist the message.
     *
     * @param storedMessage
     *            the message to store for future usage.
     * @return the unique id in the storage (guid).
     */
    MessageGUID storePublishForFuture(StoredMessage storedMessage);

    void dropMessagesInSession(String clientID);

    StoredMessage getMessageByGuid(MessageGUID guid);

    void cleanRetained(Topic topic);

    int getPendingPublishMessages(String clientID);

    MessageGUID mapToGuid(String clientID, int messageID);
}
