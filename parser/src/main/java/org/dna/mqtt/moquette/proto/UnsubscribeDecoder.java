package org.dna.mqtt.moquette.proto;

import java.io.UnsupportedEncodingException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.proto.messages.SubscribeMessage;
import org.dna.mqtt.moquette.proto.messages.UnsubscribeMessage;

/**
 *
 * @author andrea
 */
public class UnsubscribeDecoder extends MqttDecoder {

    public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
        return Utils.checkDecodable(AbstractMessage.UNSUBSCRIBE, in);
    }

    public MessageDecoderResult decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        //Common decoding part
        UnsubscribeMessage message = new UnsubscribeMessage();
        if (decodeCommonHeader(message, in) == NEED_DATA) {
            return NEED_DATA;
        }
        
        //check qos level
        if (message.getQos() != QOSType.LEAST_ONE) {
            return NOT_OK;
        }
            
        int start = in.position();
        //read  messageIDs
        message.setMessageID(Utils.readWord(in));
        int readed = in.position() - start;
        while (readed < message.getRemainingLength()) {
            message.addTopic(Utils.decodeString(in));
            readed = in.position() - start;
        }
        
        out.write(message);
        return OK;
    }
    
}
