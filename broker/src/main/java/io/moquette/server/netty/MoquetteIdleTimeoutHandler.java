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

package io.moquette.server.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

@Sharable
class MoquetteIdleTimeoutHandler extends ChannelDuplexHandler {

    private static final Logger LOG = LoggerFactory.getLogger(MoquetteIdleTimeoutHandler.class);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState e = ((IdleStateEvent) evt).state();
            if (e == IdleState.READER_IDLE) {
                LOG.info("Firing channel inactive event. MqttClientId = {}.", NettyUtils.clientID(ctx.channel()));
                // fire a channelInactive to trigger publish of Will
                ctx.fireChannelInactive();
                ctx.close();
            }
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug(
                        "Firing Netty event. MqttClientId = {}, eventClass = {}.",
                        NettyUtils.clientID(ctx.channel()),
                        evt.getClass().getName());
            }
            super.userEventTriggered(ctx, evt);
        }
    }
}
