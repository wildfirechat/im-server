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

import io.netty.handler.codec.mqtt.MqttConnectMessage;

public class InterceptConnectMessage extends InterceptAbstractMessage {

    private final MqttConnectMessage msg;

    public InterceptConnectMessage(MqttConnectMessage msg) {
        super(msg);
        this.msg = msg;
    }

    public String getClientID() {
        return msg.payload().clientIdentifier();
    }

    public boolean isCleanSession() {
        return msg.variableHeader().isCleanSession();
    }

    public int getKeepAlive() {
        return msg.variableHeader().keepAliveTimeSeconds();
    }

    public boolean isPasswordFlag() {
        return msg.variableHeader().hasPassword();
    }

    public byte getProtocolVersion() {
        return (byte) msg.variableHeader().version();
    }

    public String getProtocolName() {
        return msg.variableHeader().name();
    }

    public boolean isUserFlag() {
        return msg.variableHeader().hasUserName();
    }

    public boolean isWillFlag() {
        return msg.variableHeader().isWillFlag();
    }

    public byte getWillQos() {
        return (byte) msg.variableHeader().willQos();
    }

    public boolean isWillRetain() {
        return msg.variableHeader().isWillRetain();
    }

    public String getUsername() {
        return msg.payload().userName();
    }

    public byte[] getPassword() {
        return msg.payload().password();
    }

    public String getWillTopic() {
        return msg.payload().willTopic();
    }

    public byte[] getWillMessage() {
        return msg.payload().willMessage().getBytes();
    }
}
