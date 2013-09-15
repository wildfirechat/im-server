package org.dna.mqtt.moquette.proto.messages;

/**
 * Base class for alla the messages that carries only MessageID. (PUBACK, PUBREC,
 * PUBREL, PUBCOMP, UNSUBACK)
 * 
 * The flags dup, QOS and Retained doesn't take care.
 * 
 * @author andrea
 */
public abstract class MessageIDMessage extends AbstractMessage {
    private Integer m_messageID; //could be null if Qos is == 0

    public Integer getMessageID() {
        return m_messageID;
    }

    public void setMessageID(Integer messageID) {
        this.m_messageID = messageID;
    }

}
