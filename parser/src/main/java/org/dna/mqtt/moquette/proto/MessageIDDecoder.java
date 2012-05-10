package org.dna.mqtt.moquette.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.dna.mqtt.moquette.proto.messages.MessageIDMessage;

/**
 *
 * @author andrea
 */
public abstract class MessageIDDecoder extends MqttDecoder {
    protected abstract MessageIDMessage createMessage();
    
    public MessageDecoderResult decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        //Common decoding part
        MessageIDMessage message = createMessage();
        if (decodeCommonHeader(message, in) == NEED_DATA) {
            return NEED_DATA;
        }
        
        //read  messageIDs
        message.setMessageID(Utils.readWord(in));
        out.write(message);
        return OK;
    }
}
