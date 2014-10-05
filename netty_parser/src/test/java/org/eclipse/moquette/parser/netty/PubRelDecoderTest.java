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
import io.netty.util.AttributeMap;
import io.netty.util.DefaultAttributeMap;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.moquette.proto.messages.AbstractMessage;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author andrea
 */
public class PubRelDecoderTest {
    
    private static final int MESSAGE_ID = 123;
    
    ByteBuf m_buff;
    PubRelDecoder m_msgdec;
    AttributeMap attrMap;
    
    @Before
    public void setUp() {
        m_msgdec = new PubRelDecoder();
        attrMap = new DefaultAttributeMap();
    }
    
    @Test
    public void testValidPurRel() throws Exception {
        m_buff = Unpooled.buffer(4);
        m_buff.clear().writeByte(AbstractMessage.PUBREL << 4 | 0x02).writeByte(4);
        m_buff.writeShort(MESSAGE_ID);  //fake message_id
        
        List<Object> results = new ArrayList<Object>();
        
        //Excercise
        m_msgdec.decode(this.attrMap, m_buff, results);
        
        //Verify
        assertFalse(results.isEmpty());
    }
    
    
    @Test(expected = CorruptedFrameException.class)
    public void testInvalidPurRel_badReservedFlags() throws Exception {
        m_buff = Unpooled.buffer(4);
        m_buff.clear().writeByte(AbstractMessage.PUBREL << 4 | 0x03).writeByte(4);
        m_buff.writeShort(MESSAGE_ID);  //fake message_id
        
        List<Object> results = new ArrayList<Object>();
        
        //Excercise
        m_msgdec.decode(this.attrMap, m_buff, results);
    }
    
}
