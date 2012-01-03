package org.dna.mqtt.moquette.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.PingReqMessage;

/**
 *
 * @author andrea
 */
public class PingReqEncoder implements MessageEncoder<PingReqMessage> {

    public void encode(IoSession session, PingReqMessage message, ProtocolEncoderOutput out) throws Exception {
        IoBuffer buff = IoBuffer.allocate(2);
        buff.put((byte) (AbstractMessage.PINGREQ << 4)).put((byte)0).flip();
        out.write(buff);
    }
}
