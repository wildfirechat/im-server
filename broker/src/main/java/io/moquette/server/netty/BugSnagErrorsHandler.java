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

import com.bugsnag.Bugsnag;
import io.moquette.server.config.IConfig;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import static io.moquette.BrokerConstants.BUGSNAG_TOKEN_PROPERTY_NAME;

@Sharable
public class BugSnagErrorsHandler extends ChannelInboundHandlerAdapter {

    private Bugsnag bugsnag;

    public void init(IConfig props) {
        final String token = props.getProperty(BUGSNAG_TOKEN_PROPERTY_NAME);
        this.bugsnag = new Bugsnag(token);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        bugsnag.notify(cause);
        ctx.fireExceptionCaught(cause);
    }
}
