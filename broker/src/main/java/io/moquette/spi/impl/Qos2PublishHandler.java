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
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.ClientSession;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.subscriptions.ISubscriptionsDirectory;
import io.moquette.spi.impl.subscriptions.Topic;
import io.moquette.spi.security.IAuthorizator;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.moquette.spi.impl.DebugUtils.payload2Str;
import static io.moquette.spi.impl.ProtocolProcessor.asStoredMessage;
import static io.moquette.spi.impl.Utils.messageId;
import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;

class Qos2PublishHandler extends QosPublishHandler {

    private static final Logger LOG = LoggerFactory.getLogger(Qos2PublishHandler.class);

    private final ISubscriptionsDirectory subscriptions;
    private final IMessagesStore m_messagesStore;
    private final BrokerInterceptor m_interceptor;
    private final ConnectionDescriptorStore connectionDescriptors;
    private final MessagesPublisher publisher;
    private final SessionsRepository sessionsRepository;

    public Qos2PublishHandler(IAuthorizator authorizator, ISubscriptionsDirectory subscriptions,
                              IMessagesStore messagesStore, BrokerInterceptor interceptor,
                              ConnectionDescriptorStore connectionDescriptors,
                              MessagesPublisher messagesPublisher, SessionsRepository sessionsRepository) {
        super(authorizator);
        this.subscriptions = subscriptions;
        this.m_messagesStore = messagesStore;
        this.m_interceptor = interceptor;
        this.connectionDescriptors = connectionDescriptors;
        this.publisher = messagesPublisher;
        this.sessionsRepository = sessionsRepository;
    }

    void receivedPublishQos2(Channel channel, MqttPublishMessage msg) {
        final Topic topic = new Topic(msg.variableHeader().topicName());
        // check if the topic can be wrote
        String clientID = NettyUtils.clientID(channel);
        String username = NettyUtils.userName(channel);
        if (!m_authorizator.canWrite(topic, username, clientID)) {
            LOG.error("MQTT client is not authorized to publish on topic. CId={}, topic={}", clientID, topic);
            return;
        }
        final int messageID = msg.variableHeader().messageId();

        IMessagesStore.StoredMessage toStoreMsg = asStoredMessage(msg);
        toStoreMsg.setClientID(clientID);

        LOG.info("Sending publish message to subscribers CId={}, topic={}, messageId={}", clientID, topic, messageID);
        if (LOG.isTraceEnabled()) {
            LOG.trace("payload={}, subs Tree={}", payload2Str(toStoreMsg.getPayload()), subscriptions.dumpTree());
        }

        this.sessionsRepository.sessionForClient(clientID).markAsInboundInflight(messageID, toStoreMsg);

        sendPubRec(clientID, messageID);

        // Next the client will send us a pub rel
        // NB publish to subscribers for QoS 2 happen upon PUBREL from publisher

//        if (msg.fixedHeader().isRetain()) {
//            if (msg.payload().readableBytes() == 0) {
//                m_messagesStore.cleanRetained(topic);
//            } else {
//                m_messagesStore.storeRetained(topic, toStoreMsg);
//            }
//        }
        //TODO this should happen on PUB_REL, else we notify false positive
        m_interceptor.notifyTopicPublished(msg, clientID, username);
    }

    /**
     * Second phase of a publish QoS2 protocol, sent by publisher to the broker. Search the stored
     * message and publish to all interested subscribers.
     */
    void processPubRel(Channel channel, MqttMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        int messageID = messageId(msg);
        LOG.info("Processing PUBREL message. CId={}, messageId={}", clientID, messageID);
        ClientSession targetSession = this.sessionsRepository.sessionForClient(clientID);
        IMessagesStore.StoredMessage evt = targetSession.inboundInflight(messageID);
        if (evt == null) {
            LOG.warn("Can't find inbound inflight message for CId={}, messageId={}", clientID, messageID);
            throw new IllegalArgumentException("Can't find inbound inflight message");
        }
        final Topic topic = new Topic(evt.getTopic());

        this.publisher.publish2Subscribers(evt, topic, messageID);

        if (evt.isRetained()) {
            if (evt.getPayload().readableBytes() == 0) {
                m_messagesStore.cleanRetained(topic);
            } else {
                m_messagesStore.storeRetained(topic, evt);
            }
        }

        //TODO here we should notify to the listeners
        //m_interceptor.notifyTopicPublished(msg, clientID, username);

        sendPubComp(clientID, messageID);
    }

    private void sendPubRec(String clientID, int messageID) {
        LOG.debug("Sending PUBREC message. CId={}, messageId={}", clientID, messageID);
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, AT_MOST_ONCE, false, 0);
        MqttMessage pubRecMessage = new MqttMessage(fixedHeader, from(messageID));
        connectionDescriptors.sendMessage(pubRecMessage, messageID, clientID);
    }

    private void sendPubComp(String clientID, int messageID) {
        LOG.debug("Sending PUBCOMP message. CId={}, messageId={}", clientID, messageID);
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, AT_MOST_ONCE, false, 0);
        MqttMessage pubCompMessage = new MqttMessage(fixedHeader, from(messageID));
        connectionDescriptors.sendMessage(pubCompMessage, messageID, clientID);
    }
}
