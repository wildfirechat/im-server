package org.dna.mqtt.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.SubAckMessage;

/**
 *
 * @author andrea
 */
class SubAckEncoder extends DemuxEncoder<SubAckMessage> {

    @Override
    protected void encode(ChannelHandlerContext chc, SubAckMessage message, ByteBuf out) {
        if (message.types().isEmpty()) {
            throw new IllegalArgumentException("Found a suback message with empty topics");
        }

        ByteBuf variableHeaderBuff = chc.alloc().buffer(4);
        variableHeaderBuff.writeShort(message.getMessageID());
        for (AbstractMessage.QOSType c : message.types()) {
            variableHeaderBuff.writeByte(c.ordinal());
        }

        int variableHeaderSize = variableHeaderBuff.readableBytes();
        ByteBuf buff = chc.alloc().buffer(2 + variableHeaderSize);

        buff.writeByte(AbstractMessage.SUBACK << 4 );
        buff.writeBytes(Utils.encodeRemainingLength(variableHeaderSize));
        buff.writeBytes(variableHeaderBuff);

        out.writeBytes(buff);
    }
    
}
