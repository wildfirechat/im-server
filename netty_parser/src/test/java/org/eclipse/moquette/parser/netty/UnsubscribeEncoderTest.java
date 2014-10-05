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
import io.netty.channel.ChannelHandlerContext;
import org.eclipse.moquette.proto.messages.AbstractMessage;
import org.eclipse.moquette.proto.messages.UnsubscribeMessage;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.eclipse.moquette.parser.netty.TestUtils.*;

/**
 *
 * @author andrea
 */
public class UnsubscribeEncoderTest {
    UnsubscribeEncoder m_encoder = new UnsubscribeEncoder();
    ChannelHandlerContext m_mockedContext;
    ByteBuf m_out;
    UnsubscribeMessage m_msg;
         
    @Before
    public void setUp() {
        //mock the ChannelHandlerContext to return an UnpooledAllocator
        m_mockedContext = mockChannelHandler();
        m_out = Unpooled.buffer();
        m_msg = new UnsubscribeMessage();
        m_msg.setMessageID(0xAABB);
    }
    
    @Test
    public void testEncodeWithSingleTopic() throws Exception {
        m_msg.setQos(AbstractMessage.QOSType.LEAST_ONE);
        
        //variable part
        String topic1 = "/topic";
        m_msg.addTopicFilter(topic1);

        //Exercise
        m_encoder.encode(m_mockedContext, m_msg, m_out);
        
        //Verify
        assertEquals((byte)0xA2, (byte)m_out.readByte()); //1 byte
        //2 messageID + 2 length + 6 chars = 10
        assertEquals(10, m_out.readByte()); //remaining length
        
        //verify M1ssageID
        assertEquals((byte)0xAA, m_out.readByte());
        assertEquals((byte)0xBB, m_out.readByte());
        
        //Variable part
        verifyString(topic1, m_out);
    }
    

    @Test
    public void testEncodeWithMultiTopic() throws Exception {
        m_msg.setQos(AbstractMessage.QOSType.LEAST_ONE);
        
        //variable part
        String topic1 = "a/b";
        String topic2 = "a/b/c";
        m_msg.addTopicFilter(topic1);
        m_msg.addTopicFilter(topic2);

        //Exercise
        m_encoder.encode(m_mockedContext, m_msg, m_out);

        //Verify
        assertEquals((byte)0xA2, (byte)m_out.readByte()); //1 byte
        assertEquals(14, m_out.readByte()); //remaining length
        
        //verify M1ssageID
        assertEquals((byte)0xAA, m_out.readByte());
        assertEquals((byte)0xBB, m_out.readByte());
        
        //Variable part
        verifyString(topic1, m_out);
        verifyString(topic2, m_out);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEncode_empty_topics() throws Exception {
        m_msg.setQos(AbstractMessage.QOSType.LEAST_ONE);

        //Exercise
        m_encoder.encode(m_mockedContext, m_msg, m_out);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEncode_badQos() throws Exception {
        m_msg.setQos(AbstractMessage.QOSType.EXACTLY_ONCE);

        //Exercise
        m_encoder.encode(m_mockedContext, m_msg, m_out);
    }
}
