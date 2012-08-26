package org.dna.mqtt.moquette.proto;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.demux.MessageDecoder;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.dna.mqtt.moquette.proto.TestUtils.MockProtocolDecoderOutput;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author andrea
 */
public class PublishDecoderTest {

    IoBuffer m_buff;
    PublishDecoder m_msgdec;
    MockProtocolDecoderOutput<PublishMessage> m_mockProtoDecoder;

    @Before
    public void setUp() {
        m_buff = IoBuffer.allocate(4);
        m_msgdec = new PublishDecoder();
        m_mockProtoDecoder = new MockProtocolDecoderOutput<PublishMessage>();
    }

    @Test
    public void testDecodable_OK() {
        m_buff.put((byte) (AbstractMessage.PUBLISH << 4))
                .put((byte)0) //0 length
                .flip();
        assertEquals(MessageDecoder.OK, m_msgdec.decodable(null, m_buff));
    }

    @Test
    public void testHeader() throws Exception {
        m_buff = IoBuffer.allocate(14);
        initHeader(m_buff);
        m_buff.flip();

        //Exercise
        MessageDecoderResult res = m_msgdec.decode(null, m_buff, m_mockProtoDecoder);

        assertNotNull(m_mockProtoDecoder.getMessage());
        assertEquals(MessageDecoder.OK, res);
        assertEquals("Fake Topic", m_mockProtoDecoder.getMessage().getTopicName());
        assertNull(m_mockProtoDecoder.getMessage().getMessageID());
        assertEquals(AbstractMessage.PUBLISH, m_mockProtoDecoder.getMessage().getMessageType());
    }

    @Test
    public void testHeaderWithMessageID() throws Exception {
        m_buff = IoBuffer.allocate(14).setAutoExpand(true);
        int messageID = 123;
        initHeaderWithMessageID(m_buff, messageID);
        m_buff.flip();

        //Exercise
        MessageDecoderResult res = m_msgdec.decode(null, m_buff, m_mockProtoDecoder);

        assertNotNull(m_mockProtoDecoder.getMessage());
        assertEquals(MessageDecoder.OK, res);
        assertEquals("Fake Topic", m_mockProtoDecoder.getMessage().getTopicName());
        assertEquals(messageID, (int) m_mockProtoDecoder.getMessage().getMessageID());
    }
    
    @Test
    public void testHeaderWithMessageID_Payload() throws Exception {
        m_buff = IoBuffer.allocate(14).setAutoExpand(true);
        int messageID = 123;
        byte[] payload = new byte[]{0x0A, 0x0B, 0x0C};
        initHeaderWithMessageID_Payload(m_buff, messageID, payload);
        m_buff.flip();

        //Exercise
        MessageDecoderResult res = m_msgdec.decode(null, m_buff, m_mockProtoDecoder);

        assertNotNull(m_mockProtoDecoder.getMessage());
        assertEquals(MessageDecoder.OK, res);
        assertEquals("Fake Topic", m_mockProtoDecoder.getMessage().getTopicName());
        assertEquals(messageID, (int) m_mockProtoDecoder.getMessage().getMessageID());
        TestUtils.verifyEquals(payload, m_mockProtoDecoder.getMessage().getPayload());
    }
    
     @Test
     public void testBugOnRealCase() throws Exception {
         byte[] overallMessage = new byte[] {0x30, 0x17, //fixed header, 25 byte lenght
             0x00, 0x06, 0x2f, 0x74, 0x6f, 0x70, 0x69, 0x63, //[/topic] string 2 len + 6 content
             0x54, 0x65, 0x73, 0x74, 0x20, 0x6d, 0x79, // [Test my payload] encoding
             0x20, 0x70, 0x61, 0x79, 0x6c, 0x6f, 0x61, 0x64};
         m_buff = IoBuffer.allocate(overallMessage.length).setAutoExpand(true);
         m_buff.put(overallMessage);
         m_buff.flip();
         
         //Exercise
        MessageDecoderResult res = m_msgdec.decode(null, m_buff, m_mockProtoDecoder);

        assertNotNull(m_mockProtoDecoder.getMessage());
        assertEquals(MessageDecoder.OK, res);
     }

    private void initHeader(IoBuffer buff) throws IllegalAccessException {
        IoBuffer tmp = IoBuffer.allocate(4).setAutoExpand(true).put(Utils.encodeString("Fake Topic")).flip();
        buff.clear().put((byte) (AbstractMessage.PUBLISH << 4)).put(Utils.encodeRemainingLength(tmp.remaining()));
        //topic name
        buff.put(tmp);
    }

    private void initHeaderWithMessageID(IoBuffer buff, int messageID) throws IllegalAccessException {
        IoBuffer tmp = IoBuffer.allocate(4).setAutoExpand(true).put(Utils.encodeString("Fake Topic"));
        Utils.writeWord(tmp, messageID);
        tmp.flip();
        buff.clear().put((byte) (AbstractMessage.PUBLISH << 4 | 0x02)) //set Qos to 1
                .put(Utils.encodeRemainingLength(tmp.remaining()));
        //topic name
        buff.put(tmp);
    }
    
    private void initHeaderWithMessageID_Payload(IoBuffer buff, int messageID, byte[] payload) throws IllegalAccessException {
        IoBuffer tmp = IoBuffer.allocate(4).setAutoExpand(true).put(Utils.encodeString("Fake Topic"));
        Utils.writeWord(tmp, messageID);
        tmp.put(payload);
        tmp.flip();
        buff.clear().put((byte) (AbstractMessage.PUBLISH << 4 | 0x02)) //set Qos to 1
                .put(Utils.encodeRemainingLength(tmp.remaining()));
        //topic name
        buff.put(tmp);
    }
}
