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

import io.moquette.server.netty.NettyMQTTHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
class ClientNettyMQTTHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(NettyMQTTHandler.class);
    private Client m_client;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        MqttMessage msg = (MqttMessage) message;
        LOG.info("Received a message of type {}", msg.fixedHeader().messageType());
        m_client.messageReceived(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        m_client.setConnectionLost(true);
        ctx.close(/* false */);
    }

    void setClient(Client client) {
        m_client = client;
    }

}
