/*
 * Copyright (c) 2012-2017 The original author or authors
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

package io.moquette.server.netty.metrics;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public class BytesMetricsHandler extends ChannelDuplexHandler {

    private static final AttributeKey<BytesMetrics> ATTR_KEY_METRICS = AttributeKey.valueOf("BytesMetrics");

    private BytesMetricsCollector m_collector;

    public BytesMetricsHandler(BytesMetricsCollector collector) {
        m_collector = collector;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Attribute<BytesMetrics> attr = ctx.channel().attr(ATTR_KEY_METRICS);
        attr.set(new BytesMetrics());

        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        BytesMetrics metrics = ctx.channel().attr(ATTR_KEY_METRICS).get();
        metrics.incrementRead(((ByteBuf) msg).readableBytes());
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        BytesMetrics metrics = ctx.channel().attr(ATTR_KEY_METRICS).get();
        metrics.incrementWrote(((ByteBuf) msg).writableBytes());
        ctx.write(msg, promise);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        BytesMetrics metrics = ctx.channel().attr(ATTR_KEY_METRICS).get();
        m_collector.sumReadBytes(metrics.readBytes());
        m_collector.sumWroteBytes(metrics.wroteBytes());
        super.close(ctx, promise);
    }

    public static BytesMetrics getBytesMetrics(Channel channel) {
        return channel.attr(ATTR_KEY_METRICS).get();
    }
}
