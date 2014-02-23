package org.dna.mqtt.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.SubscribeMessage;

/**
 *
 * @author andrea
 */
class SubscribeEncoder extends DemuxEncoder<SubscribeMessage> {

    @Override
    protected void encode(ChannelHandlerContext chc, SubscribeMessage message, ByteBuf out) {
         if (message.subscriptions().isEmpty()) {
            throw new IllegalArgumentException("Found a subscribe message with empty topics");
        }

        if (message.getQos() != AbstractMessage.QOSType.LEAST_ONE) {
            throw new IllegalArgumentException("Expected a message with QOS 1, found " + message.getQos());
        }
        
        ByteBuf variableHeaderBuff = chc.alloc().buffer(4);
        ByteBuf buff = null;
        try {
            variableHeaderBuff.writeShort(message.getMessageID());
            for (SubscribeMessage.Couple c : message.subscriptions()) {
                variableHeaderBuff.writeBytes(Utils.encodeString(c.getTopic()));
                variableHeaderBuff.writeByte(c.getQos());
            }

            int variableHeaderSize = variableHeaderBuff.readableBytes();
            byte flags = Utils.encodeFlags(message);
            buff = chc.alloc().buffer(2 + variableHeaderSize);

            buff.writeByte(AbstractMessage.SUBSCRIBE << 4 | flags);
            buff.writeBytes(Utils.encodeRemainingLength(variableHeaderSize));
            buff.writeBytes(variableHeaderBuff);

            out.writeBytes(buff);
        } finally {
             variableHeaderBuff.release();
             buff.release();
        }
    }
    
}
