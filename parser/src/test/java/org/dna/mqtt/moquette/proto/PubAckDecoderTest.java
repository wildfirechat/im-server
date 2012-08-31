package org.dna.mqtt.moquette.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.demux.MessageDecoder;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.dna.mqtt.moquette.proto.TestUtils.MockProtocolDecoderOutput;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.PubAckMessage;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author andrea
 */
public class PubAckDecoderTest {
    IoBuffer m_buff;
    PubAckDecoder m_msgdec;
    MockProtocolDecoderOutput<PubAckMessage> m_mockProtoDecoder;
    
    @Before
    public void setUp() {
        m_buff = IoBuffer.allocate(4);
        m_msgdec = new PubAckDecoder();
        m_mockProtoDecoder = new MockProtocolDecoderOutput<PubAckMessage>();
    }
    
    @Test
    public void testHeader() throws Exception {
        m_buff = IoBuffer.allocate(14);
        int messageId = 0xAABB;
        initHeader(m_buff, messageId);
        m_buff.flip();
        
        //Exercise
        MessageDecoderResult res = m_msgdec.decode(null, m_buff, m_mockProtoDecoder);
        
        assertNotNull(m_mockProtoDecoder.getMessage());
        assertEquals(MessageDecoder.OK, res);
        assertEquals(messageId, m_mockProtoDecoder.getMessage().getMessageID().intValue());
        assertEquals(AbstractMessage.PUBACK, m_mockProtoDecoder.getMessage().getMessageType());
    }
    
    private void initHeader(IoBuffer buff, int messageID) {
        buff.clear().put((byte)(AbstractMessage.PUBACK << 4)).put((byte)2);
        
        //return code
        Utils.writeWord(buff, messageID);
    }
}
