/*
 * Copyright (c) 2012-2017 The original author or authorsgetRockQuestions()
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

import java.io.Serializable;
import java.nio.ByteBuffer;
import io.moquette.parser.proto.messages.AbstractMessage;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Defines the SPI to be implemented by a StorageService that handle persistence of messages
 */
public interface IMessagesStore {

    class StoredMessage implements Serializable {
        final AbstractMessage.QOSType m_qos;
        final byte[] m_payload;
        final String m_topic;
        private boolean m_retained;
        private String m_clientID;
        //Optional attribute, available only fo QoS 1 and 2
        private Integer m_msgID;
        private MessageGUID m_guid;
        private AtomicInteger referenceCounter = new AtomicInteger(0);

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

        public ByteBuffer getMessage() {
            return ByteBuffer.wrap(m_payload);
        }

        public void setRetained(boolean retained) {
            this.m_retained = retained;
        }

        public boolean isRetained() {
            return m_retained;
        }

        public void incReferenceCounter() {
            this.referenceCounter.incrementAndGet();
        }

        public void decReferenceCounter() {
            this.referenceCounter.decrementAndGet();
        }

        public int getReferenceCounter() {
            return this.referenceCounter.get();
        }

        @Override
        public String toString() {
            return "PublishEvent{" +
                    "m_msgID=" + m_msgID +
                    ", clientID='" + m_clientID + '\'' +
                    ", m_retain=" + m_retained +
                    ", m_qos=" + m_qos +
                    ", m_topic='" + m_topic + '\'' +
                    '}';
        }
    }

    /**
     * Used to initialize all persistent store structures
     * */
    void initStore();

    /**
     * Persist the message. 
     * If the message is empty then the topic is cleaned, else it's stored.
     * @param topic for the retained.
     * @param guid of the message to mark as retained.
     */
    void storeRetained(String topic, MessageGUID guid);

    /**
     * Return a list of retained messages that satisfy the condition.
     * @param condition the condition to match during the search.
     * @return the collection of matching messages.
     */
    Collection<StoredMessage> searchMatching(IMatchingCondition condition);

    /**
     * Persist the message.
     * @param storedMessage the message to store for future usage.
     * @return the unique id in the storage (guid).
     * */
    MessageGUID storePublishForFuture(StoredMessage storedMessage);

    /**
     * Return the list of persisted publishes for the given clientID.
     * For QoS1 and QoS2 with clean session flag, this method return the list of 
     * missed publish events while the client was disconnected.
     * @param guids the list of of guid to use as search keys.
     * @return the list of stored messages matching the passed keys.
     */
    List<StoredMessage> listMessagesInSession(Collection<MessageGUID> guids);
    
    void dropMessagesInSession(String clientID);

    StoredMessage getMessageByGuid(MessageGUID guid);

    void cleanRetained(String topic);

    void incUsageCounter(MessageGUID guid);

    void decUsageCounter(MessageGUID guid);
}
