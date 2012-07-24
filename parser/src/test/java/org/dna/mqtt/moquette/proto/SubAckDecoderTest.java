package org.dna.mqtt.moquette.proto;

import java.util.List;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.dna.mqtt.moquette.proto.TestUtils.MockProtocolDecoderOutput;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.proto.messages.SubAckMessage;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author andrea
 */
public class SubAckDecoderTest {

    IoBuffer m_buff;
    SubAckDecoder m_msgdec;
    MockProtocolDecoderOutput<SubAckMessage> m_mockProtoDecoder;

    @Before
    public void setUp() {
        m_buff = IoBuffer.allocate(7);
        m_msgdec = new SubAckDecoder();
        m_mockProtoDecoder = new MockProtocolDecoderOutput<SubAckMessage>();
    }

    @Test
    public void testBadQos() throws Exception {
        initHeaderQos(m_buff, 0xAABB, QOSType.LEAST_ONE, QOSType.MOST_ONE, QOSType.MOST_ONE);
        m_buff.flip();

        //Excercise
        MessageDecoderResult res = m_msgdec.decode(null, m_buff, m_mockProtoDecoder);

        //Verify
        assertEquals(MessageDecoderResult.OK, res);
        assertEquals(0xAABB, m_mockProtoDecoder.getMessage().getMessageID());
        List<QOSType> qoses = m_mockProtoDecoder.getMessage().types();
        assertEquals(3, qoses.size());
        assertEquals(QOSType.LEAST_ONE, qoses.get(0));
        assertEquals(QOSType.MOST_ONE, qoses.get(1));
        assertEquals(QOSType.MOST_ONE, qoses.get(2));
        assertEquals(AbstractMessage.SUBACK, m_mockProtoDecoder.getMessage().getMessageType());
    }

    private void initHeaderQos(IoBuffer buff, int messageID, QOSType... qoss) throws IllegalAccessException {
        buff.clear().put((byte) (AbstractMessage.SUBACK << 4)).
                put(Utils.encodeRemainingLength(3));
        
        Utils.writeWord(buff, messageID);
        for (QOSType qos : qoss) {
            buff.put((byte)qos.ordinal());
        }
    }
}
