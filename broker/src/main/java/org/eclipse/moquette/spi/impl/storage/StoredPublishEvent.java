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
package org.eclipse.moquette.spi.impl.storage;

import java.io.Serializable;
import java.nio.ByteBuffer;
import org.eclipse.moquette.spi.impl.events.PublishEvent;
import org.eclipse.moquette.proto.messages.AbstractMessage.QOSType;

/**
 * Publish event serialized to the DB.
 * 
 * @author andrea
 */
public class StoredPublishEvent implements Serializable {
    String m_topic;
    QOSType m_qos;
    byte[] m_message;
    boolean m_retain;
    String m_clientID;
    //Optional attribute, available only fo QoS 1 and 2
    Integer m_msgID;
    
    public StoredPublishEvent(PublishEvent wrapped) {
        m_topic = wrapped.getTopic();
        m_qos = wrapped.getQos();
        m_retain = wrapped.isRetain();
        m_clientID = wrapped.getClientID();
        m_msgID = wrapped.getMessageID();
        
        ByteBuffer buffer = wrapped.getMessage();
        m_message = new byte[buffer.remaining()];
        buffer.get(m_message);
        buffer.rewind();
    }
    
    public String getTopic() {
        return m_topic;
    }

    public QOSType getQos() {
        return m_qos;
    }

    public byte[] getMessage() {
        return m_message;
    }

    public boolean isRetain() {
        return m_retain;
    }
    
    public String getClientID() {
        return m_clientID;
    }

    public Integer getMessageID() {
        return m_msgID;
    }
}
