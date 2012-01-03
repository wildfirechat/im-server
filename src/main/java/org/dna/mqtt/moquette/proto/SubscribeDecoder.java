package org.dna.mqtt.moquette.proto;

import java.io.UnsupportedEncodingException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.proto.messages.SubscribeMessage;

/**
 *
 * @author andrea
 */
public class SubscribeDecoder extends MqttDecoder {

    public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
        return Utils.checkDecodable(AbstractMessage.SUBSCRIBE, in);
    }

    public MessageDecoderResult decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
         //Common decoding part
        SubscribeMessage message = new SubscribeMessage();
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
            decodeSubscription(in, message);
            readed = in.position() - start;
        }
        
        out.write(message);
        return OK;
    }
    
    /**
     * Populate the message with couple of Qos, topic
     */
    private void decodeSubscription(IoBuffer in, SubscribeMessage message) throws UnsupportedEncodingException {
        String topic = Utils.decodeString(in);
        byte qos = (byte)(in.get() & 0x03);
        message.addSubscription(new SubscribeMessage.Couple(qos, topic));
    }
    
}
