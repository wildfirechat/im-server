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
package org.eclipse.moquette.proto.messages;

import java.nio.ByteBuffer;

/**
 *
 * @author andrea
 */
public class PublishMessage extends MessageIDMessage {

    private String m_topicName;
//    private Integer m_messageID; //could be null if Qos is == 0
    private ByteBuffer m_payload;

    /*public Integer getMessageID() {
        return m_messageID;
    }

    public void setMessageID(Integer messageID) {
        this.m_messageID = messageID;
    }*/
    
    public PublishMessage() {
        m_messageType = AbstractMessage.PUBLISH;
    }

    public String getTopicName() {
        return m_topicName;
    }

    public void setTopicName(String topicName) {
        this.m_topicName = topicName;
    }

    public ByteBuffer getPayload() {
        return m_payload;
    }

    public void setPayload(ByteBuffer payload) {
        this.m_payload = payload;
    }
}
