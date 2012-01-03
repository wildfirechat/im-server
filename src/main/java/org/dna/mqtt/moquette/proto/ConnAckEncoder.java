package org.dna.mqtt.moquette.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.demux.MessageEncoder;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;

/**
 *
 * @author andrea
 */
public class ConnAckEncoder implements MessageEncoder<ConnAckMessage> {

    public void encode(IoSession session, ConnAckMessage message, ProtocolEncoderOutput out) throws Exception {
        IoBuffer buff = IoBuffer.allocate(4);
        buff.put((byte) (AbstractMessage.CONNACK << 4));
        buff.put(Utils.encodeRemainingLength(2));
        buff.put((byte) 0);
        buff.put(message.getReturnCode()).flip();
        out.write(buff);
    }
}
