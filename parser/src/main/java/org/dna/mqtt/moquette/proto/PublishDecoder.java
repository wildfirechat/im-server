package org.dna.mqtt.moquette.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
public class PublishDecoder extends MqttDecoder {
    
    private static Logger LOG = LoggerFactory.getLogger(PublishDecoder.class);
    
    public MessageDecoderResult decodable(IoSession session, IoBuffer in) {
        return Utils.checkDecodable(AbstractMessage.PUBLISH, in);
    }

    public MessageDecoderResult decode(IoSession session, IoBuffer in, ProtocolDecoderOutput out) throws Exception {
        LOG.debug("decode invoked with buffer " + in);
        int startPos = in.position();

        //Common decoding part
        PublishMessage message = new PublishMessage();
        if (decodeCommonHeader(message, in) == NEED_DATA) {
            LOG.info("decode ask for more data after " + in);
            return NEED_DATA;
        }
        int remainingLength = message.getRemainingLength();
        
        //Topic name
        String topic = Utils.decodeString(in);
        if (topic == null) {
            return NEED_DATA;
        }
        message.setTopicName(topic);
        
        if (message.getQos() == QOSType.LEAST_ONE || 
                message.getQos() == QOSType.EXACTLY_ONCE) {
            message.setMessageID(Utils.readWord(in));
        }
        int stopPos = in.position();
        
        //read the payload
        int payloadSize = remainingLength - (stopPos - startPos - 2) + (Utils.numBytesToEncode(remainingLength) - 1);
        if (in.remaining() < payloadSize) {
            return NEED_DATA;
        }
        byte[] b = new byte[payloadSize];
        in.get(b);
        message.setPayload(b);
        
        out.write(message);
        return OK;
    }
    
}
