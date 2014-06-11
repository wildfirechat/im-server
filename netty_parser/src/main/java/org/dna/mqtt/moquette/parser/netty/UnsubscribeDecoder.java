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
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import java.util.List;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.UnsubscribeMessage;

/**
 *
 * @author andrea
 */
class UnsubscribeDecoder extends DemuxDecoder {

    @Override
    void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //Common decoding part
        in.resetReaderIndex();
        UnsubscribeMessage message = new UnsubscribeMessage();
        if (!decodeCommonHeader(message, in)) {
            in.resetReaderIndex();
            return;
        }
        
        //check qos level
        if (message.getQos() != AbstractMessage.QOSType.LEAST_ONE) {
            throw new CorruptedFrameException("Found an Usubscribe message with qos other than LEAST_ONE, was: " + message.getQos());
        }
            
        int start = in.readerIndex();
        //read  messageIDs
        message.setMessageID(in.readUnsignedShort());
        int readed = in.readerIndex()- start;
        while (readed < message.getRemainingLength()) {
            message.addTopic(Utils.decodeString(in));
            readed = in.readerIndex()- start;
        }
        
        out.add(message);
    }
    
}
