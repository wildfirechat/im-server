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
package io.moquette.broker;

import io.moquette.interception.BrokerInterceptor;
import io.moquette.broker.subscriptions.ISubscriptionsDirectory;
import io.moquette.broker.subscriptions.Subscription;
import io.moquette.broker.subscriptions.Topic;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static io.moquette.broker.Utils.messageId;
import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.*;

class PostOffice {

    private static final Logger LOG = LoggerFactory.getLogger(PostOffice.class);

    private final Authorizator authorizator;
    private final ISubscriptionsDirectory subscriptions;
    private final IRetainedRepository retainedRepository;
    private SessionRegistry sessionRegistry;
    private BrokerInterceptor interceptor;

    PostOffice(ISubscriptionsDirectory subscriptions, IRetainedRepository retainedRepository,
               SessionRegistry sessionRegistry, BrokerInterceptor interceptor, Authorizator authorizator) {
        this.authorizator = authorizator;
        this.subscriptions = subscriptions;
        this.retainedRepository = retainedRepository;
        this.sessionRegistry = sessionRegistry;
        this.interceptor = interceptor;
    }

    public void init(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    public void fireWill(Session.Will will) {
        // MQTT 3.1.2.8-17
        publish2Subscribers(will.payload, new Topic(will.topic), will.qos);
    }

    public void subscribeClientToTopics(MqttSubscribeMessage msg, String clientID, String username,
                                        MQTTConnection mqttConnection) {
        // verify which topics of the subscribe ongoing has read access permission
        int messageID = messageId(msg);
        List<MqttTopicSubscription> ackTopics = authorizator.verifyTopicsReadAccess(clientID, username, msg);
        MqttSubAckMessage ackMessage = doAckMessageFromValidateFilters(ackTopics, messageID);

        // store topics subscriptions in session
        List<Subscription> newSubscriptions = ackTopics.stream()
            .filter(req -> req.qualityOfService() != FAILURE)
            .map(req -> {
                final Topic topic = new Topic(req.topicName());
                return new Subscription(clientID, topic, req.qualityOfService());
            }).collect(Collectors.toList());

        for (Subscription subscription : newSubscriptions) {
            subscriptions.add(subscription);
        }

        // add the subscriptions to Session
        Session session = sessionRegistry.retrieve(clientID);
        session.addSubscriptions(newSubscriptions);

        // send ack message
        mqttConnection.sendSubAckMessage(messageID, ackMessage);

        publishRetainedMessagesForSubscriptions(clientID, newSubscriptions);

        for (Subscription subscription : newSubscriptions) {
            interceptor.notifyTopicSubscribed(subscription, username);
        }
    }

    private void publishRetainedMessagesForSubscriptions(String clientID, List<Subscription> newSubscriptions) {
        Session targetSession = this.sessionRegistry.retrieve(clientID);
        for (Subscription subscription : newSubscriptions) {
            final String topicFilter = subscription.getTopicFilter().toString();
            final List<RetainedMessage> retainedMsgs = retainedRepository.retainedOnTopic(topicFilter);

            if (retainedMsgs.isEmpty()) {
                // not found
                continue;
            }
            for (RetainedMessage retainedMsg : retainedMsgs) {
                final MqttQoS retainedQos = retainedMsg.qosLevel();
                MqttQoS qos = lowerQosToTheSubscriptionDesired(subscription, retainedQos);

//                final ByteBuf origPayload = retainedMsg.getPayload();
                final ByteBuf payloadBuf = Unpooled.wrappedBuffer(retainedMsg.getPayload());
//                ByteBuf payload = origPayload.retainedDuplicate();
                targetSession.sendRetainedPublishOnSessionAtQos(subscription.getTopicFilter(), qos, payloadBuf);
            }
        }
    }

    /**
     * Create the SUBACK response from a list of topicFilters
     */
    private MqttSubAckMessage doAckMessageFromValidateFilters(List<MqttTopicSubscription> topicFilters, int messageId) {
        List<Integer> grantedQoSLevels = new ArrayList<>();
        for (MqttTopicSubscription req : topicFilters) {
            grantedQoSLevels.add(req.qualityOfService().value());
        }

        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, AT_MOST_ONCE,
            false, 0);
        MqttSubAckPayload payload = new MqttSubAckPayload(grantedQoSLevels);
        return new MqttSubAckMessage(fixedHeader, from(messageId), payload);
    }

    public void unsubscribe(List<String> topics, MQTTConnection mqttConnection, int messageId) {
        final String clientID = mqttConnection.getClientId();
        for (String t : topics) {
            Topic topic = new Topic(t);
            boolean validTopic = topic.isValid();
            if (!validTopic) {
                // close the connection, not valid topicFilter is a protocol violation
                mqttConnection.dropConnection();
                LOG.warn("Topic filter is not valid. CId={}, topics: {}, offending topic filter: {}", clientID,
                         topics, topic);
                return;
            }

            LOG.trace("Removing subscription. CId={}, topic={}", clientID, topic);
            subscriptions.removeSubscription(topic, clientID);

            // TODO remove the subscriptions to Session
//            clientSession.unsubscribeFrom(topic);

            String username = NettyUtils.userName(mqttConnection.channel);
            interceptor.notifyTopicUnsubscribed(topic.toString(), clientID, username);
        }

        // ack the client
        mqttConnection.sendUnsubAckMessage(topics, clientID, messageId);
    }

