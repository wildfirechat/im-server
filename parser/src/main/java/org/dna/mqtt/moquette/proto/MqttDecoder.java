package org.dna.mqtt.moquette.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.demux.MessageDecoderAdapter;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;

/**
 * Contains common code for all mqtt decoders.
 * 
 * @author andrea
 */
public abstract class MqttDecoder extends MessageDecoderAdapter {

    protected MessageDecoderResult decodeCommonHeader(AbstractMessage message, IoBuffer in) {
        //Common decoding part
        if (in.remaining() < 2) {
            return NEED_DATA;
        }
        byte h1 = in.get();
        byte messageType = (byte) ((h1 & 0x00F0) >> 4);
        boolean dupFlag = ((byte) ((h1 & 0x0008) >> 3) == 1);
        byte qosLevel = (byte) ((h1 & 0x0006) >> 1);
        boolean retainFlag = ((byte) (h1 & 0x0001) == 1);
        int remainingLength = Utils.decodeRemainingLenght(in);
        if (remainingLength == -1) {
            return NEED_DATA;
        }

        message.setMessageType(messageType);
        message.setDupFlag(dupFlag);
        message.setQos(AbstractMessage.QOSType.values()[qosLevel]);
        message.setRetainFlag(retainFlag);
        message.setRemainingLength(remainingLength);
        return OK;
    }
}
