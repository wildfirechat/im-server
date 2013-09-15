package org.dna.mqtt.moquette.proto.messages;

/**
 *
 * @author andrea
 */
public class PubRelMessage extends MessageIDMessage {
    
    public PubRelMessage() {
        m_messageType = AbstractMessage.PUBREL;
    }
}
