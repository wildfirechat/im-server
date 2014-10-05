/*
 * Copyright (c) 2012-2014 The original author or authors
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
package org.eclipse.moquette.spi.impl.events;

import org.eclipse.moquette.proto.messages.AbstractMessage.QOSType;

import java.nio.ByteBuffer;

/**
 *
 * @author andrea
 */
public class PublishEvent extends MessagingEvent {
    String m_topic;
    QOSType m_qos;
    //byte[] m_message;
    ByteBuffer m_message;
    boolean m_retain;
    String m_clientID;
    //Optional attribute, available only fo QoS 1 and 2
    int m_msgID;

    public PublishEvent(String topic, QOSType qos, ByteBuffer message, boolean retain,
                        String clientID, Integer msgID) {
        m_topic = topic;
        m_qos = qos;
        m_message = message;
        m_retain = retain;
        m_clientID = clientID;
        if (qos != QOSType.MOST_ONE) {
            m_msgID = msgID;
        }
    }
    
    public String getTopic() {
        return m_topic;
    }

    public QOSType getQos() {
        return m_qos;
    }

    public ByteBuffer getMessage() {
        return m_message;
    }

    public boolean isRetain() {
        return m_retain;
    }
    
    public String getClientID() {
        return m_clientID;
    }

    public int getMessageID() {
        return m_msgID;
    }

    @Override
    public String toString() {
        return "PublishEvent{" +
                "m_msgID=" + m_msgID +
                ", m_clientID='" + m_clientID + '\'' +
                ", m_retain=" + m_retain +
                ", m_qos=" + m_qos +
                ", m_topic='" + m_topic + '\'' +
                '}';
    }
}
