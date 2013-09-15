package org.dna.mqtt.moquette.proto.messages;

/**
 *
 * @author andrea
 */
public class UnsubAckMessage extends MessageIDMessage {
    
    public UnsubAckMessage() {
        m_messageType = AbstractMessage.UNSUBACK;
    }
}

