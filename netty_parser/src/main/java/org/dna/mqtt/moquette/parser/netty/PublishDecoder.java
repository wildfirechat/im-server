package org.dna.mqtt.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.util.List;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
class PublishDecoder extends DemuxDecoder {
    
    private static Logger LOG = LoggerFactory.getLogger(PublishDecoder.class);

    @Override
    void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        LOG.debug("decode invoked with buffer " + in);
        int startPos = in.readerIndex();
        in.resetReaderIndex();

        //Common decoding part
        PublishMessage message = new PublishMessage();
        if (!decodeCommonHeader(message, in)) {
            LOG.info("decode ask for more data after " + in);
            in.resetReaderIndex();
            return;
        }
        int remainingLength = message.getRemainingLength();
        
        //Topic name
        String topic = Utils.decodeString(in);
        if (topic == null) {
            in.resetReaderIndex();
            return;
        }
        message.setTopicName(topic);
        
        if (message.getQos() == AbstractMessage.QOSType.LEAST_ONE || 
                message.getQos() == AbstractMessage.QOSType.EXACTLY_ONCE) {
            message.setMessageID(in.readUnsignedShort());
        }
        int stopPos = in.readerIndex();
        
        //read the payload
        int payloadSize = remainingLength - (stopPos - startPos - 2) + (Utils.numBytesToEncode(remainingLength) - 1);
        if (in.readableBytes() < payloadSize) {
            in.resetReaderIndex();
            return;
        }
        byte[] b = new byte[payloadSize];
        in.readBytes(b);
        message.setPayload(b);
        
        out.add(message);
    }
    
}
