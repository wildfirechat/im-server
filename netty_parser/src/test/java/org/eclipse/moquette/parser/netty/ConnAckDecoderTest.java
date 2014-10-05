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
import java.util.ArrayList;
import java.util.List;
import org.eclipse.moquette.proto.messages.AbstractMessage;
import org.eclipse.moquette.proto.messages.ConnAckMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author andrea
 */
public class ConnAckDecoderTest {
    ByteBuf m_buff;
    ConnAckDecoder m_msgdec;
    
    @Before
    public void setUp() {
        m_msgdec = new ConnAckDecoder();
    }
    
    @Test
    public void testHeader() throws Exception {
        m_buff = Unpooled.buffer(14);
        initHeader(m_buff);
        List<Object> results = new ArrayList<Object >();
        
        //Excercise
        m_msgdec.decode(null, m_buff, results);
        
        //Verify
        assertFalse(results.isEmpty());
        ConnAckMessage message = (ConnAckMessage)results.get(0); 
        assertNotNull(message);
        assertEquals(ConnAckMessage.CONNECTION_ACCEPTED, message.getReturnCode());
        assertEquals(AbstractMessage.CONNACK, message.getMessageType());
    }
    
    private void initHeader(ByteBuf buff) {
        buff.clear().writeByte(AbstractMessage.CONNACK << 4).writeByte(2);
        //reserved
        buff.writeByte((byte)0);
        //return code
        buff.writeByte(ConnAckMessage.CONNECTION_ACCEPTED);
    }
    
}
