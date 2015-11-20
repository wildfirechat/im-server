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
package io.moquette.server.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

/**
 * Some Netty's channels utilities.
 *
 * @author andrea
 */
public class NettyUtils {

    public static Object getAttribute(ChannelHandlerContext channel, AttributeKey<Object> key) {
        Attribute<Object> attr = channel.attr(key);
        return attr.get();
    }

    public static void setAttribute(ChannelHandlerContext channel, AttributeKey<Object> key, Object value) {
        Attribute<Object> attr = channel.attr(key);
        attr.set(value);
    }

    public static void setIdleTime(ChannelHandlerContext channel, int idleTime) {
        if (channel.pipeline().names().contains("idleStateHandler")) {
            channel.pipeline().remove("idleStateHandler");
        }
        if (channel.pipeline().names().contains("idleEventHandler")) {
            channel.pipeline().remove("idleEventHandler");
        }
        channel.pipeline().addFirst("idleStateHandler", new IdleStateHandler(0, 0, idleTime));
        channel.pipeline().addAfter("idleStateHandler", "idleEventHandler", new MoquetteIdleTimoutHandler());
    }
}
