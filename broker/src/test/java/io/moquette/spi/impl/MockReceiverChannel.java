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

import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.util.AttributeKey;
import java.util.HashMap;
import java.util.Map;

/**
 * This a synchronous channel that avoid output ring buffer from Processor
 */
class MockReceiverChannel {

    MqttMessage m_receivedMessage;
    private Map<Object, Object> m_attributes = new HashMap<>();

    public Object getAttribute(AttributeKey<Object> key) {
        return m_attributes.get(key);
    }

    public void setAttribute(AttributeKey<Object> key, Object value) {
        m_attributes.put(key, value);
    }

    public void setIdleTime(int idleTime) {
    }

    public void close(boolean immediately) {
    }

    public MqttMessage getMessage() {
        return this.m_receivedMessage;
    }

    public void write(Object value) {
        try {
            this.m_receivedMessage = (MqttMessage) value;
        } catch (Exception ex) {
            throw new AssertionError("Wrong return code");
        }
    }
}
