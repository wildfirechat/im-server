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

package io.moquette.broker;

import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;

@Sharable
public class NewNettyMQTTHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(NewNettyMQTTHandler.class);

    private static final String ATTR_CONNECTION = "connection";
    private static final AttributeKey<Object> ATTR_KEY_CONNECTION = AttributeKey.valueOf(ATTR_CONNECTION);

    private MQTTConnectionFactory connectionFactory;

    NewNettyMQTTHandler(MQTTConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    private static void mqttConnection(Channel channel, MQTTConnection connection) {
        channel.attr(ATTR_KEY_CONNECTION).set(connection);
    }

    private static MQTTConnection mqttConnection(Channel channel) {
        return (MQTTConnection) channel.attr(ATTR_KEY_CONNECTION).get();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) throws Exception {
        MqttMessage msg = (MqttMessage) message;
        if (msg.fixedHeader() == null) {
            throw new IOException("Unknown packet");
        }
        final MQTTConnection mqttConnection = mqttConnection(ctx.channel());
        try {
            mqttConnection.handleMessage(msg);
        } catch (Throwable ex) {
            //ctx.fireExceptionCaught(ex);
            LOG.error("Error processing protocol message: {}", msg.fixedHeader().messageType(), ex);
            ctx.channel().close().addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    LOG.info("Closed client channel due to exception in processing");
                }
            });
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        MQTTConnection connection = connectionFactory.create(ctx.channel());
        mqttConnection(ctx.channel(), connection);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        final MQTTConnection mqttConnection = mqttConnection(ctx.channel());
        mqttConnection.handleConnectionLost();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOG.error("Unexpected exception while processing MQTT message. Closing Netty channel. CId={}",
                  NettyUtils.clientID(ctx.channel()), cause);
        ctx.close().addListener(CLOSE_ON_FAILURE);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
//        if (ctx.channel().isWritable()) {
//            m_processor.notifyChannelWritable(ctx.channel());
//        }
        final MQTTConnection mqttConnection = mqttConnection(ctx.channel());
        mqttConnection.writabilityChanged();
        ctx.fireChannelWritabilityChanged();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof InflightResender.ResendNotAckedPublishes) {
            final MQTTConnection mqttConnection = mqttConnection(ctx.channel());
            mqttConnection.resendNotAckedPublishes();
        }
        ctx.fireUserEventTriggered(evt);
    }

}
