package org.dna.mqtt.moquette.proto;

import org.dna.mqtt.moquette.proto.SubAckEncoder;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.proto.messages.SubAckMessage;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author andrea
 */
public class SubAckEncoderTest {
    SubAckEncoder m_encoder;
    TestUtils.MockProtocolEncoderOutput m_mockProtoEncoder;

    @Before
    public void setUp() {
        m_encoder = new SubAckEncoder();
        m_mockProtoEncoder = new TestUtils.MockProtocolEncoderOutput();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEncodeWithNoQoss() throws Exception {
        SubAckMessage msg = new SubAckMessage();
        msg.setMessageID(123);

        //Exercise
        m_encoder.encode(null, msg, m_mockProtoEncoder);
    }

    @Test
    public void testEncodeWithMultipleQos() throws Exception {
        SubAckMessage msg = new SubAckMessage();

        int messageID = 0xAABB;
        msg.setMessageID(messageID);
        msg.addType(QOSType.MOST_ONE);
        msg.addType(QOSType.LEAST_ONE);
        msg.addType(QOSType.EXACTLY_ONCE);
        
        //Exercise
        m_encoder.encode(null, msg, m_mockProtoEncoder);

        //Verify
        assertEquals((byte) (AbstractMessage.SUBACK << 4 ), m_mockProtoEncoder.getBuffer().get()); //1 byte
        assertEquals(5, m_mockProtoEncoder.getBuffer().get()); //remaining length

        //Variable part
        assertEquals((byte)0xAA, m_mockProtoEncoder.getBuffer().get()); //MessageID MSB
        assertEquals((byte)0xBB, m_mockProtoEncoder.getBuffer().get()); //MessageID LSB
        assertEquals((byte)QOSType.MOST_ONE.ordinal(), m_mockProtoEncoder.getBuffer().get());
        assertEquals((byte)QOSType.LEAST_ONE.ordinal(), m_mockProtoEncoder.getBuffer().get());
        assertEquals((byte)QOSType.EXACTLY_ONCE.ordinal(), m_mockProtoEncoder.getBuffer().get());
    }
}
