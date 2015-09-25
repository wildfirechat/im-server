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

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import io.netty.handler.codec.CorruptedFrameException;
import org.eclipse.moquette.proto.messages.*;
import org.eclipse.moquette.spi.IMessaging;
import org.eclipse.moquette.proto.Utils;

import static org.eclipse.moquette.proto.messages.AbstractMessage.*;

import org.eclipse.moquette.spi.impl.ProtocolProcessor;
import org.eclipse.moquette.spi.impl.events.LostConnectionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
@Sharable
public class NettyMQTTHandler extends ChannelInboundHandlerAdapter {
    
    private static final Logger LOG = LoggerFactory.getLogger(NettyMQTTHandler.class);
    private IMessaging m_messaging;
    private final ProtocolProcessor m_processor;

    private NettyMQTTHandler() {
        m_processor = null;
    }

    public NettyMQTTHandler(ProtocolProcessor processor) {
        m_processor = processor;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        AbstractMessage msg = (AbstractMessage) message;
        LOG.info("Received a message of type {}", Utils.msgType2String(msg.getMessageType()));
        try {
            switch (msg.getMessageType()) {
                case CONNECT:
                    m_processor.processConnect(new NettyChannel(ctx), (ConnectMessage) msg);
                    break;
                case SUBSCRIBE:
                    m_processor.processSubscribe(new NettyChannel(ctx), (SubscribeMessage) msg);
                    break;
                case UNSUBSCRIBE:
                    m_processor.processUnsubscribe(new NettyChannel(ctx), (UnsubscribeMessage) msg);
                    break;
                case PUBLISH:
                    m_processor.processPublish(new NettyChannel(ctx), (PublishMessage) msg);
                    break;
                case PUBREC:
                    m_processor.processPubRec(new NettyChannel(ctx), (PubRecMessage) msg);
                    break;
                case PUBCOMP:
                    m_processor.processPubComp(new NettyChannel(ctx), (PubCompMessage) msg);
                    break;
                case PUBREL:
                    m_processor.processPubRel(new NettyChannel(ctx), (PubRelMessage) msg);
                    break;
                case DISCONNECT:
                    m_processor.processDisconnect(new NettyChannel(ctx), (DisconnectMessage) msg);
                    break;
                case PUBACK:
                    m_processor.processPubAck(new NettyChannel(ctx), (PubAckMessage) msg);
                    break;
                case PINGREQ:
                    PingRespMessage pingResp = new PingRespMessage();
                    ctx.writeAndFlush(pingResp);
                    break;
            }
        } catch (Exception ex) {
            LOG.error("Bad error in processing the message", ex);
        }
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String clientID = (String) NettyUtils.getAttribute(ctx, NettyChannel.ATTR_KEY_CLIENTID);
        if (clientID != null && !clientID.isEmpty()) {
            //if the channel was of a correctly connected client, inform messagin
            //else it was of a not completed CONNECT message
            m_processor.processConnectionLost(new LostConnectionEvent(clientID));
        }
        ctx.close(/*false*/);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (cause instanceof CorruptedFrameException) {
            //something goes bad with decoding
            LOG.warn("Error decoding a packet, probably a bad formatted packet, message: " + cause.getMessage());
        } else {
            LOG.error("Ugly error on networking", cause);
        }
        ctx.close();
    }
}
