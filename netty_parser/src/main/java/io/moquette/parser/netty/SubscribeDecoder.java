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
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.util.AttributeMap;
import java.io.UnsupportedEncodingException;
import java.util.List;
import io.moquette.proto.messages.AbstractMessage.QOSType;
import io.moquette.proto.messages.SubscribeMessage;

/**
 *
 * @author andrea
 */
class SubscribeDecoder extends DemuxDecoder {

    @Override
    void decode(AttributeMap ctx, ByteBuf in, List<Object> out) throws Exception {
        //Common decoding part
        SubscribeMessage message = new SubscribeMessage();
        in.resetReaderIndex();
        if (!decodeCommonHeader(message, 0x02, in)) {
            in.resetReaderIndex();
            return;
        }
        
        //check qos level
        if (message.getQos() != QOSType.LEAST_ONE) {
            throw new CorruptedFrameException("Received SUBSCRIBE message with QoS other than LEAST_ONE, was: " + message.getQos());
        }
            
        int start = in.readerIndex();
        //read  messageIDs
        message.setMessageID(in.readUnsignedShort());
        int read = in.readerIndex() - start;
        while (read < message.getRemainingLength()) {
            decodeSubscription(in, message);
            read = in.readerIndex() - start;
        }
        
        if (message.subscriptions().isEmpty()) {
            throw new CorruptedFrameException("subscribe MUST have got at least 1 couple topic/QoS");
        } 
        
        out.add(message);
    }
    
    /**
     * Populate the message with couple of Qos, topic
     */
    private void decodeSubscription(ByteBuf in, SubscribeMessage message) throws UnsupportedEncodingException {
        String topic = Utils.decodeString(in);
        //check topic is at least one char [MQTT-4.7.3-1]
        if (topic.length() == 0) {
            throw new CorruptedFrameException("Received a SUBSCRIBE with empty topic filter");
        }
        byte qosByte = in.readByte();
        if ((qosByte & 0xFC) > 0) { //the first 6 bits is reserved => has to be 0
            throw new CorruptedFrameException("subscribe MUST have QoS byte with reserved buts to 0, found " + Integer.toHexString(qosByte));
        }
        byte qos = (byte)(qosByte & 0x03);
        //TODO check qos id 000000xx
        message.addSubscription(new SubscribeMessage.Couple(qos, topic));
    }
    
}
