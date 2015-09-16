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
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.util.Attribute;
import io.netty.util.AttributeMap;
import io.netty.util.DefaultAttributeMap;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import static org.eclipse.moquette.parser.netty.Utils.VERSION_3_1;
import static org.eclipse.moquette.parser.netty.Utils.VERSION_3_1_1;
import org.eclipse.moquette.proto.messages.AbstractMessage;
import org.eclipse.moquette.proto.messages.ConnectMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;


public class ConnectDecoderTest {
    
    ByteBuf m_buff;
    ConnectDecoder m_msgdec;
    AttributeMap attrMap;
    
    @Before
    public void setUp() {
        m_msgdec = new ConnectDecoder();
        attrMap = new DefaultAttributeMap();
    }
    
    @Test
    public void testBaseHeader() throws Exception {
        m_buff = Unpooled.buffer(14);
        initBaseHeader(m_buff);
        List<Object> results = new ArrayList<Object >();
        
        //Excercise
        m_msgdec.decode(this.attrMap, m_buff, results);
        
        //Verify
        assertFalse(results.isEmpty());
        verifyBaseHeader((ConnectMessage)results.get(0));
    }
    
    @Test
    public void testBaseHeader_311() throws UnsupportedEncodingException {
        m_buff = Unpooled.buffer(12);
        initBaseHeader311(m_buff);
        List<Object> results = new ArrayList<>();
        
        //Excercise
        m_msgdec.decode(this.attrMap, m_buff, results);
        
        //Verify
        assertFalse(results.isEmpty());
        verifyBaseHeader311((ConnectMessage)results.get(0));
        Attribute<Integer> attr = this.attrMap.attr(MQTTDecoder.PROTOCOL_VERSION);
        assertEquals(VERSION_3_1_1, attr.get().intValue());
    }
    
    @Test(expected = CorruptedFrameException.class)
    public void testBaseHeader_311_withFlagsTouched() throws UnsupportedEncodingException {
        m_buff = Unpooled.buffer(12);
        initBaseHeader311_withFixedFlags(m_buff, (byte) 0x01);
        List<Object> results = new ArrayList<Object>();
        
        //Excercise
        m_msgdec.decode(this.attrMap, m_buff, results);
        
        //Verify
        assertFalse(results.isEmpty());
        verifyBaseHeader311((ConnectMessage)results.get(0));
        Attribute<Integer> attr = this.attrMap.attr(MQTTDecoder.PROTOCOL_VERSION);
        assertEquals(VERSION_3_1_1, attr.get().intValue());
    }
    
    @Test(expected = CorruptedFrameException.class)
    public void testDoubleConnectInTheSameSession() throws Exception {
        //setup the connection session as already connected
        Attribute<Boolean> connectAttr = this.attrMap.attr(ConnectDecoder.CONNECT_STATUS);
        connectAttr.set(true);
        
        m_buff = Unpooled.buffer(12);
        initBaseHeader311(m_buff);
        List<Object> results = new ArrayList<Object>();
        
        //Excercise
        m_msgdec.decode(this.attrMap, m_buff, results);
    }
    
    @Test(expected = CorruptedFrameException.class)
    public void testConnectFlags_311_withNot0Reserved() throws UnsupportedEncodingException {
        m_buff = Unpooled.buffer(12);
        initBaseHeader311_withFixedFlags(m_buff, (byte) 0, (byte) 0xCF); // sets the bit(0) = 1
        List<Object> results = new ArrayList<Object>();
        
        //Excercise
        m_msgdec.decode(this.attrMap, m_buff, results);
        
        //Verify
        assertFalse(results.isEmpty());
        verifyBaseHeader311((ConnectMessage)results.get(0));
        Attribute<Integer> attr = this.attrMap.attr(MQTTDecoder.PROTOCOL_VERSION);
        assertEquals(VERSION_3_1_1, attr.get().intValue());
    }
    
