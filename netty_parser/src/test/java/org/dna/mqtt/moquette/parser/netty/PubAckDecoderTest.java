package org.dna.mqtt.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.PubAckMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author andrea
 */
public class PubAckDecoderTest {
    ByteBuf m_buff;
    PubAckDecoder m_msgdec;
    
    @Before
    public void setUp() {
        m_msgdec = new PubAckDecoder();
    }
    
    @Test
    public void testHeader() throws Exception {
        m_buff = Unpooled.buffer(14);
        int messageId = 0xAABB;
        initHeader(m_buff, messageId);
        List<Object> results = new ArrayList<Object >();
        
        //Excercise
        m_msgdec.decode(null, m_buff, results);
        
        assertFalse(results.isEmpty());
        PubAckMessage message = (PubAckMessage)results.get(0); 
        assertNotNull(message);
        assertEquals(messageId, message.getMessageID().intValue());
        assertEquals(AbstractMessage.PUBACK, message.getMessageType());
    }
    
    private void initHeader(ByteBuf buff, int messageID) {
        buff.clear().writeByte(AbstractMessage.PUBACK << 4).writeByte(2);
        
        //return code
        buff.writeShort(messageID);
    }
}
