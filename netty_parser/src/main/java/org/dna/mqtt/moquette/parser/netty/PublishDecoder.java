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
package org.dna.mqtt.moquette.parser.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import java.util.List;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
class PublishDecoder extends DemuxDecoder {
    
    private static Logger LOG = LoggerFactory.getLogger(PublishDecoder.class);

    @Override
    void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        LOG.info("decode invoked with buffer {}", in);
        in.resetReaderIndex();
        int startPos = in.readerIndex();

        //Common decoding part
        PublishMessage message = new PublishMessage();
        if (!decodeCommonHeader(message, in)) {
            LOG.info("decode ask for more data after {}", in);
            in.resetReaderIndex();
            return;
        }
        int remainingLength = message.getRemainingLength();
        
        //Topic name
        String topic = Utils.decodeString(in);
        if (topic == null) {
            in.resetReaderIndex();
            return;
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
//        byte[] b = new byte[payloadSize];
        ByteBuf bb = Unpooled.buffer(payloadSize);
        in.readBytes(bb);
        message.setPayload(bb.nioBuffer());
        
        out.add(message);
    }
    
}
