package org.dna.mqtt.moquette.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.proto.messages.SubscribeMessage;
import org.dna.mqtt.moquette.proto.messages.SubscribeMessage.Couple;

/**
 *
 * @author andrea
 */
public class SubscribeEncoder implements MessageEncoder<SubscribeMessage> {

    public void encode(IoSession session, SubscribeMessage message, ProtocolEncoderOutput out) throws Exception {
        if (message.subscriptions().isEmpty()) {
            throw new IllegalArgumentException("Found a subscribe message with empty topics");
        }

        if (message.getQos() != QOSType.LEAST_ONE) {
            throw new IllegalArgumentException("Expected a message with QOS 1, found " + message.getQos());
        }
        
        IoBuffer variableHeaderBuff = IoBuffer.allocate(4).setAutoExpand(true);
        Utils.writeWord(variableHeaderBuff, message.getMessageID());
        for (Couple c : message.subscriptions()) {
            variableHeaderBuff.put(Utils.encodeString(c.getTopic()));
            variableHeaderBuff.put(c.getQos());
        }
        
        variableHeaderBuff.flip();
        int variableHeaderSize = variableHeaderBuff.remaining();
        byte flags = Utils.encodeFlags(message);
        IoBuffer buff = IoBuffer.allocate(2 + variableHeaderSize);

        buff.put((byte) (AbstractMessage.SUBSCRIBE << 4 | flags));
        buff.put(Utils.encodeRemainingLength(variableHeaderSize));
        buff.put(variableHeaderBuff).flip();

        out.write(buff);
    }
}
