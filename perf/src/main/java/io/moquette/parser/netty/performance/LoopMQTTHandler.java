package io.moquette.parser.netty.performance;

import io.moquette.parser.proto.messages.*;
import static io.moquette.parser.proto.messages.AbstractMessage.*;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.moquette.parser.proto.Utils;
import io.netty.util.AttributeKey;
import org.HdrHistogram.Histogram;
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
    Histogram processingTime = new Histogram(5);
    Histogram forthNetworkTime = new Histogram(5);

    public LoopMQTTHandler(ProtocolDecodingServer.SharedState state) {
        this.m_state = state;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        AbstractMessage msg = (AbstractMessage) message;
        String clientID = clientID(ctx.channel());

        try {
            switch (msg.getMessageType()) {
                case CONNECT:
                    ConnectMessage connect = (ConnectMessage) message;
                    clientID = connect.getClientID();
                    LOG.info("Received a message of type {} from <{}>", Utils.msgType2String(msg.getMessageType()), clientID);
                    handleConnect(ctx, connect);
                    return;
                case SUBSCRIBE:
                    LOG.info("Received a message of type {} from <{}>", Utils.msgType2String(msg.getMessageType()), clientID);
                    handleSubscribe(ctx, (SubscribeMessage) msg);
                    return;
                case PUBLISH:
                    LOG.info("Received a message of type {} from <{}>", Utils.msgType2String(msg.getMessageType()), clientID);
                    handlePublish(ctx, (PublishMessage) msg);
                    return;
                case DISCONNECT:
                    LOG.info("Received a message of type {} from <{}>", Utils.msgType2String(msg.getMessageType()), clientID);
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
                default:
                    LOG.info("Received a message of type {} from <{}>", Utils.msgType2String(msg.getMessageType()), clientID);
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

        long start = System.nanoTime();
        LOG.debug("push forward message the topic {}", msg.getTopicName());
        LOG.debug("content <{}>", payload2Str(msg.getPayload()));
        String decodedPayload = payload2Str(msg.getPayload());
        long sentTime = Long.parseLong(decodedPayload.split("-")[1]);
        forthNetworkTime.recordValue(start - sentTime);

        //publish always at Qos0, to don't handle PUBACK or the complete Qos2 workflow
        PublishMessage pubMessage = new PublishMessage();
        pubMessage.setRetainFlag(false);
        pubMessage.setTopicName(msg.getTopicName());
        pubMessage.setQos(QOSType.MOST_ONE);
        pubMessage.setPayload(msg.getPayload());

        m_state.getSubscriberCh().writeAndFlush(pubMessage);
        /*Channel subscriberCh = m_state.getSubscriberCh();
        if (subscriberCh.isWritable()) {
            subscriberCh.write(pubMessage);
        } else {
            subscriberCh.writeAndFlush(pubMessage);
        }*/
        long stop = System.nanoTime();
        processingTime.recordValue(stop - start);
        LOG.info("Request processed in {} ns, matching {}", stop - start, payload2Str(msg.getPayload()));
    }

    private void handleSubscribe(ChannelHandlerContext ctx, SubscribeMessage msg) {
        m_state.setForwardable(true);
        LOG.debug(" new value of flag {}, LoopHandler instance is {}", m_state.isForwardable(), this);
        SubAckMessage ackMessage = new SubAckMessage();
        ackMessage.setMessageID(msg.getMessageID());
        ctx.writeAndFlush(ackMessage);
        LOG.debug("subscribed client to {}", msg.subscriptions());
    }

    static String payload2Str(ByteBuffer content) {
        byte[] b = new byte[content.remaining()];
        content.mark();
        content.get(b);
        content.reset();
        return new String(b);
    }

    private void handleConnect(ChannelHandlerContext ctx, ConnectMessage msg) {
        String clientID = msg.getClientID();
        clientID(ctx.channel(), clientID);
        if (clientID.toLowerCase().startsWith("sub")) {
            m_state.setSubscriberCh(ctx.channel());
        } else if (clientID.toLowerCase().startsWith("pub")) {
            m_state.setPublisherCh(ctx.channel());
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
        LOG.info("Received channel inactive");
//        NettyChannel channel = m_channelMapper.get(ctx);
//        String clientID = (String) channel.getAttribute(NettyChannel.ATTR_KEY_CLIENTID);
//        m_messaging.lostConnection(channel, clientID);
        ctx.channel().close(/*false*/);
        System.out.println("Processing time histogram (microsecs)");
        this.processingTime.outputPercentileDistribution(System.out, 1000.0);

        System.out.println("Network time histogram (microsecs)");
        this.forthNetworkTime.outputPercentileDistribution(System.out, 1000.0);

//        synchronized (m_channelMapper) {
//            m_channelMapper.remove(ctx);
//        }
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isWritable()) {
            String clientID = clientID(ctx.channel());
            LOG.info("Channel for client <{}> is writable again", clientID);
            ctx.channel().flush();
        }
        ctx.fireChannelWritabilityChanged();
    }

    private static final AttributeKey<Object> ATTR_KEY_CLIENTID = AttributeKey.valueOf("ClientID");

    private static void clientID(Channel channel, String clientID) {
        channel.attr(ATTR_KEY_CLIENTID).set(clientID);
    }

    private static String clientID(Channel channel) {
        return (String) channel.attr(ATTR_KEY_CLIENTID).get();
    }
}