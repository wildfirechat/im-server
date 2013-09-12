package org.dna.mqtt.moquette.parser.netty;

import org.dna.mqtt.moquette.proto.messages.MessageIDMessage;
import org.dna.mqtt.moquette.proto.messages.UnsubAckMessage;

/**
 *
 * @author andrea
 */
class UnsubAckDecoder extends MessageIDDecoder {

    @Override
    protected MessageIDMessage createMessage() {
        return new UnsubAckMessage();
    }
}

