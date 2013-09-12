package org.dna.mqtt.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.SubscribeMessage;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.dna.mqtt.moquette.parser.netty.TestUtils.*;

/**
 *
 * @author andrea
 */
public class SubscribeEncoderTest {
    SubscribeEncoder m_encoder = new SubscribeEncoder();
    ChannelHandlerContext m_mockedContext;
    ByteBuf m_out;
         
    @Before
    public void setUp() {
        //mock the ChannelHandlerContext to return an UnpooledAllocator
        m_mockedContext = mockChannelHandler();
        m_out = Unpooled.buffer();
    }
    
    @Test
    public void testEncodeWithMultiTopic() throws Exception {
        SubscribeMessage msg = new SubscribeMessage();
        msg.setQos(AbstractMessage.QOSType.LEAST_ONE);
        msg.setMessageID(0xAABB);
        
        //variable part
        SubscribeMessage.Couple c1 = new SubscribeMessage.Couple((byte)1, "a/b");
        SubscribeMessage.Couple c2 = new SubscribeMessage.Couple((byte)0, "a/b/c");
        msg.addSubscription(c1);
        msg.addSubscription(c2);

        //Exercise
        m_encoder.encode(m_mockedContext, msg, m_out);

        //Verify
        assertEquals((byte)0x82, (byte)m_out.readByte()); //1 byte
        assertEquals(16, m_out.readByte()); //remaining length
        
        //verify MessageID
        assertEquals((byte)0xAA, m_out.readByte());
        assertEquals((byte)0xBB, m_out.readByte());
        
        //Variable part
        verifyString(c1.getTopic(), m_out);
        assertEquals(c1.getQos(), m_out.readByte());
        verifyString(c2.getTopic(), m_out);
        assertEquals(c2.getQos(), m_out.readByte());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEncode_empty_subscription() throws Exception {
        SubscribeMessage msg = new SubscribeMessage();
        msg.setQos(AbstractMessage.QOSType.LEAST_ONE);
        msg.setMessageID(0xAABB);

        //Exercise
        m_encoder.encode(m_mockedContext, msg, m_out);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEncode_badQos() throws Exception {
        SubscribeMessage msg = new SubscribeMessage();
        msg.setQos(AbstractMessage.QOSType.EXACTLY_ONCE);
        msg.setMessageID(0xAABB);

        //Exercise
        m_encoder.encode(m_mockedContext, msg, m_out);
    }
}
