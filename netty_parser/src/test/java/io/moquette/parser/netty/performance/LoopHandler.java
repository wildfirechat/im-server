package io.moquette.parser.netty.performance;

import io.moquette.proto.messages.*;
import static io.moquette.proto.messages.AbstractMessage.*;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.moquette.proto.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Created by andrea on 6/2/15.
 */
@ChannelHandler.Sharable
class LoopMQTTHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(LoopMQTTHandler.class);
    private ProtocolDecodingServer.SharedState m_state;

    public LoopMQTTHandler(ProtocolDecodingServer.SharedState state) {
        this.m_state = state;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        AbstractMessage msg = (AbstractMessage) message;
        LOG.info("Received a message of type {}", Utils.msgType2String(msg.getMessageType()));
        try {
            switch (msg.getMessageType()) {
                case CONNECT:
                    handleConnect(ctx, (ConnectMessage) message);
                    return;
                case SUBSCRIBE:
                    handleSubscribe(ctx, (SubscribeMessage) msg);
                    return;
                case PUBLISH:
                    handlePublish(ctx, (PublishMessage) msg);
                    return;
                case DISCONNECT:
                    ctx.close();
//                case PUBACK:
//                    NettyChannel channel;
//                    synchronized (m_channelMapper) {
//                        if (!m_channelMapper.containsKey(ctx)) {
//                            m_channelMapper.put(ctx, new NettyChannel(ctx));
//                        }
//                        channel = m_channelMapper.get(ctx);
//                    }
//
//                    m_messaging.handleProtocolMessage(channel, msg);
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

    private void handlePublish(ChannelHandlerContext ctx, PublishMessage msg) {
        if (!m_state.isForwardable()) {
            LOG.info("Subscriber not yet connected, LoopHandler instance is {}", this);
            return;
        }

        LOG.debug("push forward message the topic {}", msg.getTopicName());
        if (LOG.isDebugEnabled()) {
            LOG.debug("content <{}>", payload2Str(msg.getPayload()));
        }
        //publish always at Qos0, to don't handle PUBACK or the complete Qos2 workflow
        PublishMessage pubMessage = new PublishMessage();
        pubMessage.setRetainFlag(false);
        pubMessage.setTopicName(msg.getTopicName());
        pubMessage.setQos(QOSType.MOST_ONE);
        pubMessage.setPayload(msg.getPayload());

        m_state.getSubscriberCh().writeAndFlush(pubMessage);
    }

    private void handleSubscribe(ChannelHandlerContext ctx, SubscribeMessage msg) {
        m_state.setForwardable(true);
        LOG.debug(" new value of flag {}, LoopHandler instance is {}", m_state.isForwardable(), this);
        SubAckMessage ackMessage = new SubAckMessage();
        ackMessage.setMessageID(msg.getMessageID());
        ctx.writeAndFlush(ackMessage);
        LOG.debug("subscribed client to {}", msg.subscriptions());
    }

    static String  payload2Str(ByteBuffer content) {
        byte[] b = new byte[content.remaining()];
        content.mark();
        content.get(b);
        content.reset();
        return new String(b);
    }

    private void handleConnect(ChannelHandlerContext ctx, ConnectMessage msg) {
        String clientID = msg.getClientID();
        if (clientID.toLowerCase().startsWith("sub")) {
            m_state.setSubscriberCh(ctx);
        } else if (clientID.toLowerCase().startsWith("pub")) {
            m_state.setPublisherCh(ctx);
        } else {
            //we don't admit other names
            ConnAckMessage koResp = new ConnAckMessage();
            koResp.setReturnCode(ConnAckMessage.IDENTIFIER_REJECTED);
            ctx.writeAndFlush(koResp);
            ctx.close();
        }

        ConnAckMessage okResp = new ConnAckMessage();
        okResp.setReturnCode(ConnAckMessage.CONNECTION_ACCEPTED);
        ctx.writeAndFlush(okResp);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
//        NettyChannel channel = m_channelMapper.get(ctx);
//        String clientID = (String) channel.getAttribute(NettyChannel.ATTR_KEY_CLIENTID);
//        m_messaging.lostConnection(channel, clientID);
//        ctx.close(/*false*/);
//        synchronized (m_channelMapper) {
//            m_channelMapper.remove(ctx);
//        }
    }
}