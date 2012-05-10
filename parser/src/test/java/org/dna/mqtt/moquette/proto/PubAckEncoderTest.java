package org.dna.mqtt.moquette.proto;

import org.dna.mqtt.moquette.proto.PubAckEncoder;
import org.dna.mqtt.moquette.proto.messages.PubAckMessage;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 *
 * @author andrea
 */
public class PubAckEncoderTest {
    
    @Test
    public void testHeaderEncode() throws Exception {
        int messageID = 0xAABB;
        TestUtils.MockProtocolEncoderOutput mockProtoEncoder = new TestUtils.MockProtocolEncoderOutput();
        PubAckEncoder encoder = new PubAckEncoder();
        PubAckMessage msg = new PubAckMessage();
        msg.setMessageID(messageID);
        
        //Exercise
        encoder.encode(null, msg, mockProtoEncoder);
        
        //Verify
        assertEquals(0x40, mockProtoEncoder.getBuffer().get()); //1 byte
        assertEquals(0x02, mockProtoEncoder.getBuffer().get()); //2 byte, length
        assertEquals((byte)0xAA, mockProtoEncoder.getBuffer().get());
        assertEquals((byte)0xBB, mockProtoEncoder.getBuffer().get());
    }
    
}
