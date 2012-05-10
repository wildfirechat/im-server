package org.dna.mqtt.moquette.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.PubRelMessage;

/**
 *
 * @author andrea
 */
public class PubRelEncoder implements MessageEncoder<PubRelMessage> {
    public void encode(IoSession session, PubRelMessage message, ProtocolEncoderOutput out) throws Exception {
        IoBuffer buff = IoBuffer.allocate(4);
        buff.put((byte) (AbstractMessage.PUBREL << 4));
        buff.put(Utils.encodeRemainingLength(2));
        Utils.writeWord(buff, message.getMessageID());
        buff.flip();
        out.write(buff);
    }
}