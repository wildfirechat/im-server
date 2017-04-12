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

import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.SubscriptionsStore;
import io.moquette.spi.impl.subscriptions.Topic;
import io.moquette.spi.security.IAuthorizator;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import static io.moquette.spi.impl.ProtocolProcessor.asStoredMessage;

class Qos0PublishHandler extends QosPublishHandler {

    private static final Logger LOG = LoggerFactory.getLogger(Qos0PublishHandler.class);

    private final SubscriptionsStore subscriptions;
    private final IMessagesStore m_messagesStore;
    private final BrokerInterceptor m_interceptor;
    private final MessagesPublisher publisher;

    Qos0PublishHandler(IAuthorizator authorizator, SubscriptionsStore subscriptions, IMessagesStore messagesStore,
            BrokerInterceptor interceptor, MessagesPublisher messagesPublisher) {
        super(authorizator);
        this.subscriptions = subscriptions;
        this.m_messagesStore = messagesStore;
        this.m_interceptor = interceptor;
        this.publisher = messagesPublisher;
    }

    void receivedPublishQos0(Channel channel, MqttPublishMessage msg) {
        // verify if topic can be write
        final Topic topic = new Topic(msg.variableHeader().topicName());
        if (checkWriteOnTopic(topic, channel)) {
            return;
        }

        // route message to subscribers
        IMessagesStore.StoredMessage toStoreMsg = asStoredMessage(msg);
        String clientID = NettyUtils.clientID(channel);
        toStoreMsg.setClientID(clientID);

        final int messageID = msg.variableHeader().messageId();

        if (LOG.isTraceEnabled()) {
            LOG.trace("Sending publish message to subscribers. ClientId={}, topic={}, messageId={}, payload={}, " +
                "subscriptionTree={}", clientID, topic, messageID, DebugUtils.payload2Str(toStoreMsg.getMessage()),
                subscriptions.dumpTree());
        } else {
            LOG.info("Sending publish message to subscribers. ClientId={}, topic={}, messageId={}", clientID, topic,
                messageID);
        }

        List<Subscription> topicMatchingSubscriptions = subscriptions.matches(topic);
        this.publisher.publish2Subscribers(toStoreMsg, topicMatchingSubscriptions);

        if (msg.fixedHeader().isRetain()) {
            // QoS == 0 && retain => clean old retained
            m_messagesStore.cleanRetained(topic);
        }

        String username = NettyUtils.userName(channel);
        m_interceptor.notifyTopicPublished(msg, clientID, username);
    }
}
