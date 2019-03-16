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

package io.moquette.spi.impl;

import io.moquette.spi.ClientSession;
import io.moquette.spi.IMessagesStore;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Queue;

class InternalRepublisher {

    private static final Logger LOG = LoggerFactory.getLogger(InternalRepublisher.class);

    private final PersistentQueueMessageSender messageSender;

    InternalRepublisher(PersistentQueueMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    void publishRetained(ClientSession targetSession, Collection<IMessagesStore.StoredMessage> messages) {
        for (IMessagesStore.StoredMessage storedMsg : messages) {
            // fire as retained the message
            MqttPublishMessage publishMsg = retainedPublish(storedMsg);
            if (storedMsg.getQos() != MqttQoS.AT_MOST_ONCE) {
                LOG.debug("Adding message to inflight zone. ClientId={}, topic={}", targetSession.clientID,
                    storedMsg.getTopic());
                int packetID = targetSession.inFlightAckWaiting(storedMsg);

                // set the PacketIdentifier only for QoS > 0
                publishMsg = retainedPublish(storedMsg, packetID);
            }

            this.messageSender.sendPublish(targetSession, publishMsg);
        }
    }

    void publishStored(ClientSession clientSession, Queue<IMessagesStore.StoredMessage> publishedEvents) {
        IMessagesStore.StoredMessage pubEvt;
        while ((pubEvt = publishedEvents.poll()) != null) {
            // put in flight zone
            LOG.debug("Adding message ot inflight zone. ClientId={}, guid={}, topic={}", clientSession.clientID,
                pubEvt.getGuid(), pubEvt.getTopic());
            int messageId = clientSession.inFlightAckWaiting(pubEvt);
            MqttPublishMessage publishMsg = notRetainedPublish(pubEvt);
            // set the PacketIdentifier only for QoS > 0
            if (publishMsg.fixedHeader().qosLevel() != MqttQoS.AT_MOST_ONCE) {
                publishMsg = notRetainedPublish(pubEvt, messageId);
            }
            this.messageSender.sendPublish(clientSession, publishMsg);
        }
    }

    private MqttPublishMessage notRetainedPublish(IMessagesStore.StoredMessage storedMessage, Integer messageID) {
        return createPublishForQos(storedMessage.getTopic(), storedMessage.getQos(), storedMessage.getPayload(), false,
            messageID);
    }

    private MqttPublishMessage notRetainedPublish(IMessagesStore.StoredMessage storedMessage) {
        return createPublishForQos(storedMessage.getTopic(), storedMessage.getQos(), storedMessage.getPayload(), false,
            0);
    }

    private MqttPublishMessage retainedPublish(IMessagesStore.StoredMessage storedMessage) {
        return createPublishForQos(storedMessage.getTopic(), storedMessage.getQos(), storedMessage.getPayload(), true,
            0);
    }

    private MqttPublishMessage retainedPublish(IMessagesStore.StoredMessage storedMessage, Integer packetID) {
        return createPublishForQos(storedMessage.getTopic(), storedMessage.getQos(), storedMessage.getPayload(), true,
            packetID);
    }

    public static MqttPublishMessage createPublishForQos(String topic, MqttQoS qos, ByteBuf message, boolean retained,
            int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, qos, retained, 0);
        MqttPublishVariableHeader varHeader = new MqttPublishVariableHeader(topic, messageId);
        return new MqttPublishMessage(fixedHeader, varHeader, message);
    }
}
