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

package io.moquette.server.netty;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.*;
import java.util.ArrayList;
import java.util.List;

public final class MessageBuilder {

    public static class PublishBuilder {

        private String topic;
        private boolean retained;
        private MqttQoS qos;
        private byte[] payload;
        private int messageId;

        public PublishBuilder topicName(String topic) {
            this.topic = topic;
            return PublishBuilder.this;
        }

        public PublishBuilder retained(boolean retained) {
            this.retained = retained;
            return this;
        }

        public PublishBuilder qos(MqttQoS qos) {
            this.qos = qos;
            return this;
        }

        public PublishBuilder payload(byte[] payload) {
            this.payload = payload;
            return this;
        }

        public PublishBuilder messageId(int messageId) {
            this.messageId = messageId;
            return this;
        }

        public MqttPublishMessage build() {
            MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, qos, retained, 0);
            MqttPublishVariableHeader mqttVariableHeader = new MqttPublishVariableHeader(topic, messageId);
            return new MqttPublishMessage(mqttFixedHeader, mqttVariableHeader, Unpooled.buffer().writeBytes(payload));
        }
    }

    public static class ConnectBuilder {

        private MqttVersion version = MqttVersion.MQTT_3_1_1;
        private String clientId;
        private boolean cleanSession;
        private boolean hasUser;
        private boolean hasPassword;
        private int keepAliveSecs;
        private boolean willFlag;
        private MqttQoS willQos = MqttQoS.AT_MOST_ONCE;
        private String willTopic;
        private String willMessage;
        private String username;
        private String password;

        public ConnectBuilder protocolVersion(MqttVersion version) {
            this.version = version;
            return this;
        }

        public ConnectBuilder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public ConnectBuilder cleanSession(boolean cleanSession) {
            this.cleanSession = cleanSession;
            return this;
        }

        public ConnectBuilder keepAlive(int keepAliveSecs) {
            this.keepAliveSecs = keepAliveSecs;
            return this;
        }

        public ConnectBuilder willFlag(boolean willFlag) {
            this.willFlag = willFlag;
            return this;
        }

        public ConnectBuilder willQoS(MqttQoS willQos) {
            this.willQos = willQos;
            return this;
        }

        public ConnectBuilder willTopic(String willTopic) {
            this.willTopic = willTopic;
            return this;
        }

        public ConnectBuilder willMessage(String willMessage) {
            this.willMessage = willMessage;
            return this;
        }

        public ConnectBuilder hasUser() {
            this.hasUser = true;
            return this;
        }

        public ConnectBuilder hasPassword() {
            this.hasPassword = true;
            return this;
        }

        public ConnectBuilder username(String username) {
            this.username = username;
            return this;
        }

        public ConnectBuilder password(String password) {
            this.password = password;
            return this;
        }

        public MqttConnectMessage build() {
            MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
                    MqttMessageType.CONNECT,
                    false,
                    MqttQoS.AT_MOST_ONCE,
                    false,
                    0);
            MqttConnectVariableHeader mqttConnectVariableHeader = new MqttConnectVariableHeader(
                    version.protocolName(),
                    version.protocolLevel(),
                    hasUser,
                    hasPassword,
                    false,
                    willQos.value(),
                    willFlag,
                    cleanSession,
                    keepAliveSecs);
            MqttConnectPayload mqttConnectPayload = new MqttConnectPayload(
                    clientId,
                    willTopic,
                    willMessage,
                    username,
                    password);
            return new MqttConnectMessage(mqttFixedHeader, mqttConnectVariableHeader, mqttConnectPayload);
        }
    }

    public static class SubscribeBuilder {

        private List<MqttTopicSubscription> subscriptions = new ArrayList<>();
        private int messageId;

        public SubscribeBuilder addSubscription(MqttQoS qos, String topic) {
            subscriptions.add(new MqttTopicSubscription(topic, qos));
            return this;
        }

        public SubscribeBuilder messageId(int messageId) {
            this.messageId = messageId;
            return this;
        }

        public MqttSubscribeMessage build() {
            MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
                    MqttMessageType.SUBSCRIBE,
                    false,
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    0);
            MqttMessageIdVariableHeader mqttVariableHeader = MqttMessageIdVariableHeader.from(messageId);
            MqttSubscribePayload mqttSubscribePayload = new MqttSubscribePayload(subscriptions);
            return new MqttSubscribeMessage(mqttFixedHeader, mqttVariableHeader, mqttSubscribePayload);
        }
    }

    public static class UnsubscribeBuilder {

        private List<String> topicFilters = new ArrayList<>();
        private int messageId;

        public UnsubscribeBuilder addTopicFilter(String topic) {
            topicFilters.add(topic);
            return this;
        }

        public UnsubscribeBuilder messageId(int messageId) {
            this.messageId = messageId;
            return this;
        }

        public MqttUnsubscribeMessage build() {
            MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
                    MqttMessageType.UNSUBSCRIBE,
                    false,
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    0);
            MqttMessageIdVariableHeader mqttVariableHeader = MqttMessageIdVariableHeader.from(messageId);
            MqttUnsubscribePayload mqttSubscribePayload = new MqttUnsubscribePayload(topicFilters);
            return new MqttUnsubscribeMessage(mqttFixedHeader, mqttVariableHeader, mqttSubscribePayload);
        }
    }

    public static ConnectBuilder connect() {
        return new ConnectBuilder();
    }

    public static PublishBuilder publish() {
        return new PublishBuilder();
    }

    public static SubscribeBuilder subscribe() {
        return new SubscribeBuilder();
    }

    public static UnsubscribeBuilder unsubscribe() {
        return new UnsubscribeBuilder();
    }

    private MessageBuilder() {
    }
}
