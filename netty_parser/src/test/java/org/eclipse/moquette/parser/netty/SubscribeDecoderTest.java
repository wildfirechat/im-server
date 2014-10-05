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
import java.util.ArrayList;
import java.util.List;
import org.eclipse.moquette.proto.messages.AbstractMessage;
import org.eclipse.moquette.proto.messages.SubscribeMessage;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author andrea
 */
public class SubscribeDecoderTest {
    ByteBuf m_buff;
    SubscribeDecoder m_msgdec;
    List<Object> m_results;
    
    @Before
    public void setUp() {
        m_msgdec = new SubscribeDecoder();
        m_results = new ArrayList<Object >();
    }
    
    @Test(expected = CorruptedFrameException.class)
    public void testBadQos() throws Exception {
        m_buff = Unpooled.buffer(2);
        initHeaderBadQos(m_buff);

        //Excercise
        m_msgdec.decode(null, m_buff, m_results);
    }
    
    @Test
    public void testMultiTopic() throws Exception {
        m_buff = Unpooled.buffer(4);
        SubscribeMessage.Couple c1 = new SubscribeMessage.Couple((byte)2, "a/b");
        SubscribeMessage.Couple c2 = new SubscribeMessage.Couple((byte)1, "c/d/e");
        initMultiTopic(m_buff, 123, c1, c2);
        
        //Excercise
        m_msgdec.decode(null, m_buff, m_results);

        //Verify
        assertFalse(m_results.isEmpty());
        SubscribeMessage message = (SubscribeMessage)m_results.get(0); 
        assertEquals(2, message.subscriptions().size());
        assertEquals(AbstractMessage.SUBSCRIBE, message.getMessageType());
    }
    
    @Test(expected = CorruptedFrameException.class)
    public void testFailOnEmptyTopic() throws Exception {
        m_buff = Unpooled.buffer(4);
        initMultiTopic(m_buff, 123);
        
        //Excercise
        m_msgdec.decode(null, m_buff, m_results);

        //Verify
        assertFalse(m_results.isEmpty());
        SubscribeMessage message = (SubscribeMessage)m_results.get(0); 
        assertEquals(2, message.subscriptions().size());
        assertEquals(AbstractMessage.SUBSCRIBE, message.getMessageType());
    }
    
    @Test(expected = CorruptedFrameException.class)
    public void testFailOnSubscriptionWithReservedQosPolluted() throws Exception {
        m_buff = Unpooled.buffer(4);
        SubscribeMessage.Couple c1 = new SubscribeMessage.Couple((byte)2, "a/b");
        initPollutedTopic(m_buff, 123, c1);
        
        //Excercise
        m_msgdec.decode(null, m_buff, m_results);

        //Verify
        assertFalse(m_results.isEmpty());
        SubscribeMessage message = (SubscribeMessage)m_results.get(0); 
        assertEquals(2, message.subscriptions().size());
        assertEquals(AbstractMessage.SUBSCRIBE, message.getMessageType());
    }
    
    private void initHeaderBadQos(ByteBuf buff) {
        buff.clear().writeByte(AbstractMessage.SUBSCRIBE << 4).writeByte(0);
    }
    
    private void initMultiTopic(ByteBuf buff, int messageID, SubscribeMessage.Couple... topics) throws IllegalAccessException {
        ByteBuf topicBuffer = Unpooled.buffer(4);
        topicBuffer.writeShort(messageID);
        for (SubscribeMessage.Couple couple : topics) {
            topicBuffer.writeBytes(Utils.encodeString(couple.getTopicFilter()));
            topicBuffer.writeByte(couple.getQos());
        }
        
        buff.clear().writeByte(AbstractMessage.SUBSCRIBE << 4 | (byte)0x02)
                .writeBytes(Utils.encodeRemainingLength(topicBuffer.readableBytes()));
        buff.writeBytes(topicBuffer);
    }
    
    private void initPollutedTopic(ByteBuf buff, int messageID, SubscribeMessage.Couple topic) throws IllegalAccessException {
        ByteBuf topicBuffer = Unpooled.buffer(4);
        topicBuffer.writeShort(messageID);
        topicBuffer.writeBytes(Utils.encodeString(topic.getTopicFilter()));
        topicBuffer.writeByte(0xF0 | topic.getQos());
        
        buff.clear().writeByte(AbstractMessage.SUBSCRIBE << 4 | (byte)0x02)
                .writeBytes(Utils.encodeRemainingLength(topicBuffer.readableBytes()));
        buff.writeBytes(topicBuffer);
    }
}
