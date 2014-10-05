/*
 * Copyright (c) 2012-2014 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package org.eclipse.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.util.AttributeMap;
import io.netty.util.DefaultAttributeMap;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.moquette.proto.messages.AbstractMessage;
import org.eclipse.moquette.proto.messages.PublishMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author andrea
 */
public class PublishDecoderTest {
    ByteBuf m_buff;
    PublishDecoder m_msgdec;
    List<Object> m_results;
    AttributeMap m_attrMap;
    
    private static final int MESSAGE_ID = 123;
    
    @Before
    public void setUp() {
        m_msgdec = new PublishDecoder();
        m_results = new ArrayList<Object >();
        m_attrMap = new DefaultAttributeMap();
    }
    
    @Test
    public void testHeader() throws Exception {
        m_buff = Unpooled.buffer(14);
        initHeader(m_buff);
        
        //Excercise
        m_msgdec.decode(m_attrMap, m_buff, m_results);

        assertFalse(m_results.isEmpty());
        PublishMessage message = (PublishMessage)m_results.get(0); 
        assertNotNull(message);
        assertEquals("Fake Topic", message.getTopicName());
        assertNull(message.getMessageID());
        assertEquals(AbstractMessage.PUBLISH, message.getMessageType());
    }
    
    @Test
    public void testHeaderWithMessageID() throws Exception {
        m_buff = Unpooled.buffer(14);
        initHeaderWithMessageID(m_buff, MESSAGE_ID);

        //Exercise
        m_msgdec.decode(m_attrMap, m_buff, m_results);

        assertFalse(m_results.isEmpty());
        PublishMessage message = (PublishMessage)m_results.get(0); 
        assertNotNull(message);
        assertEquals("Fake Topic", message.getTopicName());
        assertEquals(MESSAGE_ID, (int) message.getMessageID());
    }
    
    
    @Test
    public void testHeaderWithMessageID_Payload() throws Exception {
        m_buff = Unpooled.buffer(14);
//        byte[] payload = new byte[]{0x0A, 0x0B, 0x0C};
        ByteBuffer payload = ByteBuffer.allocate(3).put(new byte[]{0x0A, 0x0B, 0x0C});
        initHeaderWithMessageID_Payload(m_buff, MESSAGE_ID, payload);

        //Exercise
        m_msgdec.decode(m_attrMap, m_buff, m_results);

        assertFalse(m_results.isEmpty());
        PublishMessage message = (PublishMessage)m_results.get(0); 
        assertNotNull(message);
        assertEquals("Fake Topic", message.getTopicName());
        assertEquals(MESSAGE_ID, (int) message.getMessageID());
//        TestUtils.verifyEquals(payload, message.getPayload());
        assertEquals(payload, message.getPayload());
    }
    
    @Test(expected = CorruptedFrameException.class)
    public void testDup0WithQoS0_3_1_1() throws Exception {
        m_buff = m_buff = preparePubclishWithQosFlags((byte) 0x08);
        
        //Exercise
        m_msgdec.decode(m_attrMap, m_buff, m_results);
    }
    
    @Test(expected = CorruptedFrameException.class)
    public void testReservedQoS3_3_1_1() throws Exception {
        m_buff = preparePubclishWithQosFlags((byte) 0x0E);
        
        //Exercise
        m_msgdec.decode(m_attrMap, m_buff, m_results);
    }
    
    @Test(expected = CorruptedFrameException.class)
    public void testTopicWithWildCards() throws Exception {
        byte[] overallMessage = new byte[]{0x30, 0x17, //fixed header, 25 byte lenght
            0x00, 0x06, 0x2f, 0x74, 0x6f, 0x70, 0x2B /*+*/, 0x23 /*#*/, //[/top+#] string 2 len + 6 content
            0x54, 0x65, 0x73, 0x74, 0x20, 0x6d, 0x79, // [Test my payload] encoding
            0x20, 0x70, 0x61, 0x79, 0x6c, 0x6f, 0x61, 0x64};
        m_buff = Unpooled.buffer(overallMessage.length);
        m_buff.writeBytes(overallMessage);
        
        //Exercise
        m_msgdec.decode(m_attrMap, m_buff, m_results);
    }
    
    @Test
    public void testBugOnRealCase() throws Exception {
        byte[] overallMessage = new byte[]{0x30, 0x17, //fixed header, 25 byte lenght
            0x00, 0x06, 0x2f, 0x74, 0x6f, 0x70, 0x69, 0x63, //[/topic] string 2 len + 6 content
            0x54, 0x65, 0x73, 0x74, 0x20, 0x6d, 0x79, // [Test my payload] encoding
            0x20, 0x70, 0x61, 0x79, 0x6c, 0x6f, 0x61, 0x64};
        m_buff = Unpooled.buffer(overallMessage.length);
        m_buff.writeBytes(overallMessage);

        //Exercise
        m_msgdec.decode(m_attrMap, m_buff, m_results);

        assertFalse(m_results.isEmpty());
        PublishMessage message = (PublishMessage)m_results.get(0); 
        assertNotNull(message);
    }
    
