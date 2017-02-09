/*
 * Copyright (c) 2012-2016 The original author or authors
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

package io.moquette.server.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Auto-flush data on channel after a read timeout. It's inspired by IdleStateHandler but it's
 * specialized version, just flushing data after no read is done on the channel after a period. It's
 * used to avoid aggressively flushing from the ProtocolProcessor.
 */
public class AutoFlushHandler extends ChannelDuplexHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AutoFlushHandler.class);
    private static final long MIN_TIMEOUT_NANOS = TimeUnit.MILLISECONDS.toNanos(1);

    private final long writerIdleTimeNanos;
    volatile ScheduledFuture<?> writerIdleTimeout;
    volatile long lastWriteTime;
    // private boolean firstWriterIdleEvent = true;

    private volatile int state; // 0 - none, 1 - initialized, 2 - destroyed

    public AutoFlushHandler(long writerIdleTime, TimeUnit unit) {
        if (unit == null) {
            throw new NullPointerException("unit");
        }
        writerIdleTimeNanos = Math.max(unit.toNanos(writerIdleTime), MIN_TIMEOUT_NANOS);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive() && ctx.channel().isRegistered()) {
            // channelActive() event has been fired already, which means this.channelActive() will
            // not be invoked. We have to initialize here instead.
            initialize(ctx);
        } else {
            // channelActive() event has not been fired yet. this.channelActive() will be invoked
            // and initialization will occur there.
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        destroy();
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        // Initialize early if channel is active already.
        if (ctx.channel().isActive()) {
            initialize(ctx);
        }
        super.channelRegistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // This method will be invoked only if this handler was added
        // before channelActive() event is fired. If a user adds this handler
        // after the channelActive() event, initialize() will be called by beforeAdd().
        initialize(ctx);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        destroy();
        super.channelInactive(ctx);
    }

    // @Override
    // public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    // if (writerrIdleTimeNanos > 0) {
    // reading = true;
    // firstReaderIdleEvent = true;
    // }
    // ctx.fireChannelRead(msg);
    // }
    //
    // @Override
    // public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    // if (writerrIdleTimeNanos > 0) {
    // lastReadTime = System.nanoTime();
    // reading = false;
    // }
    // ctx.fireChannelReadComplete();
    // }

    private void initialize(ChannelHandlerContext ctx) {
        // Avoid the case where destroy() is called before scheduling timeouts.
        // See: https://github.com/netty/netty/issues/143
        if (LOG.isDebugEnabled()) {
            LOG.debug("Initializing autoflush handler. MqttClientId = {}.", NettyUtils.clientID(ctx.channel()));
        }
        switch (state) {
            case 1:
            case 2:
                return;
        }

        state = 1;

        EventExecutor loop = ctx.executor();

        lastWriteTime = System.nanoTime();
        writerIdleTimeout = loop.schedule(new WriterIdleTimeoutTask(ctx), writerIdleTimeNanos, TimeUnit.NANOSECONDS);
    }

    private void destroy() {
        state = 2;

        if (writerIdleTimeout != null) {
            writerIdleTimeout.cancel(false);
            writerIdleTimeout = null;
        }
    }

    /**
     * Is called when the write timeout expire.
     *
     * @param ctx
     *            the channel context.
     * @throws Exception
     *             in case of any IO error.
     */
    protected void channelIdle(ChannelHandlerContext ctx/* , IdleStateEvent evt */) throws Exception {
        // ctx.fireUserEventTriggered(evt);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Flushing idle Netty channel. MqttClientId = {}.", NettyUtils.clientID(ctx.channel()));
        }
        ctx.channel().flush();
    }

    private final class WriterIdleTimeoutTask implements Runnable {

        private final ChannelHandlerContext ctx;

        WriterIdleTimeoutTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            if (!ctx.channel().isOpen()) {
                return;
            }

            // long lastWriteTime = IdleStateHandler.this.lastWriteTime;
            // long lastWriteTime = lastWriteTime;
            long nextDelay = writerIdleTimeNanos - (System.nanoTime() - lastWriteTime);
            if (nextDelay <= 0) {
                // Writer is idle - set a new timeout and notify the callback.
                writerIdleTimeout = ctx.executor().schedule(this, writerIdleTimeNanos, TimeUnit.NANOSECONDS);
                try {
                    /*
                     * IdleStateEvent event; if (firstWriterIdleEvent) { firstWriterIdleEvent =
                     * false; event = IdleStateEvent.FIRST_WRITER_IDLE_STATE_EVENT; } else { event =
                     * IdleStateEvent.WRITER_IDLE_STATE_EVENT; }
                     */
                    channelIdle(ctx/* , event */);
                } catch (Throwable t) {
                    ctx.fireExceptionCaught(t);
                }
            } else {
                // Write occurred before the timeout - set a new timeout with shorter delay.
                writerIdleTimeout = ctx.executor().schedule(this, nextDelay, TimeUnit.NANOSECONDS);
            }
        }
    }
}
