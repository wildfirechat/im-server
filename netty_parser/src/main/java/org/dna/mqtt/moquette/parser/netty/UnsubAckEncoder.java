package org.dna.mqtt.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.UnsubAckMessage;

/**
 *
 * @author andrea
 */
class UnsubAckEncoder extends DemuxEncoder<UnsubAckMessage> {
    
    @Override
    protected void encode(ChannelHandlerContext chc, UnsubAckMessage msg, ByteBuf out) {
        out.writeByte(AbstractMessage.UNSUBACK << 4).
                writeBytes(Utils.encodeRemainingLength(2)).
                writeShort(msg.getMessageID());
    }
}