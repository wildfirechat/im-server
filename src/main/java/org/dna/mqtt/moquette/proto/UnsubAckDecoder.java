package org.dna.mqtt.moquette.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.MessageIDMessage;
import org.dna.mqtt.moquette.proto.messages.UnsubAckMessage;

/**
 *
 * @author andrea
 */
public class UnsubAckDecoder extends MessageIDDecoder {

    @Override
    protected MessageIDMessage createMessage() {
        return new UnsubAckMessage();
    }

    @Override
    public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
        return Utils.checkDecodable(AbstractMessage.UNSUBACK, in);
    }
}

