package org.dna.mqtt.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.dna.mqtt.moquette.parser.netty.TestUtils.*;


/**
 *
 * @author andrea
 */
public class MQTTEncoderTest {
    MQTTEncoder m_encoder = new MQTTEncoder();
    ChannelHandlerContext m_mockedContext;
         
    @Before
    public void setUp() {
        //mock the ChannelHandlerContext to return an UnpooledAllocator
        m_mockedContext = mock(ChannelHandlerContext.class);
        ByteBufAllocator allocator = UnpooledByteBufAllocator.DEFAULT;
        when(m_mockedContext.alloc()).thenReturn(allocator);
    }
    
    @Test
    public void testBaseHeader() throws Exception {
        ConnectMessage msg = new ConnectMessage();
        msg.setWillRetain(true);
        msg.setWillQos((byte)2);
        msg.setWillFlag(false);
        msg.setCleanSession(true);
        msg.setKeepAlive(512);
        //mock the ChannelHandlerContext to return an UnpooledAllocator
        /*ChannelHandlerContext mockedContext = mock(ChannelHandlerContext.class);
        ByteBufAllocator allocator = UnpooledByteBufAllocator.DEFAULT;
        when(mockedContext.alloc()).thenReturn(allocator);*/
        ByteBuf out = Unpooled.buffer();
        
        //Exercise
        m_encoder.encode(m_mockedContext, msg, out);
        
        //Verify
        assertEquals(0x10, out.readByte()); //1 byte
        assertEquals(12, out.readByte()); //remaining length
        verifyString("MQIsdp", out);
        assertEquals(0x03, out.readByte()); //protocol version
        assertEquals(0x32, out.readByte()); //flags
        assertEquals(2, out.readByte()); //keepAliveTimer msb
        assertEquals(0, out.readByte()); //keepAliveTimer lsb
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
        
        ByteBuf out = Unpooled.buffer();
        
        //Exercise
        m_encoder.encode(m_mockedContext, msg, out);
        
        //Verify
        assertEquals(0x10, out.readByte()); //1 byte
        assertEquals(36, out.readByte()); //remaining length
        verifyString("MQIsdp", out);
        assertEquals(0x03, out.readByte()); //protocol version
        assertEquals(0x36, out.readByte()); //flags
        assertEquals(2, out.readByte()); //keepAliveTimer msb
        assertEquals(0, out.readByte()); //keepAliveTimer lsb
        
        //Variable part
        verifyString("ABCDEF", out);
        verifyString("Topic", out);
        verifyString("Message", out);
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
        
        ByteBuf out = Unpooled.buffer();
        
        //Exercise
        m_encoder.encode(m_mockedContext, msg, out);
        
        //Verify
        assertEquals(0x10, out.readByte()); //1 byte
        assertEquals(48, out.readByte()); //remaining length
        verifyString("MQIsdp", out);
        assertEquals(0x03, out.readByte()); //protocol version
        assertEquals((byte)0xF6, (byte)out.readByte()); //flags
        assertEquals(2, out.readByte()); //keepAliveTimer msb
        assertEquals(0, out.readByte()); //keepAliveTimer lsb
        
        //Variable part
        verifyString("ABCDEF", out);
        verifyString("Topic", out);
        verifyString("Message", out);
        verifyString("Pablo", out);//username
        verifyString("PBL", out);//password
    }
}
