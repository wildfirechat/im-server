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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.moquette.parser.netty.performance.NettyPublishReceiverHandler.payload2Str;
import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;

@ChannelHandler.Sharable
class PublishReceiverHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(PublishReceiverHandler.class);
    private Histogram forthNetworkTime = new Histogram(5);

    PublishReceiverHandler() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        MqttMessage msg = (MqttMessage) message;
        MqttMessageType messageType = msg.fixedHeader().messageType();

        try {
            switch (messageType) {
                case PUBLISH:
                    LOG.info("Received a message of type {}", messageType);
                    handlePublish((MqttPublishMessage) msg);
                    return;
                default:
                    LOG.info("Received a message of type {}", messageType);
            }
        } catch (Exception ex) {
            LOG.error("Bad error in processing the message", ex);
        }
    }

    private void handlePublish(MqttPublishMessage msg) {
        long start = System.nanoTime();
        LOG.debug("push forward message the topic {}", msg.variableHeader().topicName());
        LOG.debug("content <{}>", payload2Str(msg.payload()));
        String decodedPayload = payload2Str(msg.payload());
        long sentTime = Long.parseLong(decodedPayload.split("-")[1]);
        forthNetworkTime.recordValue(start - sentTime);

        long stop = System.nanoTime();
        LOG.info("Request processed in {} ns, matching {}", stop - start, payload2Str(msg.payload()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOG.info("Received channel inactive");
        ctx.channel().close().addListener(CLOSE_ON_FAILURE);

        System.out.println("Network time histogram (microsecs)");
        this.forthNetworkTime.outputPercentileDistribution(System.out, 1000.0);
    }

}