    void receivedPublishQos0(Topic topic, String username, String clientID, ByteBuf payload, boolean retain,
                             MqttPublishMessage msg) {
        if (!authorizator.canWrite(topic, username, clientID)) {
            LOG.error("MQTT client: {} is not authorized to publish on topic: {}", clientID, topic);
            return;
        }
        publish2Subscribers(payload, topic, AT_MOST_ONCE);

        if (retain) {
            // QoS == 0 && retain => clean old retained
            retainedRepository.cleanRetained(topic);
        }

        interceptor.notifyTopicPublished(msg, clientID, username);
    }

    void receivedPublishQos1(MQTTConnection connection, Topic topic, String username, ByteBuf payload, int messageID,
                             boolean retain, MqttPublishMessage msg) {
        // verify if topic can be write
        topic.getTokens();
        if (!topic.isValid()) {
            LOG.warn("Invalid topic format, force close the connection");
            connection.dropConnection();
            return;
        }
        final String clientId = connection.getClientId();
        if (!authorizator.canWrite(topic, username, clientId)) {
            LOG.error("MQTT client: {} is not authorized to publish on topic: {}", clientId, topic);
            return;
        }

        publish2Subscribers(payload, topic, AT_LEAST_ONCE);

        connection.sendPubAck(messageID);

        if (retain) {
            if (!payload.isReadable()) {
                retainedRepository.cleanRetained(topic);
            } else {
                // before wasn't stored
                retainedRepository.retain(topic, msg);
            }
        }
        interceptor.notifyTopicPublished(msg, clientId, username);
    }

    private void publish2Subscribers(ByteBuf origPayload, Topic topic, MqttQoS publishingQos) {
        Set<Subscription> topicMatchingSubscriptions = subscriptions.matchQosSharpening(topic);

        for (final Subscription sub : topicMatchingSubscriptions) {
            MqttQoS qos = lowerQosToTheSubscriptionDesired(sub, publishingQos);
            Session targetSession = this.sessionRegistry.retrieve(sub.getClientId());

            boolean isSessionPresent = targetSession != null;
            if (isSessionPresent) {
                LOG.debug("Sending PUBLISH message to active subscriber CId: {}, topicFilter: {}, qos: {}",
                          sub.getClientId(), sub.getTopicFilter(), qos);
                // we need to retain because duplicate only copy r/w indexes and don't retain() causing refCnt = 0
                ByteBuf payload = origPayload.retainedDuplicate();
                targetSession.sendPublishOnSessionAtQos(topic, qos, payload);
            } else {
                // If we are, the subscriber disconnected after the subscriptions tree selected that session as a
                // destination.
                LOG.debug("PUBLISH to not yet present session. CId: {}, topicFilter: {}, qos: {}", sub.getClientId(),
                          sub.getTopicFilter(), qos);
            }
        }
    }

    /**
     * First phase of a publish QoS2 protocol, sent by publisher to the broker. Publish to all interested
     * subscribers.
     */
    void receivedPublishQos2(MQTTConnection connection, MqttPublishMessage mqttPublishMessage, String username) {
        LOG.trace("Processing PUBREL message on connection: {}", connection);
        final Topic topic = new Topic(mqttPublishMessage.variableHeader().topicName());
        final ByteBuf payload = mqttPublishMessage.payload();

        final String clientId = connection.getClientId();
        if (!authorizator.canWrite(topic, username, clientId)) {
            LOG.error("MQTT client is not authorized to publish on topic. CId={}, topic: {}", clientId, topic);
            return;
        }

        publish2Subscribers(payload, topic, EXACTLY_ONCE);

        final boolean retained = mqttPublishMessage.fixedHeader().isRetain();
        if (retained) {
            if (!payload.isReadable()) {
                retainedRepository.cleanRetained(topic);
            } else {
                // before wasn't stored
                retainedRepository.retain(topic, mqttPublishMessage);
            }
        }

        String clientID = connection.getClientId();
        interceptor.notifyTopicPublished(mqttPublishMessage, clientID, username);
    }

    static MqttQoS lowerQosToTheSubscriptionDesired(Subscription sub, MqttQoS qos) {
        if (qos.value() > sub.getRequestedQos().value()) {
            qos = sub.getRequestedQos();
        }
        return qos;
    }

    /**
     * Intended usage is only for embedded versions of the broker, where the hosting application
     * want to use the broker to send a publish message. Like normal external publish message but
     * with some changes to avoid security check, and the handshake phases for Qos1 and Qos2. It
     * also doesn't notifyTopicPublished because using internally the owner should already know
     * where it's publishing.
     *
     * @param msg
     *            the message to publish
     */
    public void internalPublish(MqttPublishMessage msg) {
        final MqttQoS qos = msg.fixedHeader().qosLevel();
        final Topic topic = new Topic(msg.variableHeader().topicName());
        final ByteBuf payload = msg.payload();
        LOG.info("Sending internal PUBLISH message Topic={}, qos={}", topic, qos);

        publish2Subscribers(payload, topic, qos);

        if (!msg.fixedHeader().isRetain()) {
            return;
        }
        if (qos == AT_MOST_ONCE || msg.payload().readableBytes() == 0) {
            // QoS == 0 && retain => clean old retained
            retainedRepository.cleanRetained(topic);
            return;
        }
        retainedRepository.retain(topic, msg);
    }

    /**
     * notify MqttConnectMessage after connection established (already pass login).
     * @param msg
     */
    void dispatchConnection(MqttConnectMessage msg){
        interceptor.notifyClientConnected(msg);
    }

    void dispatchDisconnection(String clientId,String userName){
        interceptor.notifyClientDisconnected(clientId,userName);
    }

    void dispatchConnectionLost(String clientId,String userName){
        interceptor.notifyClientConnectionLost(clientId,userName);
    }
}
