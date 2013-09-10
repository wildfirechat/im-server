package org.dna.mqtt.moquette.proto.messages;

/**
 *
 * @author andrea
 */
public class PublishMessage extends MessageIDMessage {

    private String m_topicName;
//    private Integer m_messageID; //could be null if Qos is == 0
    private byte[] m_payload;

    /*public Integer getMessageID() {
        return m_messageID;
    }

    public void setMessageID(Integer messageID) {
        this.m_messageID = messageID;
    }*/

    public String getTopicName() {
        return m_topicName;
    }

    public void setTopicName(String topicName) {
        this.m_topicName = topicName;
    }

    public byte[] getPayload() {
        return m_payload;
    }

    public void setPayload(byte[] payload) {
        this.m_payload = payload;
    }
}
