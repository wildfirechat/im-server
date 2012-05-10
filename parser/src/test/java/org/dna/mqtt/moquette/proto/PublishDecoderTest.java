package org.dna.mqtt.moquette.proto;

import org.dna.mqtt.moquette.proto.PublishDecoder;
import org.dna.mqtt.moquette.proto.Utils;
import org.apache.mina.filter.codec.demux.MessageDecoderResult;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.demux.MessageDecoder;
import org.dna.mqtt.moquette.proto.TestUtils.MockProtocolDecoderOutput;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

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
        m_buff.put((byte) (AbstractMessage.PUBLISH << 4)).flip();
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
