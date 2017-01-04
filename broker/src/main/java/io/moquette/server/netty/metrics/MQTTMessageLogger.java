/*
 * Copyright (c) 2012-2017 The original author or authorsgetRockQuestions()
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

import io.moquette.parser.proto.messages.*;
import io.moquette.server.netty.NettyUtils;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.moquette.parser.proto.messages.AbstractMessage.*;

/**
 *
 * @author andrea
 */
@Sharable
public class MQTTMessageLogger extends ChannelDuplexHandler {

    private static final Logger LOG = LoggerFactory.getLogger("messageLogger");

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        logMQTTMessage(ctx, message, "C->B");
        ctx.fireChannelRead(message);
    }

    private void logMQTTMessage(ChannelHandlerContext ctx, Object message, String direction) {
        if (!(message instanceof AbstractMessage)) {
            return;
        }
        AbstractMessage msg = (AbstractMessage) message;
        String clientID = NettyUtils.clientID(ctx.channel());
        switch (msg.getMessageType()) {
            case CONNECT:
                ConnectMessage connect = (ConnectMessage) msg;
                LOG.info("{} CONNECT client <{}>", direction, connect.getClientID());
                break;
            case SUBSCRIBE:
                SubscribeMessage subscribe = (SubscribeMessage) msg;
                LOG.info("{} SUBSCRIBE <{}> to topics {}", direction, clientID, subscribe.subscriptions());
                break;
            case UNSUBSCRIBE:
                UnsubscribeMessage unsubscribe = (UnsubscribeMessage) msg;
                LOG.info("{} UNSUBSCRIBE <{}> to topics <{}>", direction, clientID, unsubscribe.topicFilters());
                break;
            case PUBLISH:
                PublishMessage publish = (PublishMessage) msg;
                LOG.info("{} PUBLISH <{}> to topics <{}>", direction, clientID, publish.getTopicName());
                break;
            case PUBREC:
                PubRecMessage pubrec = (PubRecMessage) msg;
                LOG.info("{} PUBREC <{}> packetID <{}>", direction, clientID, pubrec.getMessageID());
                break;
            case PUBCOMP:
                PubCompMessage pubCompleted = (PubCompMessage) msg;
                LOG.info("{} PUBCOMP <{}> packetID <{}>", direction, clientID, pubCompleted.getMessageID());
                break;
            case PUBREL:
                PubRelMessage pubRelease = (PubRelMessage) msg;
                LOG.info("{} PUBREL <{}> packetID <{}>", direction, clientID, pubRelease.getMessageID());
                break;
            case DISCONNECT:
                LOG.info("{} DISCONNECT <{}>", direction, clientID);
                break;
            case PUBACK:
                PubAckMessage pubAck = (PubAckMessage) msg;
                LOG.info("{} PUBACK <{}> packetID <{}>", direction, clientID, pubAck.getMessageID());
                break;
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String clientID = NettyUtils.clientID(ctx.channel());
        if (clientID != null && !clientID.isEmpty()) {
            LOG.info("Channel closed <{}>", clientID);
        }
        ctx.fireChannelInactive();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        logMQTTMessage(ctx, msg, "C<-B");
        ctx.write(msg, promise);
    }
//
//    @Override
//    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
//        if (cause instanceof CorruptedFrameException) {
//            //something goes bad with decoding
//            LOG.warn("Error decoding a packet, probably a bad formatted packet, message: " + cause.getMessage());
//        } else {
//            LOG.error("Ugly error on networking", cause);
//        }
//        ctx.close();
//    }
//
//    @Override
//    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
//        if (ctx.channel().isWritable()) {
//            m_processor.notifyChannelWritable(ctx.channel());
//        }
//        ctx.fireChannelWritabilityChanged();
//    }

}
