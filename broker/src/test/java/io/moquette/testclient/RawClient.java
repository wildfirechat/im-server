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

package io.moquette.testclient;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.ReferenceCountUtil;
import io.moquette.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * This class is used to have a fluent interface to interact with a server. Inspired by
 * org.kaazing.robot
 */
public final class RawClient {

    @ChannelHandler.Sharable
    class RawMessageHandler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object message) {
            LOG.info("Received a message {}", message);
            try {
                ByteBuf in = (ByteBuf) message;
                int readBytes = in.readableBytes();
                heapBuffer.writeBytes(in);
                readableBytesSem.release(readBytes);
            } finally {
                ReferenceCountUtil.release(message);
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            // m_client.setConnectionLost(true);
            disconnectLatch.countDown();
            ctx.close(/* false */);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(RawClient.class);
    final RawMessageHandler handler;
    EventLoopGroup workerGroup;
    Channel m_channel;

    private boolean connected;
    private ByteBuf heapBuffer;
    private CountDownLatch disconnectLatch;
    private final Semaphore readableBytesSem;

    private RawClient(String host, int port) {
        handler = new RawMessageHandler();
        heapBuffer = Unpooled.buffer(128);
        disconnectLatch = new CountDownLatch(1);
        readableBytesSem = new Semaphore(0, true);

        workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {

                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("handler", handler);
                }
            });

            // Start the client.
            m_channel = b.connect(host, port).sync().channel();
            this.connected = true;
        } catch (Exception ex) {
            LOG.error("Error received in client setup", ex);
            workerGroup.shutdownGracefully();
        }
    }

    public static RawClient connect(String host, int port) {
        return new RawClient(host, port);
    }

    public RawClient isConnected() {
        if (!this.connected) {
            throw new IllegalStateException("Can't connect the client");
        }
        return this;
    }

    public RawClient write(int... bytes) {
        ByteBuf buff = Unpooled.buffer(bytes.length);
        buff.clear();
        for (int b : bytes) {
            buff.writeByte((byte) b);
        }
        m_channel.write(buff);
        return this;
    }

    public RawClient writeWithSize(String str) {
        ByteBuf buff = Unpooled.buffer(str.length() + 2);
        buff.writeBytes(Utils.encodeString(str));
        m_channel.write(buff);
        return this;
    }

    /**
     * Write just String bytes not length
     */
    public RawClient write(String str) {
        ByteBuf out = Unpooled.buffer(str.length());
        byte[] raw;
        try {
            raw = str.getBytes("UTF-8");
            // NB every Java platform has got UTF-8 encoding by default, so this
            // exception are never raised.
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
        out.writeBytes(raw);
        m_channel.write(out);
        return this;
    }

    public RawClient flush() {
        m_channel.flush();
        return this;
    }

    public RawClient read(int expectedByte) {
        try {
            readableBytesSem.acquire(1);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted while waiting data");
        }
        byte b = heapBuffer.readByte();
        if (b != expectedByte) {
            throw new IllegalStateException(String.format("Expected byte 0x%02X but found 0x%02X", b, expectedByte));
        }
        return this;
    }

    /**
     * Expect the closing of the underling channel, with timeout
     */
    public void closed(long timeout) throws InterruptedException {
        disconnectLatch.await(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Expect closing the connect with unbound time
     */
    public void closed() throws InterruptedException {
        disconnectLatch.await();
    }
}
