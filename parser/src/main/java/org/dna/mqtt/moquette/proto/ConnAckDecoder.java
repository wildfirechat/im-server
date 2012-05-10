package org.dna.mqtt.moquette.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;

/**
 *
 * @author andrea
 */
public class ConnAckDecoder extends MqttDecoder {

    public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
        return Utils.checkDecodable(AbstractMessage.CONNACK, in);
    }

    public MessageDecoderResult decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        //Common decoding part
        ConnAckMessage message = new ConnAckMessage();
        if (decodeCommonHeader(message, in) == NEED_DATA) {
            return NEED_DATA;
        }
        //skip reserved byte
        in.skip(1);
        
        //read  return code
        message.setReturnCode(in.get());
        out.write(message);
        return OK;
    }
    
}
