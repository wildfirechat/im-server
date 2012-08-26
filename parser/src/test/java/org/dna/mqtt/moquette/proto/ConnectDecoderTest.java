package org.dna.mqtt.moquette.proto;

import java.io.UnsupportedEncodingException;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.demux.MessageDecoder;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.dna.mqtt.moquette.proto.TestUtils.MockProtocolDecoderOutput;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class ConnectDecoderTest {
    
    IoBuffer m_buff;
    ConnectDecoder m_msgdec;
    MockProtocolDecoderOutput<ConnectMessage> m_mockProtoDecoder;
    
    @Before
    public void setUp() {
        m_buff = IoBuffer.allocate(4);
        m_msgdec = new ConnectDecoder();
        m_mockProtoDecoder = new MockProtocolDecoderOutput<ConnectMessage>();
    }
    
    @Test
    public void testDecodable_OK() {
        m_buff.put((byte)(AbstractMessage.CONNECT << 4))
                .put((byte)0) //0 length
                .flip();
        assertEquals(MessageDecoder.OK , m_msgdec.decodable(null, m_buff));
    }
    
    @Test
    public void testDecodable_NOT_OK() {
        m_buff.put((byte)(AbstractMessage.CONNACK << 4))
                .put((byte)0) //0 length
                .flip();
        assertEquals(MessageDecoder.NOT_OK , m_msgdec.decodable(null, m_buff));
    }

    @Test
    public void testDecodable_NEED_DATA() {
        m_buff = IoBuffer.allocate(0);
        assertFalse(m_buff.hasRemaining());
        assertEquals(MessageDecoder.NEED_DATA , m_msgdec.decodable(null, m_buff));
    }
    
    @Test
    public void testDecodeRemainingLenght() {
        //1 byte length
        m_buff.put((byte)0x0).flip();
        assertEquals(0, Utils.decodeRemainingLenght(m_buff));
        m_buff.clear().put((byte) 0x007F).flip();
        assertEquals(127, Utils.decodeRemainingLenght(m_buff));
        
        //2 byte length
        m_buff.clear().put((byte) 0x80).put((byte) 0x01).flip();
        assertEquals(128, Utils.decodeRemainingLenght(m_buff));
        m_buff.clear().put((byte) 0xFF).put((byte) 0x7F).flip();
        assertEquals(16383, Utils.decodeRemainingLenght(m_buff));
        
        //3 byte length
        m_buff.clear().put((byte) 0x80).put((byte) 0x80).put((byte)0x01).flip();
        assertEquals(16384, Utils.decodeRemainingLenght(m_buff));
        m_buff.clear().put((byte) 0xFF).put((byte) 0xFF).put((byte)0x7F).flip();
        assertEquals(2097151, Utils.decodeRemainingLenght(m_buff));
        
        //4 byte length
        m_buff.clear().put((byte) 0x80).put((byte) 0x80).put((byte)0x80).put((byte)0x01).flip();
        assertEquals(2097152, Utils.decodeRemainingLenght(m_buff));
        m_buff.clear().put((byte) 0xFF).put((byte) 0xFF).put((byte) 0xFF).put((byte)0x7F).flip();
        assertEquals(268435455, Utils.decodeRemainingLenght(m_buff));
    }
    
    @Test
    public void testBaseHeader() throws Exception {
        m_buff = IoBuffer.allocate(14);
        initBaseHeader(m_buff);
        m_buff.flip();
        
        //Excercise
        m_msgdec.decode(null, m_buff, m_mockProtoDecoder);
        
        //Verify
        verifyBaseHeader(m_mockProtoDecoder.getMessage());
    }
    
    @Test
    public void testBaseHeader_ClientID() throws UnsupportedEncodingException, Exception {
        m_buff = IoBuffer.allocate(40);
        initHeader(m_buff, (byte) 38);
        encodeString(m_buff, "ABCDEFGH");
        encodeString(m_buff, "Topic");
        encodeString(m_buff, "Message");
        m_buff.flip();
        
        //Excercise
        m_msgdec.decode(null, m_buff, m_mockProtoDecoder);
        
        //Verify
        verifyBaseHeader(m_mockProtoDecoder.getMessage());
        assertEquals("ABCDEFGH", m_mockProtoDecoder.getMessage().getClientID());
        assertEquals("Topic", m_mockProtoDecoder.getMessage().getWillTopic());
        assertEquals("Message", m_mockProtoDecoder.getMessage().getWillMessage());
    }
    
    @Test
    public void testBaseHeader_extra_with_user_pwd() throws UnsupportedEncodingException, Exception {
        m_buff = IoBuffer.allocate(55);
        initHeader(m_buff, (byte) 53);
        encodeString(m_buff, "ABCDEFGH");
        encodeString(m_buff, "Topic");
        encodeString(m_buff, "Message");
        encodeString(m_buff, "Fakeuser");
        encodeString(m_buff, "pwd");
        m_buff.flip();
        
        //Excercise
        m_msgdec.decode(null, m_buff, m_mockProtoDecoder);
        
        //Verify
        verifyBaseHeader(m_mockProtoDecoder.getMessage());
        assertEquals(AbstractMessage.CONNECT, m_mockProtoDecoder.getMessage().getMessageType());
        assertEquals("ABCDEFGH", m_mockProtoDecoder.getMessage().getClientID());
        assertEquals("Topic", m_mockProtoDecoder.getMessage().getWillTopic());
        assertEquals("Message", m_mockProtoDecoder.getMessage().getWillMessage());
        assertEquals("Fakeuser", m_mockProtoDecoder.getMessage().getUsername());
        assertEquals("pwd", m_mockProtoDecoder.getMessage().getPassword());
    }
    
    @Test
    public void testBadFlagUserPwd() throws UnsupportedEncodingException, Exception {
        m_buff = IoBuffer.allocate(14);
        m_buff.clear().put((byte)(AbstractMessage.CONNECT << 4)).put((byte)12);
        //Proto name
        encodeString(m_buff, "MQIsdp");
        //version
        m_buff.put((byte)3);
        //conn flags
        m_buff.put((byte)0x4E); //sets user to false and password to true
        //keepAlive
        m_buff.put((byte)0).put((byte) 0x0A);
        m_buff.flip();
        
        //Excercise
        MessageDecoderResult res = m_msgdec.decode(null, m_buff, m_mockProtoDecoder);
        
        assertNull(m_mockProtoDecoder.getMessage());
        assertEquals(MessageDecoder.NOT_OK, res);
    }
    
    /**
     * Encode and insert the given stirng into the given buff
     */
    private IoBuffer encodeString(IoBuffer buff, String str) throws UnsupportedEncodingException {
        buff.put(Utils.encodeString(str));
        return buff;
    }
    
    private void initBaseHeader(IoBuffer buff) throws UnsupportedEncodingException {
        initHeader(buff, (byte)0x0C);
    }
    
    private void initHeader(IoBuffer buff, byte remaingLength) throws UnsupportedEncodingException {
        buff.clear().put((byte)(AbstractMessage.CONNECT << 4)).put(remaingLength);
        //Proto name
        encodeString(buff, "MQIsdp");
        //version
        buff.put((byte)3);
        //conn flags
        buff.put((byte)0xCE);
        //keepAlive
        buff.put((byte)0).put((byte) 0x0A);
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
