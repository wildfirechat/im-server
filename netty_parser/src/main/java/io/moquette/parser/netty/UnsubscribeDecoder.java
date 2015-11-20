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

import io.moquette.proto.messages.UnsubscribeMessage;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.util.AttributeMap;
import java.util.List;
import io.moquette.proto.messages.AbstractMessage;

/**
 *
 * @author andrea
 */
class UnsubscribeDecoder extends DemuxDecoder {

    @Override
    void decode(AttributeMap ctx, ByteBuf in, List<Object> out) throws Exception {
        //Common decoding part
        in.resetReaderIndex();
        UnsubscribeMessage message = new UnsubscribeMessage();
        if (!decodeCommonHeader(message, 0x02, in)) {
            in.resetReaderIndex();
            return;
        }
        
        //check qos level
        if (message.getQos() != AbstractMessage.QOSType.LEAST_ONE) {
            throw new CorruptedFrameException("Found an Unsubscribe message with qos other than LEAST_ONE, was: " + message.getQos());
        }
            
        int start = in.readerIndex();
        //read  messageIDs
        message.setMessageID(in.readUnsignedShort());
        int read = in.readerIndex() - start;
        while (read < message.getRemainingLength()) {
            String topicFilter = Utils.decodeString(in);
            //check topic is at least one char [MQTT-4.7.3-1]
            if (topicFilter.length() == 0) {
                throw new CorruptedFrameException("Received an UNSUBSCRIBE with empty topic filter");
            }
            message.addTopicFilter(topicFilter);
            read = in.readerIndex() - start;
        }
        if (message.topicFilters().isEmpty()) {
            throw new CorruptedFrameException("unsubscribe MUST have got at least 1 topic");
        }
        out.add(message);
    }
    
}
