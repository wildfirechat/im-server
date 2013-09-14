package org.dna.mqtt.moquette.proto.messages;

/**
 *
 * @author andrea
 */
public class PubCompMessage extends MessageIDMessage {
    
    public PubCompMessage() {
        m_messageType = AbstractMessage.PUBCOMP;
    }
}