    @Test
    public void testBaseHeader_ClientID() throws UnsupportedEncodingException, Exception {
        m_buff = Unpooled.buffer(40);
        initHeader(m_buff, (byte) 38);
        encodeString(m_buff, "ABCDEFGH");
        encodeString(m_buff, "Topic");
        encodeString(m_buff, "Message");
        List<Object> results = new ArrayList<Object >();
        
        //Excercise
        m_msgdec.decode(this.attrMap, m_buff, results);
        
        //Verify
        assertFalse(results.isEmpty());
        ConnectMessage message = (ConnectMessage)results.get(0); 
        verifyBaseHeader(message);
        assertEquals("ABCDEFGH", message.getClientID());
        assertEquals("Topic", message.getWillTopic());
        assertEquals("Message", new String(message.getWillMessage()));
    }
    
    
    @Test
    public void testBaseHeader_extra_with_user_pwd() throws UnsupportedEncodingException, Exception {
        m_buff = Unpooled.buffer(55);
        initHeader(m_buff, (byte) 53);
        encodeString(m_buff, "ABCDEFGH");
        encodeString(m_buff, "Topic");
        encodeString(m_buff, "Message");
        encodeString(m_buff, "Fakeuser");
        encodeString(m_buff, "pwd");
        List<Object> results = new ArrayList<Object >();
        
        //Excercise
        m_msgdec.decode(this.attrMap, m_buff, results);
        
        //Verify
        assertFalse(results.isEmpty());
        ConnectMessage message = (ConnectMessage)results.get(0); 
        verifyBaseHeader(message);
        assertEquals(AbstractMessage.CONNECT, message.getMessageType());
        assertEquals("ABCDEFGH", message.getClientID());
        assertEquals("Topic", message.getWillTopic());
        assertEquals("Message", new String(message.getWillMessage()));
        assertEquals("Fakeuser", message.getUsername());
        assertEquals("pwd", new String(message.getPassword()));
    }
    
    @Test(expected = CorruptedFrameException.class)
    public void testBadFlagUserPwd() throws Exception {
        m_buff = Unpooled.buffer(14);
        m_buff.writeByte((AbstractMessage.CONNECT << 4)).writeByte(12);
        //Proto name
        encodeString(m_buff, "MQIsdp");
        //version
        m_buff.writeByte(VERSION_3_1);
        //conn flags
        m_buff.writeByte(0x4E); //sets user to false and password to true
        //keepAlive
        m_buff.writeByte(0).writeByte(0x0A);
        List<Object> results = new ArrayList<Object>();
        
        //Excercise
        m_msgdec.decode(this.attrMap, m_buff, results);
    }
    
    
    private void initBaseHeader(ByteBuf buff) throws UnsupportedEncodingException {
        initHeader(buff, (byte)0x0C);
    }
    
    private void initHeader(ByteBuf buff, byte remaingLength) throws UnsupportedEncodingException {
        buff.clear().writeByte((byte)(AbstractMessage.CONNECT << 4)).writeByte(remaingLength);
        //Proto name
        encodeString(buff, "MQIsdp");
        //version
        buff.writeByte(3);
        //conn flags
        buff.writeByte(0xCE);
        //keepAlive
        buff.writeBytes(new byte[]{(byte)0, (byte) 0x0A});
    }
    
    private void initBaseHeader311(ByteBuf buff) throws UnsupportedEncodingException {
        initBaseHeader311_withFixedFlags(buff, (byte) 0);
    }
    
    private void initBaseHeader311_withFixedFlags(ByteBuf buff, byte fixedFlags) throws UnsupportedEncodingException {
        initBaseHeader311_withFixedFlags(buff, fixedFlags, (byte) 0xCE);
    }
    
    private void initBaseHeader311_withFixedFlags(ByteBuf buff, byte fixedFlags, byte connectFlags) throws UnsupportedEncodingException {
        buff.clear().writeByte((byte)(AbstractMessage.CONNECT << 4) | fixedFlags).writeByte((byte)0x0A);
        //Proto name
        encodeString(buff, "MQTT");
        //version
        buff.writeByte(VERSION_3_1_1);
        //conn flags
        buff.writeByte(connectFlags);
        //keepAlive
        buff.writeBytes(new byte[]{(byte)0, (byte) 0x0A});
    }
    
    /**
     * Encode and insert the given string into the given buff
     */
    private ByteBuf encodeString(ByteBuf buff, String str) throws UnsupportedEncodingException {
        buff.writeBytes(Utils.encodeString(str));
        return buff;
    }
    
    private void verifyBaseHeader(ConnectMessage connMessage) {
        assertNotNull(connMessage);
        assertEquals("MQIsdp", connMessage.getProtocolName());
        assertEquals(VERSION_3_1, connMessage.getProtocolVersion());
        assertTrue(connMessage.isUserFlag());
        assertTrue(connMessage.isPasswordFlag());
        assertTrue(connMessage.isCleanSession());
        assertEquals(10, connMessage.getKeepAlive());
        assertTrue(connMessage.isWillFlag());
        assertFalse(connMessage.isWillRetain());
        assertEquals(1, connMessage.getWillQos());
    }
    
    private void verifyBaseHeader311(ConnectMessage connMessage) {
        assertNotNull(connMessage);
        assertEquals("MQTT", connMessage.getProtocolName());
        assertEquals(VERSION_3_1_1, connMessage.getProtocolVersion());
        assertTrue(connMessage.isUserFlag());
        assertTrue(connMessage.isPasswordFlag());
        assertTrue(connMessage.isCleanSession());
        assertEquals(10, connMessage.getKeepAlive());
        assertTrue(connMessage.isWillFlag());
        assertFalse(connMessage.isWillRetain());
        assertEquals(1, connMessage.getWillQos());
    }
}