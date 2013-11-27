package org.dna.mqtt.moquette.proto.messages;

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
