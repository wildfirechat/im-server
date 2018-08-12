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

import io.moquette.connections.IConnectionsManager;
import io.moquette.server.ConnectionDescriptor;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.impl.subscriptions.Topic;
import io.moquette.spi.security.IAuthorizatorPolicy;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static io.moquette.spi.impl.ProtocolProcessor.asStoredMessage;
import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;
import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;

class Qos1PublishHandler extends QosPublishHandler {

    private static final Logger LOG = LoggerFactory.getLogger(Qos1PublishHandler.class);

    private final IMessagesStore m_messagesStore;
    private final BrokerInterceptor m_interceptor;
    private final IConnectionsManager connectionDescriptors;
    private final MessagesPublisher publisher;

    Qos1PublishHandler(IAuthorizatorPolicy authorizator, IMessagesStore messagesStore, BrokerInterceptor interceptor,
                       IConnectionsManager connectionDescriptors, MessagesPublisher messagesPublisher) {
        super(authorizator);
        this.m_messagesStore = messagesStore;
        this.m_interceptor = interceptor;
        this.connectionDescriptors = connectionDescriptors;
        this.publisher = messagesPublisher;
    }

    void receivedPublishQos1(Channel channel, MqttPublishMessage msg) {
        // verify if topic can be write
        final Topic topic = new Topic(msg.variableHeader().topicName());
        topic.getTokens();
        if (!topic.isValid()) {
            LOG.warn("Invalid topic format, force close the connection");
            channel.close().addListener(CLOSE_ON_FAILURE);
            return;
        }
        String clientID = NettyUtils.clientID(channel);
        String username = NettyUtils.userName(channel);
        if (!m_authorizator.canWrite(topic, username, clientID)) {
            LOG.error("MQTT client is not authorized to publish on topic. CId={}, topic={}", clientID, topic);
            return;
        }

        final int messageID = msg.variableHeader().messageId();

        // route message to subscribers
        IMessagesStore.StoredMessage toStoreMsg = asStoredMessage(msg);
        toStoreMsg.setClientID(clientID);

        this.publisher.publish2Subscribers(toStoreMsg, topic, messageID);

        sendPubAck(clientID, messageID);

        if (msg.fixedHeader().isRetain()) {
            if (!msg.payload().isReadable()) {
                m_messagesStore.cleanRetained(topic);
            } else {
                // before wasn't stored
                m_messagesStore.storeRetained(topic, toStoreMsg);
            }
        }

        m_interceptor.notifyTopicPublished(msg, clientID, username);
    }

    private void sendPubAck(String clientId, int messageID) {
        LOG.trace("sendPubAck invoked");
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, AT_MOST_ONCE, false, 0);
        MqttPubAckMessage pubAckMessage = new MqttPubAckMessage(fixedHeader, from(messageID));

        try {
            LOG.trace("connected clientIDs are {}", connectionDescriptors.getConnectedClientIds());
            if (!connectionDescriptors.isConnected(clientId)) {
                throw new RuntimeException(String.format("Can't find a ConnectionDescriptor for client %s in cache %s",
                    clientId, connectionDescriptors));
            }
            LOG.info("Sending {} message CId=<{}>", MqttMessageType.PUBACK, clientId);
            final Optional<ConnectionDescriptor> optDescriptor = connectionDescriptors.lookupDescriptor(clientId);
            if (optDescriptor.isPresent()) {
                final ConnectionDescriptor descriptor = optDescriptor.get();
                descriptor.writeAndFlush(pubAckMessage);
            }
        } catch (Throwable t) {
            LOG.error(null, t);
        }
    }

}
