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
package org.eclipse.moquette.server.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import org.eclipse.moquette.parser.netty.ConnectDecoder;
import org.eclipse.moquette.spi.IMessaging;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.List;

import static org.mockito.Mockito.mock;

/**
 *
 * @author andrea
 */
public class NettyMQTTHandlerTest {

//    class ConnectDecoderAdapter extends ByteToMessageDecoder {
//
//        final ConnectDecoder decoder;
//
//        ConnectDecoderAdapter(ConnectDecoder decoder) {
//            this.decoder = decoder;
//        }
//
//        @Override
//        protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) throws Exception {
//            decoder.decode(ctx, byteBuf, list);
//        }
//    }

    class ThrowingExceptionInboundHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            throw new CorruptedFrameException("Invalid protoName: SUPERFAKE");
        }
    }

    /**
     * Identified by issue #65
     * */
    @Test
    public void testCatchingCorruptedFrameException() {
        IMessaging mockMsg = mock(IMessaging.class);
        NettyMQTTHandler sut = new NettyMQTTHandler();
        sut.setMessaging(mockMsg);
        EmbeddedChannel channel = new EmbeddedChannel(new ThrowingExceptionInboundHandler(), sut);

        //prepare a packet with bad protocol name
        ByteBuf input = Unpooled.buffer();
        input.writeByte(0x01);

        //Exercise
        assertTrue(channel.isOpen());
        channel.writeInbound(input.readBytes(1));

        //Verify the handler close the channel
        assertFalse(channel.isOpen());
    }
}
