package org.dna.mqtt.moquette.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.proto.messages.UnsubscribeMessage;

/**
 *
 * @author andrea
 */
public class UnsubscribeEncoder implements MessageEncoder<UnsubscribeMessage> {

    public void encode(IoSession session, UnsubscribeMessage message, ProtocolEncoderOutput out) throws Exception {
        if (message.topics().isEmpty()) {
            throw new IllegalArgumentException("Found an unsubscribe message with empty topics");
        }

        if (message.getQos() != QOSType.LEAST_ONE) {
            throw new IllegalArgumentException("Expected a message with QOS 1, found " + message.getQos());
        }
        
        IoBuffer variableHeaderBuff = IoBuffer.allocate(4).setAutoExpand(true);
        Utils.writeWord(variableHeaderBuff, message.getMessageID());
        for (String topic : message.topics()) {
            variableHeaderBuff.put(Utils.encodeString(topic));
        }
        
        variableHeaderBuff.flip();
        int variableHeaderSize = variableHeaderBuff.remaining();
        byte flags = Utils.encodeFlags(message);
        IoBuffer buff = IoBuffer.allocate(2 + variableHeaderSize);

        buff.put((byte) (AbstractMessage.UNSUBSCRIBE << 4 | flags));
        buff.put(Utils.encodeRemainingLength(variableHeaderSize));
        buff.put(variableHeaderBuff).flip();

        out.write(buff);
    }
}
