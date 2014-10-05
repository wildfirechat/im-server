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
import java.util.Arrays;
import java.util.List;
import org.eclipse.moquette.proto.messages.AbstractMessage;
import org.eclipse.moquette.proto.messages.UnsubscribeMessage;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author andrea
 */
public class UnsubscribeDecoderTest {
    ByteBuf m_buff;
    UnsubscribeDecoder m_msgdec;
    List<Object> m_results;
    
    @Before
    public void setUp() {
        m_msgdec = new UnsubscribeDecoder();
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
        String topic1 = "a/b";
        String topic2 = "c/d/e";
        initMultiTopic(m_buff, 123, topic1, topic2);
        
        //Excercise
        m_msgdec.decode(null, m_buff, m_results);

        //Verify
        assertFalse(m_results.isEmpty());
        UnsubscribeMessage message = (UnsubscribeMessage)m_results.get(0); 
        assertNotNull(message);
        assertEquals(Arrays.asList(topic1, topic2), message.topicFilters());
        assertEquals(AbstractMessage.UNSUBSCRIBE, message.getMessageType());
    }
    
    
    @Test(expected = CorruptedFrameException.class)
    public void testFailOnEmptyTopic() throws Exception {
        m_buff = Unpooled.buffer(4);
        initMultiTopic(m_buff, 123);
        
        //Excercise
        m_msgdec.decode(null, m_buff, m_results);
    }
    
    @Test
    public void testDecodeSingleTopic_bug() throws Exception {
        //A2 0C 00 01 00 06 2F 74 6F 70 69 63 //12 byte
        byte[] overallMessage = new byte[] {(byte)0xA2, 0x0A, //fixed header
             0x00, 0x01, //MSG ID
             0x00, 0x06, 0x2F, 0x74, 0x6F, 0x70, 0x69, 0x63}; //"/topic" string
        m_buff = Unpooled.buffer(overallMessage.length);
        m_buff.writeBytes(overallMessage);
         
        m_msgdec.decode(null, m_buff, m_results);

        assertFalse(m_results.isEmpty());
        UnsubscribeMessage message = (UnsubscribeMessage)m_results.get(0); 
        assertNotNull(message);
        assertEquals(0x01, (int)message.getMessageID());
    }
    
    
    private void initHeaderBadQos(ByteBuf buff) {
        buff.clear().writeByte(AbstractMessage.UNSUBSCRIBE << 4).writeByte(0);
    }
    
    private void initMultiTopic(ByteBuf buff, int messageID, String... topics) throws IllegalAccessException {
        ByteBuf topicBuffer = Unpooled.buffer(4);
        topicBuffer.writeShort(messageID);
        for (String topic : topics) {
            topicBuffer.writeBytes(Utils.encodeString(topic));
        }
        
        buff.clear().writeByte(AbstractMessage.UNSUBSCRIBE << 4 | (byte)0x02)
                .writeBytes(Utils.encodeRemainingLength(topicBuffer.readableBytes()));
        buff.writeBytes(topicBuffer);
    }
    
}
