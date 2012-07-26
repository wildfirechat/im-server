package org.dna.mqtt.moquette.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.proto.messages.SubAckMessage;

/**
 * QoS, DUP and RETAIN are NOT used.
 * 
 * @author andrea
 */
public class SubAckDecoder extends MqttDecoder {

    public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
        return Utils.checkDecodable(AbstractMessage.SUBACK, in);
    }

    public MessageDecoderResult decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        //Common decoding part
        SubAckMessage message = new SubAckMessage();
        if (decodeCommonHeader(message, in) == NEED_DATA) {
            return NEED_DATA;
        }
        int remainingLength = message.getRemainingLength();
        
        //MessageID
        message.setMessageID(Utils.readWord(in));
        remainingLength -= 2;
        
        //Qos array
        if (in.remaining() < remainingLength ) {
            return NEED_DATA;
        }
        for (int i = 0; i < remainingLength; i++) {
            byte qos = in.get();
            message.addType(QOSType.values()[qos]);
        }
        
        out.write(message);
        return OK;
    }
    
}
