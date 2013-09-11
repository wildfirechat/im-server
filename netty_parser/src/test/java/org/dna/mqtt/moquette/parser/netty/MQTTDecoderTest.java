package org.dna.mqtt.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;


public class MQTTDecoderTest {
    
    ByteBuf m_buff;
    MQTTDecoder m_msgdec;
    
    @Before
    public void setUp() {
        m_msgdec = new MQTTDecoder();
    }
    
    @Test
    public void testBaseHeader() throws Exception {
        m_buff = Unpooled.buffer(14);
        initBaseHeader(m_buff);
        //m_buff.flip();
        List<Object> results = new ArrayList<Object >();
        
        //Excercise
        m_msgdec.decode(null, m_buff, results);
        
        //Verify
        assertFalse(results.isEmpty());
        verifyBaseHeader((ConnectMessage)results.get(0));
    }
    
    
    private void initBaseHeader(ByteBuf buff) throws UnsupportedEncodingException {
        initHeader(buff, (byte)0x0C);
    }
    
    private void initHeader(ByteBuf buff, byte remaingLength) throws UnsupportedEncodingException {
        buff.clear().writeByte((byte)(AbstractMessage.CONNECT << 4)).writeByte(remaingLength);
        //Proto name
        encodeString(buff, "MQIsdp");
        //version
        buff.writeByte(3);
        //conn flags
        buff.writeByte(0xCE);
        //keepAlive
        buff.writeBytes(new byte[]{(byte)0, (byte) 0x0A});
    }
    
    /**
     * Encode and insert the given string into the given buff
     */
    private ByteBuf encodeString(ByteBuf buff, String str) throws UnsupportedEncodingException {
        buff.writeBytes(Utils.encodeString(str));
        return buff;
    }
    
    private void verifyBaseHeader(ConnectMessage connMessage) {
        assertNotNull(connMessage);
        assertEquals("MQIsdp", connMessage.getProtocolName());
        assertEquals(3, connMessage.getProcotolVersion());
        assertTrue(connMessage.isUserFlag());
        assertTrue(connMessage.isPasswordFlag());
        assertTrue(connMessage.isCleanSession());
        assertEquals(10, connMessage.getKeepAlive());
        assertTrue(connMessage.isWillFlag());
        assertFalse(connMessage.isWillRetain());
        assertEquals(1, connMessage.getWillQos());
    }
}