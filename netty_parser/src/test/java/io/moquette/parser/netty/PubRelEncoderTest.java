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

import io.moquette.proto.messages.PubRelMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author kazvictor
 */
public class PubRelEncoderTest {
    ChannelHandlerContext m_mockedContext;
         
    @Before
    public void setUp() {
        //mock the ChannelHandlerContext to return an UnpooledAllocator
        m_mockedContext = TestUtils.mockChannelHandler();
    }
    
    @Test
    public void testHeaderEncode() throws Exception {
        int messageID = 0xAABB;
        PubRelEncoder encoder = new PubRelEncoder();
        PubRelMessage msg = new PubRelMessage();
        msg.setMessageID(messageID);
        ByteBuf out = Unpooled.buffer();
        
        //Exercise
        encoder.encode(m_mockedContext, msg, out);
        
        //Verify
        assertEquals(0x62, out.readByte()); //1 byte. See MQTT-3.6.1-1
        assertEquals(0x02, out.readByte()); //2 byte, length
        assertEquals((byte)0xAA, out.readByte());
        assertEquals((byte)0xBB, out.readByte());
    }
}
