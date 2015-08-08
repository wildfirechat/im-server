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
package org.eclipse.moquette.spi.impl;

import org.eclipse.moquette.proto.messages.PublishMessage;

import java.nio.ByteBuffer;


/**
 * A PublishMessage that is unmodifiable with copy through copy constructor.
 *
 * @author andrea
 */
final class UnmodifiablePublishMessage extends PublishMessage {

    protected byte m_messageType;

    UnmodifiablePublishMessage(final PublishMessage orig) {
        this.m_messageID = orig.getMessageID();
        this.m_topicName = orig.getTopicName();
        if (orig.getPayload() != null) {
            this.m_payload = orig.getPayload().asReadOnlyBuffer();
        }
        this.m_dupFlag = orig.isDupFlag();
        this.m_qos = orig.getQos();
        this.m_retainFlag = orig.isRetainFlag();
        this.m_remainingLength = orig.getRemainingLength();
        this.m_messageType = orig.getMessageType();
    }

    @Override
    public void setTopicName(String topicName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPayload(ByteBuffer payload) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setMessageID(Integer messageID) {
        throw new UnsupportedOperationException();
    }
}
