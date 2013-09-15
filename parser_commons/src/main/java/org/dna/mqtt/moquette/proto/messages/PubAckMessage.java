package org.dna.mqtt.moquette.proto.messages;

/**
 * Placeholder for PUBACK message.
 * 
 * @author andrea
 */
public class PubAckMessage extends MessageIDMessage {
    
    public PubAckMessage() {
        m_messageType = AbstractMessage.PUBACK;
    }
}
