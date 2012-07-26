package org.dna.mqtt.moquette.proto;

import org.dna.mqtt.moquette.proto.PublishEncoder;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.dna.mqtt.moquette.proto.TestUtils.*;

/**
 *
 * @author andrea
 */
public class PublishEncoderTest {

    PublishEncoder m_encoder;
    TestUtils.MockProtocolEncoderOutput m_mockProtoEncoder;

    @Before
    public void setUp() {
        m_encoder = new PublishEncoder();
        m_mockProtoEncoder = new TestUtils.MockProtocolEncoderOutput();
    }

    @Test
    public void testEncodeWithQos_0_noMessageID() throws Exception {
        String topic = "/photos";
        PublishMessage msg = new PublishMessage();
        msg.setQos(QOSType.MOST_ONE);
        msg.setTopicName(topic);

        //variable part
        byte[] payload = new byte[]{0x0A, 0x0B, 0x0C};
        msg.setPayload(payload);

        //Exercise
        m_encoder.encode(null, msg, m_mockProtoEncoder);

        //Verify
        assertEquals(0x30, m_mockProtoEncoder.getBuffer().get()); //1 byte
        //(2+7) topic + 3 payload
        assertEquals(12, m_mockProtoEncoder.getBuffer().get()); //remaining length

        //Variable part
        verifyString(topic, m_mockProtoEncoder.getBuffer());
        verifyBuff(payload.length, payload, m_mockProtoEncoder.getBuffer());
    }

    @Test
    public void testEncodeWithQos_1_MessageID() throws Exception {
        String topic = "/photos";
        PublishMessage msg = new PublishMessage();
        msg.setQos(QOSType.LEAST_ONE);
        msg.setMessageID(1);
        msg.setTopicName(topic);

        //variable part
        byte[] payload = new byte[]{0x0A, 0x0B, 0x0C};
        msg.setPayload(payload);

        //Exercise
        m_encoder.encode(null, msg, m_mockProtoEncoder);

        //Verify
        assertEquals(0x32, m_mockProtoEncoder.getBuffer().get()); //1 byte
        //(2+7) topic + 2 messageID + 3 payload
        assertEquals(14, m_mockProtoEncoder.getBuffer().get()); //remaining length

        //Variable part
        verifyString(topic, m_mockProtoEncoder.getBuffer());
        assertEquals(0, m_mockProtoEncoder.getBuffer().get()); //MessageID MSB
        assertEquals(1, m_mockProtoEncoder.getBuffer().get()); //MessageID LSB
        verifyBuff(payload.length, payload, m_mockProtoEncoder.getBuffer());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncodeWithQos_1_noMessageID_fail() throws Exception {
        String topic = "pictures/photos";
        PublishMessage msg = new PublishMessage();
        msg.setQos(QOSType.LEAST_ONE);
        msg.setTopicName(topic);

        //Exercise
        m_encoder.encode(null, msg, m_mockProtoEncoder);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncode_empty_topic() throws Exception {
        PublishMessage msg = new PublishMessage();
        msg.setQos(QOSType.LEAST_ONE);
        msg.setMessageID(1);

        //Exercise
        m_encoder.encode(null, msg, m_mockProtoEncoder);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncode_null_topic() throws Exception {
        PublishMessage msg = new PublishMessage();
        msg.setQos(QOSType.LEAST_ONE);
        msg.setMessageID(1);
        msg.setTopicName(null);

        //Exercise
        m_encoder.encode(null, msg, m_mockProtoEncoder);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testEncode_qos_reserved() throws Exception {
        PublishMessage msg = new PublishMessage();
        msg.setQos(QOSType.RESERVED);
        msg.setMessageID(1);
        msg.setTopicName(null);

        //variable part
        byte[] payload = new byte[]{0x0A, 0x0B, 0x0C};
        msg.setPayload(payload);

        //Exercise
        m_encoder.encode(null, msg, m_mockProtoEncoder);
    }
    
    @Test
    public void testBugMissedMessageIDEncoding() throws Exception{
        String topic = "/topic";
        PublishMessage msg = new PublishMessage();
        msg.setQos(QOSType.MOST_ONE);
        msg.setTopicName(topic);

        //variable part
        byte[] payload = "Test my payload".getBytes();
        msg.setPayload(payload);

        //Exercise
        m_encoder.encode(null, msg, m_mockProtoEncoder);

        //Verify
        //2 byte header + (2+6) topic name + [2 message ID if QoQ <> 0]+ payload
        int size = m_mockProtoEncoder.getBuffer().remaining();
        assertEquals(10+payload.length, size);
        assertEquals(0x30, m_mockProtoEncoder.getBuffer().get()); //1 byte
        assertEquals(23, m_mockProtoEncoder.getBuffer().get()); //1 byte the length
    }
}
