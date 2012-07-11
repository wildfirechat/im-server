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
    int m_messageID;

    public int getMessageID() {
        return m_messageID;
    }

    public void setMessageID(int messageID) {
        this.m_messageID = messageID;
    }
    
}
