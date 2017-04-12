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

import io.moquette.server.ConnectionDescriptorStore;
import io.moquette.spi.ClientSession;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.MessageGUID;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import static io.moquette.spi.impl.ProtocolProcessor.lowerQosToTheSubscriptionDesired;

class MessagesPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(MessagesPublisher.class);
    private final ConnectionDescriptorStore connectionDescriptors;
    private final ISessionsStore m_sessionsStore;
    private final IMessagesStore m_messagesStore;
    private final PersistentQueueMessageSender messageSender;

    MessagesPublisher(ConnectionDescriptorStore connectionDescriptors, ISessionsStore sessionsStore,
            IMessagesStore messagesStore, PersistentQueueMessageSender messageSender) {
        this.connectionDescriptors = connectionDescriptors;
        this.m_sessionsStore = sessionsStore;
        this.m_messagesStore = messagesStore;
        this.messageSender = messageSender;
    }

    static MqttPublishMessage notRetainedPublish(String topic, MqttQoS qos, ByteBuf message) {
        return notRetainedPublishWithMessageId(topic, qos, message, 0);
    }

    private static MqttPublishMessage notRetainedPublishWithMessageId(String topic, MqttQoS qos, ByteBuf message,
            int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, qos, false, 0);
        MqttPublishVariableHeader varHeader = new MqttPublishVariableHeader(topic, messageId);
        return new MqttPublishMessage(fixedHeader, varHeader, message);
    }

    void publish2Subscribers(IMessagesStore.StoredMessage pubMsg, List<Subscription> topicMatchingSubscriptions) {
        final String topic = pubMsg.getTopic();
        final MqttQoS publishingQos = pubMsg.getQos();
        final ByteBuf origMessage = pubMsg.getMessage();

        // if QoS 1 or 2 store the message
        // TODO perhaps this block is not needed.
        MessageGUID guid = null;
        if (publishingQos != MqttQoS.AT_MOST_ONCE) {
            guid = m_messagesStore.storePublishForFuture(pubMsg);
        }

        for (final Subscription sub : topicMatchingSubscriptions) {
            MqttQoS qos = lowerQosToTheSubscriptionDesired(sub, publishingQos);
            ClientSession targetSession = m_sessionsStore.sessionForClient(sub.getClientId());

            boolean targetIsActive = this.connectionDescriptors.isConnected(sub.getClientId());

            // we need to retain because duplicate only copy r/w indexes and don't retain() causing
            // refCnt = 0
            ByteBuf message = origMessage.retainedDuplicate();
            if (targetIsActive) {
                LOG.debug("Sending PUBLISH message to active subscriber. CId={}, topicFilter={}, qos={}",
                    sub.getClientId(), sub.getTopicFilter(), qos);
                MqttPublishMessage publishMsg = notRetainedPublish(topic, qos, message);
                if (qos != MqttQoS.AT_MOST_ONCE) {
                    // QoS 1 or 2
                    int messageId = targetSession.inFlightAckWaiting(guid);
                    // set the PacketIdentifier only for QoS > 0
                    publishMsg = notRetainedPublishWithMessageId(topic, qos, message, messageId);
                }
                this.messageSender.sendPublish(targetSession, publishMsg);
            } else {
                if (!targetSession.isCleanSession()) {
                    LOG.debug("Storing pending PUBLISH inactive message. CId={}, topicFilter={}, qos={}",
                        sub.getClientId(), sub.getTopicFilter(), qos);
                    // store the message in targetSession queue to deliver
                    targetSession.enqueue(pubMsg);
                }
            }
        }
    }
}
