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
package io.moquette.parser.netty;

import io.moquette.proto.messages.PubAckMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

/**
 *
 * @author andrea
 */
public class PubAckEncoderTest {
    ChannelHandlerContext m_mockedContext;
         
    @Before
    public void setUp() {
        //mock the ChannelHandlerContext to return an UnpooledAllocator
        m_mockedContext = TestUtils.mockChannelHandler();
    }
    
    @Test
    public void testHeaderEncode() throws Exception {
        int messageID = 0xAABB;
        PubAckEncoder encoder = new PubAckEncoder();
        PubAckMessage msg = new PubAckMessage();
        msg.setMessageID(messageID);
        ByteBuf out = Unpooled.buffer();
        
        //Exercise
        encoder.encode(m_mockedContext, msg, out);
        
        //Verify
        assertEquals(0x40, out.readByte()); //1 byte
        assertEquals(0x02, out.readByte()); //2 byte, length
        assertEquals((byte)0xAA, out.readByte());
        assertEquals((byte)0xBB, out.readByte());
    }
}
