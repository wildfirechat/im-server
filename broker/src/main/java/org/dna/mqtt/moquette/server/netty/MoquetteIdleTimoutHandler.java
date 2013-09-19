package org.dna.mqtt.moquette.server.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;

class MoquetteIdleTimoutHandler extends ChannelDuplexHandler {
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleState) {
            IdleState e = (IdleState) evt;
            if (e == IdleState.ALL_IDLE) {
                ctx.close();
            } /*else if (e.getState() == IdleState.WRITER_IDLE) {
                    ctx.writeAndFlush(new PingMessage());
                }*/
        }
    }
}
