package org.dna.mqtt.moquette.proto;

import org.dna.mqtt.moquette.proto.ConnAckEncoder;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author andrea
 */
public class ConnAckEncoderTest {
    
    @Test
    public void testHeaderEncode() throws Exception {
        TestUtils.MockProtocolEncoderOutput mockProtoEncoder = new TestUtils.MockProtocolEncoderOutput();
        ConnAckEncoder encoder = new ConnAckEncoder();
        ConnAckMessage msg = new ConnAckMessage();
        
        //Exercise
        encoder.encode(null, msg, mockProtoEncoder);
        
        //Verify
        assertEquals(0x20, mockProtoEncoder.getBuffer().get()); //1 byte
        assertEquals(0x02, mockProtoEncoder.getBuffer().get()); //2 byte, length
        assertEquals(ConnAckMessage.CONNECTION_ACCEPTED, mockProtoEncoder.getBuffer().skip(1).get());
    }
}
