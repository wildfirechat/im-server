package org.dna.mqtt.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.CorruptedFrameException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.dna.mqtt.moquette.parser.netty.TestUtils.*;

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
    
    @Test(expected=CorruptedFrameException.class)
    public void testEncodeRemainingLength_invalid_upper() {
        Utils.encodeRemainingLength(Utils.MAX_LENGTH_LIMIT + 1);
    }
    
    @Test(expected=CorruptedFrameException.class)
    public void testEncodeRemainingLength_invalid_lower() {
        Utils.encodeRemainingLength(-1);
    }
    
    @Test
    public void testEncodeRemainingLenght() {
        //1 byte length
        verifyBuff(1, new byte[]{0}, Utils.encodeRemainingLength(0));
        verifyBuff(1, new byte[]{0x7F}, Utils.encodeRemainingLength(127));
        
        //2 byte length
        verifyBuff(2, new byte[]{(byte)0x80, 0x01}, Utils.encodeRemainingLength(128));
        verifyBuff(2, new byte[]{(byte)0xFF, 0x7F}, Utils.encodeRemainingLength(16383));
        
        //3 byte length
        verifyBuff(3, new byte[]{(byte)0x80, (byte)0x80, 0x01}, Utils.encodeRemainingLength(16384));
        verifyBuff(3, new byte[]{(byte)0xFF, (byte)0xFF, 0x7F}, Utils.encodeRemainingLength(2097151));
        
        //4 byte length
        verifyBuff(4, new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, 0x01}, Utils.encodeRemainingLength(2097152));
        verifyBuff(4, new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, 0x7F}, Utils.encodeRemainingLength(268435455));
    }
}
