package org.dna.mqtt.moquette.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;

/**
 *
 * @author andrea
 */
public class PubCompEncoder implements MessageEncoder<PubCompMessage> {
    public void encode(IoSession session, PubCompMessage message, ProtocolEncoderOutput out) throws Exception {
        IoBuffer buff = IoBuffer.allocate(4);
        buff.put((byte) (AbstractMessage.PUBCOMP << 4));
        buff.put(Utils.encodeRemainingLength(2));
        Utils.writeWord(buff, message.getMessageID());
        buff.flip();
        out.write(buff);
    }
}
