package org.dna.mqtt.moquette.proto;

import org.dna.mqtt.moquette.proto.ConnectEncoder;
import org.dna.mqtt.moquette.proto.Utils;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.dna.mqtt.moquette.proto.TestUtils.*;

/**
 *
 * @author andrea
 */
public class ConnectEncoderTest {
    
    ConnectEncoder m_encoder;
    TestUtils.MockProtocolEncoderOutput m_mockProtoEncoder;
    
    @Before
    public void setUp() {
        m_encoder = new ConnectEncoder();
        m_mockProtoEncoder = new TestUtils.MockProtocolEncoderOutput();
    }
    
    @Test(expected=IllegalAccessException.class)
    public void testEncodeRemainingLength_invalid_upper() throws IllegalAccessException {
        Utils.encodeRemainingLength(Utils.MAX_LENGTH_LIMIT + 1);
    }
    
    @Test(expected=IllegalAccessException.class)
    public void testEncodeRemainingLength_invalid_lower() throws IllegalAccessException {
        Utils.encodeRemainingLength(-1);
    }
    
    @Test
    public void testEncodeRemainingLenght() throws IllegalAccessException {
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
    
    @Test
    public void testBaseHeader() throws Exception {
        ConnectMessage msg = new ConnectMessage();
        msg.setWillRetain(true);
        msg.setWillQos((byte)2);
        msg.setWillFlag(false);
        msg.setCleanSession(true);
        msg.setKeepAlive(512);
        
        //Exercise
        m_encoder.encode(null, msg, m_mockProtoEncoder);
        
        //Verify
        assertEquals(0x10, m_mockProtoEncoder.getBuffer().get()); //1 byte
        assertEquals(12, m_mockProtoEncoder.getBuffer().get()); //remaining length
        verifyString("MQIsdp", m_mockProtoEncoder.getBuffer());
        assertEquals(0x03, m_mockProtoEncoder.getBuffer().get()); //protocol version
        assertEquals(0x32, m_mockProtoEncoder.getBuffer().get()); //flags
        assertEquals(2, m_mockProtoEncoder.getBuffer().get()); //keepAliveTimer msb
        assertEquals(0, m_mockProtoEncoder.getBuffer().get()); //keepAliveTimer lsb
    }
    
    @Test
    public void testCompleteHeader() throws Exception {
        ConnectMessage msg = new ConnectMessage();
        msg.setWillRetain(true);
        msg.setWillQos((byte)2);
        msg.setWillFlag(true);
        msg.setCleanSession(true);
        msg.setKeepAlive(512);
        
        //variable part
        msg.setClientID("ABCDEF");
        msg.setWillTopic("Topic");
        msg.setWillMessage("Message");
        
        //Exercise
        m_encoder.encode(null, msg, m_mockProtoEncoder);
        
        //Verify
        assertEquals(0x10, m_mockProtoEncoder.getBuffer().get()); //1 byte
        assertEquals(36, m_mockProtoEncoder.getBuffer().get()); //remaining length
        verifyString("MQIsdp", m_mockProtoEncoder.getBuffer());
        assertEquals(0x03, m_mockProtoEncoder.getBuffer().get()); //protocol version
        assertEquals(0x36, m_mockProtoEncoder.getBuffer().get()); //flags
        assertEquals(2, m_mockProtoEncoder.getBuffer().get()); //keepAliveTimer msb
        assertEquals(0, m_mockProtoEncoder.getBuffer().get()); //keepAliveTimer lsb
        
        //Variable part
        verifyString("ABCDEF", m_mockProtoEncoder.getBuffer());
        verifyString("Topic", m_mockProtoEncoder.getBuffer());
        verifyString("Message", m_mockProtoEncoder.getBuffer());
    }
    
    @Test
    public void testCompleteHeaderWIthUser_password() throws Exception {
        ConnectMessage msg = new ConnectMessage();
        msg.setUserFlag(true);
        msg.setPasswordFlag(true);
        msg.setWillRetain(true);
        msg.setWillQos((byte)2);
        msg.setWillFlag(true);
        msg.setCleanSession(true);
        msg.setKeepAlive(512);
        
        //variable part
        msg.setClientID("ABCDEF");
        msg.setWillTopic("Topic");
        msg.setWillMessage("Message");
        msg.setUsername("Pablo");
        msg.setPassword("PBL");
        
        //Exercise
        m_encoder.encode(null, msg, m_mockProtoEncoder);
        
        //Verify
        assertEquals(0x10, m_mockProtoEncoder.getBuffer().get()); //1 byte
        assertEquals(48, m_mockProtoEncoder.getBuffer().get()); //remaining length
        verifyString("MQIsdp", m_mockProtoEncoder.getBuffer());
        assertEquals(0x03, m_mockProtoEncoder.getBuffer().get()); //protocol version
        assertEquals((byte)0xF6, (byte)m_mockProtoEncoder.getBuffer().get()); //flags
        assertEquals(2, m_mockProtoEncoder.getBuffer().get()); //keepAliveTimer msb
        assertEquals(0, m_mockProtoEncoder.getBuffer().get()); //keepAliveTimer lsb
        
        //Variable part
        verifyString("ABCDEF", m_mockProtoEncoder.getBuffer());
        verifyString("Topic", m_mockProtoEncoder.getBuffer());
        verifyString("Message", m_mockProtoEncoder.getBuffer());
        verifyString("Pablo", m_mockProtoEncoder.getBuffer());//username
        verifyString("PBL", m_mockProtoEncoder.getBuffer());//password
    }
    
}
