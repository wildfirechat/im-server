package org.dna.mqtt.moquette.proto;

import org.dna.mqtt.moquette.proto.UnsubscribeEncoder;
import org.dna.mqtt.moquette.proto.messages.UnsubscribeMessage;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.dna.mqtt.moquette.proto.TestUtils.*;

/**
 *
 * @author andrea
 */
public class UnsubscribeEncoderTest {
    UnsubscribeMessage m_msg;
    UnsubscribeEncoder m_encoder;
    TestUtils.MockProtocolEncoderOutput m_mockProtoEncoder;

    @Before
    public void setUp() {
        m_encoder = new UnsubscribeEncoder();
        m_mockProtoEncoder = new TestUtils.MockProtocolEncoderOutput();
        m_msg = new UnsubscribeMessage();
        m_msg.setMessageID(0xAABB);
    }
    
    @Test
    public void testEncodeWithSingleTopic() throws Exception {
        m_msg.setQos(QOSType.LEAST_ONE);
        
        //variable part
        String topic1 = "/topic";
        m_msg.addTopic(topic1);

        //Exercise
        m_encoder.encode(null, m_msg, m_mockProtoEncoder);
        
        //Verify
        assertEquals((byte)0xA2, (byte)m_mockProtoEncoder.getBuffer().get()); //1 byte
        //2 messageID + 2 length + 6 chars = 10
        assertEquals(10, m_mockProtoEncoder.getBuffer().get()); //remaining length
        
        //verify M1ssageID
        assertEquals((byte)0xAA, m_mockProtoEncoder.getBuffer().get());
        assertEquals((byte)0xBB, m_mockProtoEncoder.getBuffer().get());
        
        //Variable part
        verifyString(topic1, m_mockProtoEncoder.getBuffer());
    }
    

    @Test
    public void testEncodeWithMultiTopic() throws Exception {
        m_msg.setQos(QOSType.LEAST_ONE);
        
        //variable part
        String topic1 = "a/b";
        String topic2 = "a/b/c";
        m_msg.addTopic(topic1);
        m_msg.addTopic(topic2);

        //Exercise
        m_encoder.encode(null, m_msg, m_mockProtoEncoder);

        //Verify
        assertEquals((byte)0xA2, (byte)m_mockProtoEncoder.getBuffer().get()); //1 byte
        assertEquals(14, m_mockProtoEncoder.getBuffer().get()); //remaining length
        
        //verify M1ssageID
        assertEquals((byte)0xAA, m_mockProtoEncoder.getBuffer().get());
        assertEquals((byte)0xBB, m_mockProtoEncoder.getBuffer().get());
        
        //Variable part
        verifyString(topic1, m_mockProtoEncoder.getBuffer());
        verifyString(topic2, m_mockProtoEncoder.getBuffer());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEncode_empty_topics() throws Exception {
        m_msg.setQos(QOSType.LEAST_ONE);

        //Exercise
        m_encoder.encode(null, m_msg, m_mockProtoEncoder);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEncode_badQos() throws Exception {
        m_msg.setQos(QOSType.EXACTLY_ONCE);

        //Exercise
        m_encoder.encode(null, m_msg, m_mockProtoEncoder);
    }

}
