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
import java.util.ArrayList;
import java.util.List;
import org.eclipse.moquette.proto.messages.AbstractMessage;
import org.eclipse.moquette.proto.messages.SubAckMessage;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author andrea
 */
public class SubAckDecoderTest {
    ByteBuf m_buff;
    SubAckDecoder m_msgdec;
    List<Object> m_results;
    
    @Before
    public void setUp() {
        m_msgdec = new SubAckDecoder();
        m_results = new ArrayList<Object>();
        m_buff = Unpooled.buffer(7);
    }
    
        @Test
    public void testBadQos() throws Exception {
        initHeaderQos(m_buff, 0xAABB, AbstractMessage.QOSType.LEAST_ONE, AbstractMessage.QOSType.MOST_ONE, AbstractMessage.QOSType.MOST_ONE);

        //Excercise
        m_msgdec.decode(null, m_buff, m_results);

        //Verify
        assertFalse(m_results.isEmpty());
        SubAckMessage message = (SubAckMessage)m_results.get(0); 
        assertNotNull(message);
        assertEquals(0xAABB, message.getMessageID().intValue());
        List<AbstractMessage.QOSType> qoses = message.types();
        assertEquals(3, qoses.size());
        assertEquals(AbstractMessage.QOSType.LEAST_ONE, qoses.get(0));
        assertEquals(AbstractMessage.QOSType.MOST_ONE, qoses.get(1));
        assertEquals(AbstractMessage.QOSType.MOST_ONE, qoses.get(2));
        assertEquals(AbstractMessage.SUBACK, message.getMessageType());
    }
    
    
    @Test
    public void testBugBadRemainingCalculation() throws Exception {
        byte[] overallMessage = new byte[] {(byte)0x90, 0x03, //fixed header
             0x00, 0x0A, //MSG ID
             0x01}; //QoS array
         m_buff = Unpooled.buffer(overallMessage.length);
         m_buff.writeBytes(overallMessage);
         
        //Exercise
        m_msgdec.decode(null, m_buff, m_results);

        assertFalse(m_results.isEmpty());
        SubAckMessage message = (SubAckMessage)m_results.get(0); 
        assertNotNull(message);
        assertEquals(0x0A, message.getMessageID().intValue());
        assertEquals(1, message.types().size());
        assertEquals(AbstractMessage.QOSType.LEAST_ONE, message.types().get(0));
    }

    @Test
    public void testEncodeWithFailureQOS() throws Exception {
        initHeaderQos(m_buff, 0xAABB, AbstractMessage.QOSType.FAILURE);

        //Exercise
        m_msgdec.decode(null, m_buff, m_results);

        //Verify
        assertFalse(m_results.isEmpty());
        SubAckMessage message = (SubAckMessage)m_results.get(0);
        assertNotNull(message);
        assertEquals(0xAABB, message.getMessageID().intValue());
        List<AbstractMessage.QOSType> qoses = message.types();
        assertEquals(1, qoses.size());
        assertEquals(AbstractMessage.QOSType.FAILURE, qoses.get(0));
        assertEquals(AbstractMessage.SUBACK, message.getMessageType());
    }

    private void initHeaderQos(ByteBuf buff, int messageID, AbstractMessage.QOSType... qoss) throws IllegalAccessException {
        buff.clear().writeByte(AbstractMessage.SUBACK << 4).
                writeBytes(Utils.encodeRemainingLength(2 + qoss.length));
        
        buff.writeShort(messageID);
        for (AbstractMessage.QOSType qos : qoss) {
            buff.writeByte(qos.byteValue());
        }
    }
}
