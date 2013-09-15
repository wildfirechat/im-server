package org.dna.mqtt.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.dna.mqtt.moquette.parser.netty.DemuxEncoder;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.PingReqMessage;

/**
 *
 * @author andrea
 */
class PingReqEncoder  extends DemuxEncoder<PingReqMessage> {

    @Override
    protected void encode(ChannelHandlerContext chc, PingReqMessage msg, ByteBuf out) {
        out.writeByte(AbstractMessage.PINGREQ << 4).writeByte(0);
    }
}
