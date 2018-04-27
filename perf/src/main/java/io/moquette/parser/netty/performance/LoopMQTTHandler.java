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

package io.moquette.parser.netty.performance;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.*;
import io.netty.util.AttributeKey;
import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.moquette.parser.netty.performance.NettyPublishReceiverHandler.payload2Str;
import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;
import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_LEAST_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;

@ChannelHandler.Sharable
class LoopMQTTHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(LoopMQTTHandler.class);
    private ProtocolDecodingServer.SharedState m_state;
    Histogram processingTime = new Histogram(5);
    Histogram forthNetworkTime = new Histogram(5);

    LoopMQTTHandler(ProtocolDecodingServer.SharedState state) {
        this.m_state = state;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        MqttMessage msg = (MqttMessage) message;
        MqttMessageType messageType = msg.fixedHeader().messageType();
        String clientID = clientID(ctx.channel());

        try {
            switch (messageType) {
                case CONNECT:
                    MqttConnectMessage connect = (MqttConnectMessage) message;
                    handleConnect(ctx, connect);
                    return;
                case SUBSCRIBE:
                    LOG.info("Received a message of type {} from <{}>", messageType, clientID);
                    handleSubscribe(ctx, (MqttSubscribeMessage) msg);
                    return;
                case PUBLISH:
                    LOG.info("Received a message of type {} from <{}>", messageType, clientID);
                    handlePublish(ctx, (MqttPublishMessage) msg);
                    return;
                case DISCONNECT:
                    LOG.info("Received a message of type {} from <{}>", messageType, clientID);
                    ctx.close().addListener(CLOSE_ON_FAILURE);
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
                    MqttFixedHeader pingHeader = new MqttFixedHeader(
                            MqttMessageType.PINGRESP,
                            false,
                            AT_MOST_ONCE,
                            false,
                            0);
                    MqttMessage pingResp = new MqttMessage(pingHeader);
                    ctx.writeAndFlush(pingResp).addListener(CLOSE_ON_FAILURE);
                    break;
                default:
                    LOG.info("Received a message of type {} from <{}>", messageType, clientID);
            }
        } catch (Exception ex) {
            LOG.error("Bad error in processing the message", ex);
        }
    }

    private void handlePublish(ChannelHandlerContext ctx, MqttPublishMessage msg) {
        if (!m_state.isForwardable()) {
            LOG.info("Subscriber not yet connected, LoopHandler instance is {}", this);
            return;
        }

        long start = System.nanoTime();
        LOG.debug("push forward message the topic {}", msg.variableHeader().topicName());
        LOG.debug("content <{}>", payload2Str(msg.payload()));
        String decodedPayload = payload2Str(msg.payload());
        long sentTime = Long.parseLong(decodedPayload.split("-")[1]);
        forthNetworkTime.recordValue(start - sentTime);

        //publish always at Qos0, to don't handle PUBACK or the complete Qos2 workflow
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE,
                false, 0);
        MqttPublishVariableHeader varHeader = new MqttPublishVariableHeader(msg.variableHeader().topicName(), 0);
        MqttPublishMessage pubMessage = new MqttPublishMessage(fixedHeader, varHeader, msg.payload());

        m_state.getSubscriberCh().writeAndFlush(pubMessage).addListener(CLOSE_ON_FAILURE);
        /*Channel subscriberCh = m_state.getSubscriberCh();
        if (subscriberCh.isWritable()) {
            subscriberCh.write(pubMessage);
        } else {
            subscriberCh.writeAndFlush(pubMessage);
        }*/
        long stop = System.nanoTime();
        processingTime.recordValue(stop - start);
        LOG.info("Request processed in {} ns, matching {}", stop - start, payload2Str(msg.payload()));
    }

    private void handleSubscribe(ChannelHandlerContext ctx, MqttSubscribeMessage msg) {
        m_state.setForwardable(true);
        LOG.debug(" new value of flag {}, LoopHandler instance is {}", m_state.isForwardable(), this);
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.SUBACK, false, AT_LEAST_ONCE, false, 0);
        MqttSubAckPayload payload = new MqttSubAckPayload(AT_MOST_ONCE.value());
        MqttSubAckMessage ackMessage = new MqttSubAckMessage(
                fixedHeader,
                from(msg.variableHeader().messageId()),
                payload);
        ctx.writeAndFlush(ackMessage).addListener(CLOSE_ON_FAILURE);
        LOG.debug("subscribed client to {}", msg.payload().topicSubscriptions());
    }

    private void handleConnect(ChannelHandlerContext ctx, MqttConnectMessage msg) {
        MqttConnectPayload payload = msg.payload();
        String clientID = payload.clientIdentifier();
        LOG.info("Received a message of type {} from <{}>", MqttMessageType.CONNECT, clientID);

        clientID(ctx.channel(), clientID);
        if (clientID.toLowerCase().startsWith("sub")) {
            m_state.setSubscriberCh(ctx.channel());
        } else if (clientID.toLowerCase().startsWith("pub")) {
            m_state.setPublisherCh(ctx.channel());
        } else {
            //we don't admit other names
            MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE,
                    false, 0);
            MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(
                    MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false);
            MqttConnAckMessage koResp = new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
            ctx.writeAndFlush(koResp).addListener(CLOSE_ON_FAILURE);
            ctx.close().addListener(CLOSE_ON_FAILURE);
            return;
        }

        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE,
                false, 0);
        MqttConnAckVariableHeader mqttConnAckVariableHeader = new MqttConnAckVariableHeader(
                MqttConnectReturnCode.CONNECTION_ACCEPTED, false);
        MqttConnAckMessage okResp = new MqttConnAckMessage(mqttFixedHeader, mqttConnAckVariableHeader);
        ctx.writeAndFlush(okResp).addListener(CLOSE_ON_FAILURE);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOG.info("Received channel inactive");
//        NettyChannel channel = m_channelMapper.get(ctx);
//        String clientID = (String) channel.getAttribute(NettyChannel.ATTR_KEY_CLIENTID);
//        m_messaging.lostConnection(channel, clientID);
        ctx.channel().close().addListener(CLOSE_ON_FAILURE);
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
