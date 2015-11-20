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
import io.netty.channel.ChannelHandlerContext;
import io.moquette.proto.messages.AbstractMessage;
import io.moquette.proto.messages.PublishMessage;

/**
 *
 * @author andrea
 */
class PublishEncoder extends DemuxEncoder<PublishMessage> {

    @Override
    protected void encode(ChannelHandlerContext ctx, PublishMessage message, ByteBuf out) {
        if (message.getQos() == AbstractMessage.QOSType.RESERVED) {
            throw new IllegalArgumentException("Found a message with RESERVED Qos");
        }
        if (message.getTopicName() == null || message.getTopicName().isEmpty()) {
            throw new IllegalArgumentException("Found a message with empty or null topic name");
        }
        
        ByteBuf variableHeaderBuff = ctx.alloc().buffer(2);
        ByteBuf buff = null;
        try {
            variableHeaderBuff.writeBytes(Utils.encodeString(message.getTopicName()));
            if (message.getQos() == AbstractMessage.QOSType.LEAST_ONE || 
                message.getQos() == AbstractMessage.QOSType.EXACTLY_ONCE ) {
                if (message.getMessageID() == null) {
                    throw new IllegalArgumentException("Found a message with QOS 1 or 2 and not MessageID setted");
                }
                variableHeaderBuff.writeShort(message.getMessageID());
            }
            variableHeaderBuff.writeBytes(message.getPayload());
            int variableHeaderSize = variableHeaderBuff.readableBytes();

            byte flags = Utils.encodeFlags(message);

            buff = ctx.alloc().buffer(2 + variableHeaderSize);
            buff.writeByte(AbstractMessage.PUBLISH << 4 | flags);
            buff.writeBytes(Utils.encodeRemainingLength(variableHeaderSize));
            buff.writeBytes(variableHeaderBuff);
            out.writeBytes(buff);
        } finally {
            variableHeaderBuff.release();
            if (buff != null) {
                buff.release();
            }
        }
    }
    
}
