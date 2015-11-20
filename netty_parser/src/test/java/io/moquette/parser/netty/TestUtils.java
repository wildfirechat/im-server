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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Common test methods
 * 
 * @author andrea
 */
public class TestUtils {
    
    /**
     * Verify that the given bytes buffer of the given numBytes length is present
     * in the buff starting from the current position.
     */
    static void verifyBuff(int numBytes, byte[] bytes, ByteBuf buff) {
        assertTrue(numBytes <= buff.readableBytes());
        byte[] toCheck = new byte[numBytes];
        buff.readBytes(toCheck);
        
        for (int i = 0; i < numBytes; i++) {
            assertEquals(bytes[i], toCheck[i]);
        }
    }
    
    static void verifyBuff(int numBytes, ByteBuffer bytes, ByteBuf buff) {
        assertTrue(numBytes <= buff.readableBytes());
        assertEquals(bytes, buff.nioBuffer());
    }
    
    /**
     * Verify the presence of the given string starting from the current position
     * inside the buffer.
     */
    static void verifyString(String str, ByteBuf buff) throws UnsupportedEncodingException {
        ByteBuf tmpBuff = Unpooled.buffer(2);
        byte[] raw = str.getBytes("UTF-8");
        tmpBuff.writeShort(raw.length);
        tmpBuff.writeBytes(raw);
        int buffLen = raw.length + 2;
        verifyByteBuf(tmpBuff, buff.slice(buff.readerIndex(), buffLen));
        buff.skipBytes(buffLen);
    }
    
    
    static void verifyByteBuf(ByteBuf expected, ByteBuf found) {
        assertEquals(expected, found);
    }
    
    static ChannelHandlerContext mockChannelHandler() {
        ChannelHandlerContext m_mockedContext = mock(ChannelHandlerContext.class);
        ByteBufAllocator allocator = UnpooledByteBufAllocator.DEFAULT;
        when(m_mockedContext.alloc()).thenReturn(allocator);
        return m_mockedContext;
    }
    
    static void verifyEquals(byte[] expected, byte[] found) {
        assertEquals(expected.length, found.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], found[i]);
        }
    }
    
    
    public static ByteBuf generateRandomPayload(int size) {
        ByteBuf payloadBuffer = Unpooled.buffer(size);
        for (int i = 0; i < size; i++) {
            payloadBuffer.writeByte((byte) (Math.random() * 255));
        }
        return payloadBuffer;
    }
}
