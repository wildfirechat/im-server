package org.dna.mqtt.moquette.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;

/**
 *
 * @author andrea
 */
public class PublishEncoder implements MessageEncoder<PublishMessage> {

    public void encode(IoSession session, PublishMessage message, ProtocolEncoderOutput out) throws Exception {
        if (message.getQos() == QOSType.RESERVED) {
            throw new IllegalArgumentException("Found a message with RESERVED Qos");
        }
        if (message.getTopicName() == null || message.getTopicName().isEmpty()) {
            throw new IllegalArgumentException("Found a message with empty or null topic name");
        }
        
        IoBuffer variableHeaderBuff = IoBuffer.allocate(2).setAutoExpand(true);
        variableHeaderBuff.put(Utils.encodeString(message.getTopicName()));
        if (message.getQos() == QOSType.LEAST_ONE || 
            message.getQos() == QOSType.EXACTLY_ONCE ) {
            if (message.getMessageID() == null) {
                throw new IllegalArgumentException("Found a message with QOS 1 or 2 and not MessageID setted");
            }
            Utils.writeWord(variableHeaderBuff, message.getMessageID());
        }
        variableHeaderBuff.put(message.getPayload());
        variableHeaderBuff.flip();
        int variableHeaderSize = variableHeaderBuff.remaining();
        
        byte flags = Utils.encodeFlags(message);
        
        IoBuffer buff = IoBuffer.allocate(2 + variableHeaderSize);
        buff.put((byte) (AbstractMessage.PUBLISH << 4 | flags));
        buff.put(Utils.encodeRemainingLength(variableHeaderSize));
        buff.put(variableHeaderBuff).flip();

        out.write(buff);
    }
    
}
