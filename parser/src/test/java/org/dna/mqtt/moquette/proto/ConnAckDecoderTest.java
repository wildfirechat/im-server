package org.dna.mqtt.moquette.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.demux.MessageDecoder;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.dna.mqtt.moquette.proto.TestUtils.MockProtocolDecoderOutput;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
/**
 *
 * @author andrea
 */
public class ConnAckDecoderTest {
    IoBuffer m_buff;
    ConnAckDecoder m_msgdec;
    MockProtocolDecoderOutput<ConnAckMessage> m_mockProtoDecoder;
    
    @Before
    public void setUp() {
        m_buff = IoBuffer.allocate(4);
        m_msgdec = new ConnAckDecoder();
        m_mockProtoDecoder = new MockProtocolDecoderOutput<ConnAckMessage>();
    }
    
    @Test
    public void testDecodable_OK() {
        m_buff.put((byte)(AbstractMessage.CONNACK << 4))
                .put((byte)0) //0 length
                .flip();
        assertEquals(MessageDecoder.OK , m_msgdec.decodable(null, m_buff));
    }
    
    @Test
    public void testDecodable_NOT_OK() {
        m_buff.put((byte)(AbstractMessage.CONNECT << 4))
                .put((byte)0) //0 length
                .flip();
        assertEquals(MessageDecoder.NOT_OK , m_msgdec.decodable(null, m_buff));
    }
    
    @Test
    public void testHeader() throws Exception {
        m_buff = IoBuffer.allocate(14);
        initHeader(m_buff);
        m_buff.flip();
        
        //Exercise
        MessageDecoderResult res = m_msgdec.decode(null, m_buff, m_mockProtoDecoder);
        
        assertNotNull(m_mockProtoDecoder.getMessage());
        assertEquals(MessageDecoder.OK, res);
        assertEquals(ConnAckMessage.CONNECTION_ACCEPTED, m_mockProtoDecoder.getMessage().getReturnCode());
        assertEquals(AbstractMessage.CONNACK, m_mockProtoDecoder.getMessage().getMessageType());
    }
    
    private void initHeader(IoBuffer buff) {
        buff.clear().put((byte)(AbstractMessage.CONNACK << 4)).put((byte)2);
        //reserved
        buff.put((byte)0);
        //return code
        buff.put(ConnAckMessage.CONNECTION_ACCEPTED);
    }
    
}
