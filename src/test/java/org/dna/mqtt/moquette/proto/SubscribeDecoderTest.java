package org.dna.mqtt.moquette.proto;

import org.dna.mqtt.moquette.proto.SubscribeDecoder;
import org.dna.mqtt.moquette.proto.Utils;
import org.dna.mqtt.moquette.proto.messages.SubscribeMessage.Couple;
import org.dna.mqtt.moquette.proto.messages.SubscribeMessage;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.dna.mqtt.moquette.proto.TestUtils.MockProtocolDecoderOutput;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author andrea
 */
public class SubscribeDecoderTest {

    IoBuffer m_buff;
    SubscribeDecoder m_msgdec;
    MockProtocolDecoderOutput<SubscribeMessage> m_mockProtoDecoder;

    @Before
    public void setUp() {
        m_buff = IoBuffer.allocate(4);
        m_msgdec = new SubscribeDecoder();
        m_mockProtoDecoder = new MockProtocolDecoderOutput<SubscribeMessage>();
    }

    @Test
    public void testBadQos() throws Exception {
        m_buff = IoBuffer.allocate(2);
        initHeaderBadQos(m_buff);
        m_buff.flip();

        //Excercise
        MessageDecoderResult res = m_msgdec.decode(null, m_buff, m_mockProtoDecoder);

        //Verify
        assertEquals(MessageDecoderResult.NOT_OK, res);
    }
    
    @Test
    public void testMultiTopic() throws Exception {
        m_buff = IoBuffer.allocate(4).setAutoExpand(true);
        Couple c1 = new Couple((byte)2, "a/b");
        Couple c2 = new Couple((byte)1, "c/d/e");
        initMultiTopic(m_buff, 123, c1, c2);
        m_buff.flip();
        
        //Excercise
        MessageDecoderResult res = m_msgdec.decode(null, m_buff, m_mockProtoDecoder);

        //Verify
        assertEquals(MessageDecoderResult.OK, res);
        assertEquals(2, m_mockProtoDecoder.getMessage().subscriptions().size());
    }
    
    private void initHeaderBadQos(IoBuffer buff) {
        buff.clear().put((byte)(AbstractMessage.SUBSCRIBE << 4)).put((byte)0);
    }
    
    private void initMultiTopic(IoBuffer buff, int messageID, Couple... topics) throws IllegalAccessException {
        IoBuffer topicBuffer = IoBuffer.allocate(4).setAutoExpand(true);
        Utils.writeWord(topicBuffer, messageID);
        for (Couple couple : topics) {
            topicBuffer.put(Utils.encodeString(couple.getTopic()));
            topicBuffer.put((byte)couple.getQos());
        }
        topicBuffer.flip();
        
        buff.clear().put((byte)(AbstractMessage.SUBSCRIBE << 4 | (byte)0x02))
                .put(Utils.encodeRemainingLength(topicBuffer.remaining()));
        buff.put(topicBuffer);
    }
}

