package org.dna.mqtt.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author andrea
 */
/*@RunWith(Suite.class)
@Suite.SuiteClasses({})*/
public class UtilsTest {
    
    ByteBuf m_buff;
    
    @Before
    public void setUp() { 
        m_buff = Unpooled.buffer(4);
    }

    @Test
    public void testDecodeRemainingLength() {
        //1 byte length
        m_buff.writeByte(0x0);
        assertEquals(0, Utils.decodeRemainingLenght(m_buff));
        m_buff.clear().writeByte(0x007F);
        assertEquals(127, Utils.decodeRemainingLenght(m_buff));
        
        //2 byte length
        m_buff.clear().writeBytes(new byte[]{(byte)0x80, (byte)0x01});
        assertEquals(128, Utils.decodeRemainingLenght(m_buff));
        m_buff.clear().writeBytes(new byte[]{(byte)0xFF, (byte)0x7F});
        assertEquals(16383, Utils.decodeRemainingLenght(m_buff));
        
        //3 byte length
        m_buff.clear().writeBytes(new byte[]{(byte)0x80, (byte)0x80, (byte)0x01});
        assertEquals(16384, Utils.decodeRemainingLenght(m_buff));
        m_buff.clear().writeBytes(new byte[]{(byte)0xFF, (byte)0xFF, (byte)0x7F});
        assertEquals(2097151, Utils.decodeRemainingLenght(m_buff));
        
        //4 byte length
        m_buff.clear().writeBytes(new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, (byte)0x01});
        assertEquals(2097152, Utils.decodeRemainingLenght(m_buff));
        m_buff.clear().writeBytes(new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x7F});
        assertEquals(268435455, Utils.decodeRemainingLenght(m_buff));
    }
}
