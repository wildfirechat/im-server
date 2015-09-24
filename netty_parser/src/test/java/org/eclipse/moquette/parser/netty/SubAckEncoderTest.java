/*
 * Copyright (c) 2012-2015 The original author or authors
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
import static org.eclipse.moquette.parser.netty.TestUtils.mockChannelHandler;
import org.eclipse.moquette.proto.messages.AbstractMessage;
import org.eclipse.moquette.proto.messages.SubAckMessage;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author andrea
 */
public class SubAckEncoderTest {
    SubAckEncoder m_encoder = new SubAckEncoder();
    ChannelHandlerContext m_mockedContext;
    ByteBuf m_out;
         
    @Before
    public void setUp() {
        //mock the ChannelHandlerContext to return an UnpooledAllocator
        m_mockedContext = mockChannelHandler();
        m_out = Unpooled.buffer();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testEncodeWithNoQoss() throws Exception {
        SubAckMessage msg = new SubAckMessage();
        msg.setMessageID(123);

        //Exercise
        m_encoder.encode(m_mockedContext, msg, m_out);
    }

    @Test
    public void testEncodeWithMultipleQos() throws Exception {
        SubAckMessage msg = new SubAckMessage();

        int messageID = 0xAABB;
        msg.setMessageID(messageID);
        msg.addType(AbstractMessage.QOSType.MOST_ONE);
        msg.addType(AbstractMessage.QOSType.LEAST_ONE);
        msg.addType(AbstractMessage.QOSType.EXACTLY_ONCE);
        
        //Exercise
        m_encoder.encode(m_mockedContext, msg, m_out);

        //Verify
        assertEquals((byte) (AbstractMessage.SUBACK << 4 ), m_out.readByte()); //1 byte
        assertEquals(5, m_out.readByte()); //remaining length

        //Variable part
        assertEquals((byte)0xAA, m_out.readByte()); //MessageID MSB
        assertEquals((byte)0xBB, m_out.readByte()); //MessageID LSB
        assertEquals(AbstractMessage.QOSType.MOST_ONE.byteValue(), m_out.readByte());
        assertEquals(AbstractMessage.QOSType.LEAST_ONE.byteValue(), m_out.readByte());
        assertEquals(AbstractMessage.QOSType.EXACTLY_ONCE.byteValue(), m_out.readByte());
    }

    @Test
    public void testEncodeWithFailureQOS() throws Exception {
        SubAckMessage msg = new SubAckMessage();

        int messageID = 0xAABB;
        msg.setMessageID(messageID);
        msg.addType(AbstractMessage.QOSType.FAILURE);

        //Exercise
        m_encoder.encode(m_mockedContext, msg, m_out);

        //Verify
        assertEquals((byte) (AbstractMessage.SUBACK << 4 ), m_out.readByte()); //1 byte
        assertEquals(3, m_out.readByte()); //remaining length

        //Variable part
        assertEquals((byte)0xAA, m_out.readByte()); //MessageID MSB
        assertEquals((byte)0xBB, m_out.readByte()); //MessageID LSB
        assertEquals((byte)0x80, m_out.readByte()); //Failure return code
    }
}
