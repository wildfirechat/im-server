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

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.moquette.server.netty.NettyUtils.ATTR_USERNAME;

public class MessageMetricsHandler extends ChannelDuplexHandler {
    private static final Logger LOG = LoggerFactory.getLogger(MessageMetricsHandler.class);
    private static final AttributeKey<MessageMetrics> ATTR_KEY_METRICS = AttributeKey.valueOf("MessageMetrics");
    private static final AttributeKey<String> ATTR_KEY_USERNAME = AttributeKey.valueOf(ATTR_USERNAME);


    private MessageMetricsCollector m_collector;

    public MessageMetricsHandler(MessageMetricsCollector collector) {
        m_collector = collector;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Attribute<MessageMetrics> attr = ctx.channel().attr(ATTR_KEY_METRICS);
        attr.set(new MessageMetrics());

        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        MessageMetrics metrics = ctx.channel().attr(ATTR_KEY_METRICS).get();
        metrics.incrementRead(1);
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        MessageMetrics metrics = ctx.channel().attr(ATTR_KEY_METRICS).get();
        metrics.incrementWrote(1);
        ctx.write(msg, promise);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        MessageMetrics metrics = ctx.channel().attr(ATTR_KEY_METRICS).get();
        String userId = ctx.channel().attr(ATTR_KEY_USERNAME).get();
        if (userId == null) {
            userId = "";
        }

        LOG.info("channel<{}> closing after read {} messages and wrote {} messages", userId,  metrics.messagesRead(), metrics.messagesWrote());
        m_collector.sumReadMessages(metrics.messagesRead());
        m_collector.sumWroteMessages(metrics.messagesWrote());
        super.close(ctx, promise);
    }

    public static MessageMetrics getMessageMetrics(Channel channel) {
        return channel.attr(ATTR_KEY_METRICS).get();
    }
}
