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
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.eclipse.moquette.parser.netty.TestUtils.*;
import org.eclipse.moquette.proto.messages.AbstractMessage;
import org.eclipse.moquette.proto.messages.UnsubscribeMessage;

/**
 *
 * @author andrea
 */
/*@RunWith(Suite.class)
@Suite.SuiteClasses({})*/
public class UtilsTest {
    
    ByteBuf m_buff;
    
    @Before
    public void setUp() { 
        m_buff = Unpooled.buffer(4);
    }

    @Test
    public void testDecodeRemainingLength() {
        //1 byte length
        m_buff.writeByte(0x0);
        assertEquals(0, Utils.decodeRemainingLenght(m_buff));
        m_buff.clear().writeByte(0x007F);
        assertEquals(127, Utils.decodeRemainingLenght(m_buff));
        
        //2 byte length
        m_buff.clear().writeBytes(new byte[]{(byte)0x80, (byte)0x01});
        assertEquals(128, Utils.decodeRemainingLenght(m_buff));
        m_buff.clear().writeBytes(new byte[]{(byte)0xFF, (byte)0x7F});
        assertEquals(16383, Utils.decodeRemainingLenght(m_buff));
        
        //3 byte length
        m_buff.clear().writeBytes(new byte[]{(byte)0x80, (byte)0x80, (byte)0x01});
        assertEquals(16384, Utils.decodeRemainingLenght(m_buff));
        m_buff.clear().writeBytes(new byte[]{(byte)0xFF, (byte)0xFF, (byte)0x7F});
        assertEquals(2097151, Utils.decodeRemainingLenght(m_buff));
        
        //4 byte length
        m_buff.clear().writeBytes(new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, (byte)0x01});
        assertEquals(2097152, Utils.decodeRemainingLenght(m_buff));
        m_buff.clear().writeBytes(new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0x7F});
        assertEquals(268435455, Utils.decodeRemainingLenght(m_buff));
    }
    
    @Test(expected=CorruptedFrameException.class)
    public void testEncodeRemainingLength_invalid_upper() {
        Utils.encodeRemainingLength(Utils.MAX_LENGTH_LIMIT + 1);
    }
    
    @Test(expected=CorruptedFrameException.class)
    public void testEncodeRemainingLength_invalid_lower() {
        Utils.encodeRemainingLength(-1);
    }
    
    @Test
    public void testEncodeRemainingLenght() {
        //1 byte length
        verifyBuff(1, new byte[]{0}, Utils.encodeRemainingLength(0));
        verifyBuff(1, new byte[]{0x7F}, Utils.encodeRemainingLength(127));
        
        //2 byte length
        verifyBuff(2, new byte[]{(byte)0x80, 0x01}, Utils.encodeRemainingLength(128));
        verifyBuff(2, new byte[]{(byte)0xFF, 0x7F}, Utils.encodeRemainingLength(16383));
        
        //3 byte length
        verifyBuff(3, new byte[]{(byte)0x80, (byte)0x80, 0x01}, Utils.encodeRemainingLength(16384));
        verifyBuff(3, new byte[]{(byte)0xFF, (byte)0xFF, 0x7F}, Utils.encodeRemainingLength(2097151));
        
        //4 byte length
        verifyBuff(4, new byte[]{(byte)0x80, (byte)0x80, (byte)0x80, 0x01}, Utils.encodeRemainingLength(2097152));
        verifyBuff(4, new byte[]{(byte)0xFF, (byte)0xFF, (byte)0xFF, 0x7F}, Utils.encodeRemainingLength(268435455));
    }
    
    @Test
    public void testEncodeFlags() {
        UnsubscribeMessage msg = new UnsubscribeMessage();
        msg.setRetainFlag(true);
        msg.setQos(AbstractMessage.QOSType.MOST_ONE);
        
        //Exercise
        assertEquals(1, Utils.encodeFlags(msg));
    }
}
