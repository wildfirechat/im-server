package org.dna.mqtt.moquette.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.SubAckMessage;

/**
 *
 * @author andrea
 */
public class SubAckEncoder implements MessageEncoder<SubAckMessage> {

    public void encode(IoSession session, SubAckMessage message, ProtocolEncoderOutput out) throws Exception {
        if (message.types().isEmpty()) {
            throw new IllegalArgumentException("Found a suback message with empty topics");
        }

        IoBuffer variableHeaderBuff = IoBuffer.allocate(4).setAutoExpand(true);
        Utils.writeWord(variableHeaderBuff, message.getMessageID());
        for (AbstractMessage.QOSType c : message.types()) {
            byte ord = (byte) c.ordinal();
            variableHeaderBuff.put((byte)c.ordinal());
        }

        variableHeaderBuff.flip();
        int variableHeaderSize = variableHeaderBuff.remaining();
        IoBuffer buff = IoBuffer.allocate(2 + variableHeaderSize);

        buff.put((byte) (AbstractMessage.SUBACK << 4 ));
        buff.put(Utils.encodeRemainingLength(variableHeaderSize));
        buff.put(variableHeaderBuff).flip();

        out.write(buff);
    }

}