    @Test
    public void testDecodeBigContent() throws Exception {
        int size = 129;
        ByteBuf payload = TestUtils.generateRandomPayload(size);

        ByteBuf firstPublish = generatePublishQoS0(payload);
        ByteBuf secondPublish = generatePublishQoS0(TestUtils.generateRandomPayload(size));

        ByteBuf doubleMessageBuf = Unpooled.buffer(size * 2);
        doubleMessageBuf.writeBytes(firstPublish).writeBytes(secondPublish);


        //Exercise
        m_msgdec.decode(m_attrMap, doubleMessageBuf, m_results);

        assertFalse(m_results.isEmpty());
        PublishMessage message = (PublishMessage)m_results.get(0); 
        assertNotNull(message);
        m_results.clear();

        m_msgdec.decode(m_attrMap, doubleMessageBuf, m_results);

        assertFalse(m_results.isEmpty());
        message = (PublishMessage)m_results.get(0); 
        assertNotNull(message);
    }
    
    @Test
    public void testBadClaimMoreData() throws Exception {
        byte[] rawMessage = new byte[] {
            0x30, 0x17, 0x00, 0x06, 0x2f, (byte)0x74, 0x6f, (byte)0x70, 0x69, 0x63, 0x54, 0x65, (byte)0x73, 0x74, 0x20, 0x6d,
            (byte)0x79, 0x20, (byte)0x70, 0x61, (byte)0x79, 0x6c, 0x6f, 0x61, 0x64
        };
        
        ByteBuf msgBuf = Unpooled.buffer(25);
        msgBuf.writeBytes(rawMessage);
        msgBuf.readByte();  //to simulate the reading of messageType done by MQTTDecoder dispatcher
        
        //Exercise
        m_msgdec.decode(m_attrMap, msgBuf, m_results);

        assertFalse(m_results.isEmpty());
        PublishMessage message = (PublishMessage)m_results.get(0); 
        assertNotNull(message);
        assertEquals("/topic", message.getTopicName());
//        assertEquals("Test my payload", new String(message.getPayload()));
        Buffer expectedPayload =  ByteBuffer.allocate(15).put("Test my payload".getBytes()).flip();
        assertEquals(expectedPayload, message.getPayload());
    }
    
    private void initHeader(ByteBuf buff) throws IllegalAccessException {
        ByteBuf tmp = Unpooled.buffer(4).writeBytes(Utils.encodeString("Fake Topic"));
        buff.clear().writeByte(AbstractMessage.PUBLISH << 4).writeBytes(Utils.encodeRemainingLength(tmp.readableBytes()));
        //topic name
        buff.writeBytes(tmp);
    }
    
    private void initHeaderWithMessageID(ByteBuf buff, int messageID) throws IllegalAccessException {
        ByteBuf tmp = Unpooled.buffer(4).writeBytes(Utils.encodeString("Fake Topic"));
        tmp.writeShort(messageID);
        buff.clear().writeByte(AbstractMessage.PUBLISH << 4 | 0x02) //set Qos to 1
                .writeBytes(Utils.encodeRemainingLength(tmp.readableBytes()));
        //topic name
        buff.writeBytes(tmp);
    }
     
    private void initHeaderWithMessageID_Payload(ByteBuf buff, int messageID, byte[] payload) throws IllegalAccessException {
        ByteBuf tmp = Unpooled.buffer(4).writeBytes(Utils.encodeString("Fake Topic"));
        tmp.writeShort(messageID);
        tmp.writeBytes(payload);
        buff.clear().writeByte(AbstractMessage.PUBLISH << 4 | 0x02) //set Qos to 1
                .writeBytes(Utils.encodeRemainingLength(tmp.readableBytes()));
        //topic name
        buff.writeBytes(tmp);
    }
    
    private void initHeaderWithMessageID_Payload(ByteBuf buff, int messageID, ByteBuffer payload) throws IllegalAccessException {
        ByteBuf tmp = Unpooled.buffer(4).writeBytes(Utils.encodeString("Fake Topic"));
        tmp.writeShort(messageID);
        tmp.writeBytes(payload);
        buff.clear().writeByte(AbstractMessage.PUBLISH << 4 | 0x02) //set Qos to 1
                .writeBytes(Utils.encodeRemainingLength(tmp.readableBytes()));
        //topic name
        buff.writeBytes(tmp);
    }
    
    private ByteBuf generatePublishQoS0(ByteBuf payload) throws IllegalAccessException {
        int size = payload.capacity();
        ByteBuf messageBody = Unpooled.buffer(size);
        messageBody.writeBytes(Utils.encodeString("/topic"));

        //ONLY for QoS > 1 Utils.writeWord(messageBody, messageID);
        messageBody.writeBytes(payload);

        ByteBuf completeMsg = Unpooled.buffer(size);
        completeMsg.clear().writeByte(AbstractMessage.PUBLISH << 4 | 0x00) //set Qos to 0
                .writeBytes(Utils.encodeRemainingLength(messageBody.readableBytes()));
        completeMsg.writeBytes(messageBody);

        return completeMsg;
    }
    
    private ByteBuf preparePubclishWithQosFlags(byte flags) {
        ByteBuf buff = Unpooled.buffer(14);
        ByteBuffer payload = ByteBuffer.allocate(3).put(new byte[]{0x0A, 0x0B, 0x0C});
        ByteBuf tmp = Unpooled.buffer(4).writeBytes(Utils.encodeString("Fake Topic"));
        tmp.writeShort(MESSAGE_ID);
        tmp.writeBytes(payload);
        buff.clear().writeByte(AbstractMessage.PUBLISH << 4 | flags) //set DUP=1 Qos to 11 => b1110
                .writeBytes(Utils.encodeRemainingLength(tmp.readableBytes()));
        //topic name
        buff.writeBytes(tmp);
        
        return buff;
    }
}
