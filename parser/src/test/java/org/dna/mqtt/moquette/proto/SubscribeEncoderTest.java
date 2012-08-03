package org.dna.mqtt.moquette.proto;

import static org.dna.mqtt.moquette.proto.TestUtils.*;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.proto.messages.SubscribeMessage;
import org.dna.mqtt.moquette.proto.messages.SubscribeMessage.Couple;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author andrea
 */
public class SubscribeEncoderTest {

    SubscribeEncoder m_encoder;
    TestUtils.MockProtocolEncoderOutput m_mockProtoEncoder;

    @Before
    public void setUp() {
        m_encoder = new SubscribeEncoder();
        m_mockProtoEncoder = new TestUtils.MockProtocolEncoderOutput();
    }

    @Test
    public void testEncodeWithMultiTopic() throws Exception {
        SubscribeMessage msg = new SubscribeMessage();
        msg.setQos(QOSType.LEAST_ONE);
        msg.setMessageID(0xAABB);
        
        //variable part
        Couple c1 = new Couple((byte)1, "a/b");
        Couple c2 = new Couple((byte)0, "a/b/c");
        msg.addSubscription(c1);
        msg.addSubscription(c2);

        //Exercise
        m_encoder.encode(null, msg, m_mockProtoEncoder);

        //Verify
        assertEquals((byte)0x82, (byte)m_mockProtoEncoder.getBuffer().get()); //1 byte
        assertEquals(16, m_mockProtoEncoder.getBuffer().get()); //remaining length
        
        //verify M1ssageID
        assertEquals((byte)0xAA, m_mockProtoEncoder.getBuffer().get());
        assertEquals((byte)0xBB, m_mockProtoEncoder.getBuffer().get());
        
        //Variable part
        verifyString(c1.getTopic(), m_mockProtoEncoder.getBuffer());
        assertEquals(c1.getQos(), m_mockProtoEncoder.getBuffer().get());
        verifyString(c2.getTopic(), m_mockProtoEncoder.getBuffer());
        assertEquals(c2.getQos(), m_mockProtoEncoder.getBuffer().get());
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEncode_empty_subscription() throws Exception {
        SubscribeMessage msg = new SubscribeMessage();
        msg.setQos(QOSType.LEAST_ONE);
        msg.setMessageID(0xAABB);

        //Exercise
        m_encoder.encode(null, msg, m_mockProtoEncoder);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEncode_badQos() throws Exception {
        SubscribeMessage msg = new SubscribeMessage();
        msg.setQos(QOSType.EXACTLY_ONCE);
        msg.setMessageID(0xAABB);

        //Exercise
        m_encoder.encode(null, msg, m_mockProtoEncoder);
    }

}
