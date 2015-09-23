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
import io.netty.util.AttributeMap;
import java.util.List;
import org.eclipse.moquette.proto.messages.AbstractMessage;
import org.eclipse.moquette.proto.messages.PublishMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
class PublishDecoder extends DemuxDecoder {
    
    private static Logger LOG = LoggerFactory.getLogger(PublishDecoder.class);

    @Override
    void decode(AttributeMap ctx, ByteBuf in, List<Object> out) throws Exception {
        LOG.debug("decode invoked with buffer {}", in);
        in.resetReaderIndex();
        int startPos = in.readerIndex();

        //Common decoding part
        PublishMessage message = new PublishMessage();
        if (!decodeCommonHeader(message, in)) {
            LOG.debug("decode ask for more data after {}", in);
            in.resetReaderIndex();
            return;
        }
        
        if (Utils.isMQTT3_1_1(ctx)) {
            if (message.getQos() == AbstractMessage.QOSType.MOST_ONE && message.isDupFlag()) {
                //bad protocol, if QoS=0 => DUP = 0
                throw new CorruptedFrameException("Received a PUBLISH with QoS=0 & DUP = 1, MQTT 3.1.1 violation");
            }
            
            if (message.getQos() == AbstractMessage.QOSType.RESERVED) {
                throw new CorruptedFrameException("Received a PUBLISH with QoS flags setted 10 b11, MQTT 3.1.1 violation");
            }
        }
        
        int remainingLength = message.getRemainingLength();
        
        //Topic name
        String topic = Utils.decodeString(in);
        if (topic == null) {
            in.resetReaderIndex();
            return;
        }
        //[MQTT-3.3.2-2] The Topic Name in the PUBLISH Packet MUST NOT contain wildcard characters.
        if (topic.contains("+") || topic.contains("#")) {
            throw new CorruptedFrameException("Received a PUBLISH with topic containing wild card chars, topic: " + topic);
        }
        //check topic is at least one char [MQTT-4.7.3-1]
        if (topic.length() == 0) {
            throw new CorruptedFrameException("Received a PUBLISH with topic without any character");
        }
        
        message.setTopicName(topic);
        
        if (message.getQos() == AbstractMessage.QOSType.LEAST_ONE || 
                message.getQos() == AbstractMessage.QOSType.EXACTLY_ONCE) {
            message.setMessageID(in.readUnsignedShort());
        }
        int stopPos = in.readerIndex();
        
        //read the payload
        int payloadSize = remainingLength - (stopPos - startPos - 2) + (Utils.numBytesToEncode(remainingLength) - 1);
        if (in.readableBytes() < payloadSize) {
            in.resetReaderIndex();
            return;
        }
        ByteBuf bb = Unpooled.buffer(payloadSize);
        in.readBytes(bb);
        message.setPayload(bb.nioBuffer());
        
        out.add(message);
    }
    
}
