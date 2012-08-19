package org.dna.mqtt.moquette.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.dna.mqtt.moquette.proto.TestUtils.MockProtocolDecoderOutput;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.UnsubscribeMessage;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author andrea
 */
public class UnsubscribeDecoderTest {

    IoBuffer m_buff;
    UnsubscribeDecoder m_msgdec;
    MockProtocolDecoderOutput<UnsubscribeMessage> m_mockProtoDecoder;

    @Before
    public void setUp() {
        m_buff = IoBuffer.allocate(4);
        m_msgdec = new UnsubscribeDecoder();
        m_mockProtoDecoder = new MockProtocolDecoderOutput<UnsubscribeMessage>();
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
        String topic1 = "a/b";
        String topic2 = "c/d/e";
        initMultiTopic(m_buff, 123, topic1, topic2);
        m_buff.flip();
        
        //Excercise
        MessageDecoderResult res = m_msgdec.decode(null, m_buff, m_mockProtoDecoder);

        //Verify
        assertEquals(MessageDecoderResult.OK, res);
        assertEquals(2, m_mockProtoDecoder.getMessage().topics().size());
        assertEquals(topic1, m_mockProtoDecoder.getMessage().topics().get(0));
        assertEquals(topic2, m_mockProtoDecoder.getMessage().topics().get(1));
        assertEquals(AbstractMessage.UNSUBSCRIBE, m_mockProtoDecoder.getMessage().getMessageType());
    }
    
    
    @Test
    public void testDecodeSingleTopic_bug() throws Exception {
        //A2 0C 00 01 00 06 2F 74 6F 70 69 63 //12 byte
        byte[] overallMessage = new byte[] {(byte)0xA2, 0x0A, //fixed header
             0x00, 0x01, //MSG ID
             0x00, 0x06, 0x2F, 0x74, 0x6F, 0x70, 0x69, 0x63}; //"/topic" string
        m_buff = IoBuffer.allocate(overallMessage.length).setAutoExpand(true);
        m_buff.put(overallMessage);
        m_buff.flip();
         
        MessageDecoderResult res = m_msgdec.decode(null, m_buff, m_mockProtoDecoder);

        assertNotNull(m_mockProtoDecoder.getMessage());
        UnsubscribeMessage message = (UnsubscribeMessage) m_mockProtoDecoder.getMessage();
//        assertEquals(0x0A, message.getMessageID());
//        assertEquals(1, message.types().size());
//        assertEquals(AbstractMessage.QOSType.LEAST_ONE, message.types().get(0));
        
    }
    
//    @Test
//    public void testDecodeSingleTopic_BadFormat() throws Exception {
//        //A2 0C 00 01 00 06 2F 74 6F 70 69 63 //10 byte of content, but lenght says 12!!
//        byte[] overallMessage = new byte[] {(byte)0xA2, 0x0C, //fixed header
//             0x00, 0x01, //MSG ID
//             0x00, 0x06, 0x2F, 0x74, 0x6F, 0x70, 0x69, 0x63}; //"/topic" string
//        m_buff = IoBuffer.allocate(overallMessage.length).setAutoExpand(true);
//        m_buff.put(overallMessage);
//        m_buff.flip();
//         
//        MessageDecoderResult res = m_msgdec.decode(null, m_buff, m_mockProtoDecoder);
//
//        assertNotNull(m_mockProtoDecoder.getMessage());
//        UnsubscribeMessage message = (UnsubscribeMessage) m_mockProtoDecoder.getMessage();
////        assertEquals(0x0A, message.getMessageID());
////        assertEquals(1, message.types().size());
////        assertEquals(AbstractMessage.QOSType.LEAST_ONE, message.types().get(0));
//        
//    }
    
    private void initHeaderBadQos(IoBuffer buff) {
        buff.clear().put((byte)(AbstractMessage.UNSUBSCRIBE << 4)).put((byte)0);
    }
    
    private void initMultiTopic(IoBuffer buff, int messageID, String... topics) throws IllegalAccessException {
        IoBuffer topicBuffer = IoBuffer.allocate(4).setAutoExpand(true);
        Utils.writeWord(topicBuffer, messageID);
        for (String topic : topics) {
            topicBuffer.put(Utils.encodeString(topic));
        }
        topicBuffer.flip();
        
        buff.clear().put((byte)(AbstractMessage.UNSUBSCRIBE << 4 | (byte)0x02))
                .put(Utils.encodeRemainingLength(topicBuffer.remaining()));
        buff.put(topicBuffer);
    }
}

