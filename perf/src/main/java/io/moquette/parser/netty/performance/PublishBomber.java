/*
 * Copyright (c) 2012-2018 The original author or authors
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

package io.moquette.parser.netty.performance;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.*;
import org.apache.commons.codec.CharEncoding;
import org.eclipse.jetty.toolchain.perf.PlatformTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;

class PublishBomber {

    private static final Logger LOG = LoggerFactory.getLogger(PublishBomber.class);

    private final EventLoopGroup workerGroup;
    private Channel channel;

    @SuppressWarnings("FutureReturnValueIgnored")
    PublishBomber(String host, int port) {
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
                    pipeline.addLast("decoder", new MqttDecoder());
                    pipeline.addLast("encoder", MqttEncoder.INSTANCE);
                }
            });

            // Start the client.
            channel = b.connect(host, port).sync().channel();
        } catch (Exception ex) {
            LOG.error("Error received in client setup", ex);
            workerGroup.shutdownGracefully();
        }
    }

    private void sendMessage(MqttMessage msg) {
        try {
            channel.writeAndFlush(msg).await().addListener(CLOSE_ON_FAILURE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void publishLoop(int messagesPerSecond, int numToSend) throws UnsupportedEncodingException {
        long pauseMicroseconds = (int) ((1.0 / messagesPerSecond) * 1000 * 1000);
        LOG.warn("PUB: Pause over the each message sent {} microsecs", pauseMicroseconds);

        LOG.info("PUB: publishing..");
        final long startTime = System.currentTimeMillis();

        //initialize the timer
        PlatformTimer timer = PlatformTimer.detect();
        for (int i = 0; i < numToSend; i++) {
            long nanos = System.nanoTime();
            byte[] rawContent = ("Hello world!!-" + nanos).getBytes(CharEncoding.UTF_8);
            ByteBuf payload = Unpooled.copiedBuffer(rawContent);

            MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE,
                    false, 0);
            MqttPublishVariableHeader varHeader = new MqttPublishVariableHeader("/topic", 0);
            MqttPublishMessage pubMessage = new MqttPublishMessage(fixedHeader, varHeader, payload);

            sendMessage(pubMessage);
            timer.sleep(pauseMicroseconds);
        }
        LOG.info("PUB: published in {} ms", System.currentTimeMillis() - startTime);
    }

    public void disconnect() {
        try {
            this.channel.disconnect().await().addListener(CLOSE_ON_FAILURE);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
