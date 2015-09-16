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
import org.eclipse.moquette.proto.messages.ConnectMessage;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import static org.eclipse.moquette.parser.netty.TestUtils.*;


/**
 *
 * @author andrea
 */
public class ConnectEncoderTest {
    ConnectEncoder m_encoder = new ConnectEncoder();
    ChannelHandlerContext m_mockedContext;
         
    @Before
    public void setUp() {
        //mock the ChannelHandlerContext to return an UnpooledAllocator
        m_mockedContext = mockChannelHandler();
    }
    
    @Test
    public void testBaseHeader() throws Exception {
        ConnectMessage msg = new ConnectMessage();
        msg.setWillRetain(true);
        msg.setWillQos((byte)2);
        msg.setWillFlag(false);
        msg.setCleanSession(true);
        msg.setKeepAlive(512);
        ByteBuf out = Unpooled.buffer();
        
        //Exercise
        m_encoder.encode(m_mockedContext, msg, out);
        
        //Verify
        assertEquals(0x10, out.readByte()); //1 byte
        assertEquals(12, out.readByte()); //remaining length
        verifyString("MQIsdp", out);
        assertEquals(0x03, out.readByte()); //protocol version
        assertEquals(0x32, out.readByte()); //flags
        assertEquals(2, out.readByte()); //keepAliveTimer msb
        assertEquals(0, out.readByte()); //keepAliveTimer lsb
    }
    
    
    @Test
    public void testCompleteHeader() throws Exception {
        ConnectMessage msg = new ConnectMessage();
        msg.setWillRetain(true);
        msg.setWillQos((byte)2);
        msg.setWillFlag(true);
        msg.setCleanSession(true);
        msg.setKeepAlive(512);
        
        //variable part
        msg.setClientID("ABCDEF");
        msg.setWillTopic("Topic");
        msg.setWillMessage("Message".getBytes());
        
        ByteBuf out = Unpooled.buffer();
        
        //Exercise
        m_encoder.encode(m_mockedContext, msg, out);
        
        //Verify
        assertEquals(0x10, out.readByte()); //1 byte
        assertEquals(36, out.readByte()); //remaining length
        verifyString("MQIsdp", out);
        assertEquals(0x03, out.readByte()); //protocol version
        assertEquals(0x36, out.readByte()); //flags
        assertEquals(2, out.readByte()); //keepAliveTimer msb
        assertEquals(0, out.readByte()); //keepAliveTimer lsb
        
        //Variable part
        verifyString("ABCDEF", out);
        verifyString("Topic", out);
        verifyString("Message", out);
    }
    
    @Test
    public void testCompleteHeaderWIthUser_password() throws Exception {
        ConnectMessage msg = new ConnectMessage();
        msg.setUserFlag(true);
        msg.setPasswordFlag(true);
        msg.setWillRetain(true);
        msg.setWillQos((byte)2);
        msg.setWillFlag(true);
        msg.setCleanSession(true);
        msg.setKeepAlive(512);
        
        //variable part
        msg.setClientID("ABCDEF");
        msg.setWillTopic("Topic");
        msg.setWillMessage("Message".getBytes());
        msg.setUsername("Pablo");
        msg.setPassword("PBL".getBytes());
        
        ByteBuf out = Unpooled.buffer();
        
        //Exercise
        m_encoder.encode(m_mockedContext, msg, out);
        
        //Verify
        assertEquals(0x10, out.readByte()); //1 byte
        assertEquals(48, out.readByte()); //remaining length
        verifyString("MQIsdp", out);
        assertEquals(0x03, out.readByte()); //protocol version
        assertEquals((byte)0xF6, (byte)out.readByte()); //flags
        assertEquals(2, out.readByte()); //keepAliveTimer msb
        assertEquals(0, out.readByte()); //keepAliveTimer lsb
        
        //Variable part
        verifyString("ABCDEF", out);
        verifyString("Topic", out);
        verifyString("Message", out);
        verifyString("Pablo", out);//username
        verifyString("PBL", out);//password
    }
}
