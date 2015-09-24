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
package org.eclipse.moquette.spi;

import java.io.Serializable;
import java.nio.ByteBuffer;
import org.eclipse.moquette.spi.impl.events.PublishEvent;
import org.eclipse.moquette.proto.messages.AbstractMessage;

import java.util.Collection;
import java.util.List;

/**
 * Defines the SPI to be implemented by a StorageService that handle persistence of messages
 */
public interface IMessagesStore {

    public static class StoredMessage implements Serializable {
        final AbstractMessage.QOSType m_qos;
        final byte[] m_payload;
        final String m_topic;

        public StoredMessage(byte[] message, AbstractMessage.QOSType qos, String topic) {
            m_qos = qos;
            m_payload = message;
            m_topic = topic;
        }

        public AbstractMessage.QOSType getQos() {
            return m_qos;
        }

        public ByteBuffer getPayload() {
            return (ByteBuffer) ByteBuffer.allocate(m_payload.length).put(m_payload).flip();
        }

        public String getTopic() {
            return m_topic;
        }
    }

    /**
     * Used to initialize all persistent store structures
     * */
    void initStore();

    /**
     * Persist the message. 
     * If the message is empty then the topic is cleaned, else it's stored.
     */
    void storeRetained(String topic, ByteBuffer message, AbstractMessage.QOSType qos);

    /**
     * Return a list of retained messages that satisfy the condition.
     */
    Collection<StoredMessage> searchMatching(IMatchingCondition condition);

    void storePublishForFuture(PublishEvent evt);

    /**
     * Return the list of persisted publishes for the given clientID.
     * For QoS1 and QoS2 with clean session flag, this method return the list of 
     * missed publish events while the client was disconnected.
     */
    List<PublishEvent> listMessagesInSession(String clientID);
    
    void removeMessageInSession(String clientID, Integer packetID);

    void dropMessagesInSession(String clientID);

    void cleanTemporaryPublish(String clientID, int packetID);

    void storeTemporaryPublish(PublishEvent evt, String clientID, int packetID);

    /**
     * Return the next valid packetIdentifer for the given client session.
     * */
    int nextPacketID(String clientID);

    void close();

    void persistQoS2Message(String publishKey, PublishEvent evt);

    void removeQoS2Message(String publishKey);

    PublishEvent retrieveQoS2Message(String publishKey);

    void cleanRetained(String topic);
}
