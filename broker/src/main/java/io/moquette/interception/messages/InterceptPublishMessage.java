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

package io.moquette.interception.messages;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttPublishMessage;

public class InterceptPublishMessage extends InterceptAbstractMessage {

    private final MqttPublishMessage msg;
    private final String clientID;
    private final String username;

    public InterceptPublishMessage(MqttPublishMessage msg, String clientID, String username) {
        super(msg);
        this.msg = msg;
        this.clientID = clientID;
        this.username = username;
    }

    public String getTopicName() {
        return msg.variableHeader().topicName();
    }

    public ByteBuf getPayload() {
        return msg.payload();
    }

    public String getClientID() {
        return clientID;
    }

    public String getUsername() {
        return username;
    }
}
