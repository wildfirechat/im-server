package org.dna.mqtt.moquette.proto;

import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.UnsubscribeMessage;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author andrea
 */
public class UtilsTests {
    
    @Test
    public void testEncodeFlags() {
        UnsubscribeMessage msg = new UnsubscribeMessage();
        msg.setRetainFlag(true);
        msg.setQos(AbstractMessage.QOSType.MOST_ONE);
        
        //Exercise
        assertEquals(1, Utils.encodeFlags(msg));
    }
    
}

