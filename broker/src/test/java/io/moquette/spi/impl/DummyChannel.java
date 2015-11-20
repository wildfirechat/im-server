/*
 * Copyright (c) 2012-2015 The original author or authors
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

import io.moquette.server.ServerChannel;
import io.netty.util.AttributeKey;
import io.moquette.proto.messages.AbstractMessage;
import io.moquette.proto.messages.ConnAckMessage;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author andrea
 */
public class DummyChannel implements ServerChannel {

    AbstractMessage m_receivedMessage;

    byte m_returnCode;

    private boolean m_channelClosed = false;

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
        m_channelClosed = true;
    }

    boolean isClosed() {
        return this.m_channelClosed;
    }

    public void write(Object value) {
        try {
            m_receivedMessage = (AbstractMessage) value;
            if (m_receivedMessage instanceof ConnAckMessage) {
                ConnAckMessage buf = (ConnAckMessage) m_receivedMessage;
                m_returnCode = buf.getReturnCode();
            }
        } catch (Exception ex) {
            throw new AssertionError("Wrong return code");
        }
    }

    public byte getReturnCode() {
        return m_returnCode;
    }

    public AbstractMessage getReceivedMessage() {
        return m_receivedMessage;
    }
}
