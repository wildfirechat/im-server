package org.dna.mqtt.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.List;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;

/**
 *
 * @author andrea
 */
abstract class DemuxDecoder {
    abstract void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception;
    
    protected boolean decodeCommonHeader(AbstractMessage message, ByteBuf in) {
        //Common decoding part
        if (in.readableBytes() < 2) {
            return false;
        }
        byte h1 = in.readByte();
        byte messageType = (byte) ((h1 & 0x00F0) >> 4);
        boolean dupFlag = ((byte) ((h1 & 0x0008) >> 3) == 1);
        byte qosLevel = (byte) ((h1 & 0x0006) >> 1);
        boolean retainFlag = ((byte) (h1 & 0x0001) == 1);
        int remainingLength = Utils.decodeRemainingLenght(in);
        if (remainingLength == -1) {
            return false;
        }

        message.setMessageType(messageType);
        message.setDupFlag(dupFlag);
        message.setQos(AbstractMessage.QOSType.values()[qosLevel]);
        message.setRetainFlag(retainFlag);
        message.setRemainingLength(remainingLength);
        return true;
    }
    
}
