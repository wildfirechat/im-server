package org.dna.mqtt.moquette.proto;

import java.util.List;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.demux.MessageDecoder;
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
        assertEquals(0xAABB, m_mockProtoDecoder.getMessage().getMessageID().intValue());
        List<QOSType> qoses = m_mockProtoDecoder.getMessage().types();
        assertEquals(3, qoses.size());
        assertEquals(QOSType.LEAST_ONE, qoses.get(0));
        assertEquals(QOSType.MOST_ONE, qoses.get(1));
        assertEquals(QOSType.MOST_ONE, qoses.get(2));
        assertEquals(AbstractMessage.SUBACK, m_mockProtoDecoder.getMessage().getMessageType());
    }
    
    
    @Test
    public void testBugBadRemainingCalculation() throws Exception {
        byte[] overallMessage = new byte[] {(byte)0x90, 0x03, //fixed header
             0x00, 0x0A, //MSG ID
             0x01}; //QoS array
         m_buff = IoBuffer.allocate(overallMessage.length).setAutoExpand(true);
         m_buff.put(overallMessage);
         m_buff.flip();
         
         //Exercise
        MessageDecoderResult res = m_msgdec.decode(null, m_buff, m_mockProtoDecoder);

        assertNotNull(m_mockProtoDecoder.getMessage());
        assertEquals(MessageDecoder.OK, res);
        SubAckMessage message = (SubAckMessage) m_mockProtoDecoder.getMessage();
        assertEquals(0x0A, message.getMessageID().intValue());
        assertEquals(1, message.types().size());
        assertEquals(AbstractMessage.QOSType.LEAST_ONE, message.types().get(0));
    }

    private void initHeaderQos(IoBuffer buff, int messageID, QOSType... qoss) throws IllegalAccessException {
        buff.clear().put((byte) (AbstractMessage.SUBACK << 4)).
                put(Utils.encodeRemainingLength(2 + qoss.length));
        
        Utils.writeWord(buff, messageID);
        for (QOSType qos : qoss) {
            buff.put((byte)qos.ordinal());
        }
    }
}
