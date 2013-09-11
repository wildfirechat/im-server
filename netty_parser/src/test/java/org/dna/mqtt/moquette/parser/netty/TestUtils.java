package org.dna.mqtt.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.UnsupportedEncodingException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Common test methods
 * 
 * @author andrea
 */
public class TestUtils {
    
    /**
     * Verify that the given bytes buffer of the given numBytes length is present
     * in the buff starting from the current position.
     */
    static void verifyBuff(int numBytes, byte[] bytes, ByteBuf buff) {
        assertTrue(numBytes <= buff.readableBytes());
        byte[] toCheck = new byte[numBytes];
        buff.readBytes(toCheck);
        
        for (int i = 0; i < numBytes; i++) {
            assertEquals(bytes[i], toCheck[i]);
        }
    }
    
    /**
     * Verify the presence of the given string starting from the current position
     * inside the buffer.
     */
    static void verifyString(String str, ByteBuf buff) throws UnsupportedEncodingException {
        ByteBuf tmpBuff = Unpooled.buffer(2);
        byte[] raw = str.getBytes("UTF-8");
        tmpBuff.writeShort(raw.length);
        tmpBuff.writeBytes(raw);
        int buffLen = raw.length + 2;
        verifyByteBuf(tmpBuff, buff.slice(buff.readerIndex(), buffLen));
        buff.skipBytes(buffLen);
    }
    
    
    static void verifyByteBuf(ByteBuf expected, ByteBuf found) {
        assertEquals(expected, found);
    }
    
}
