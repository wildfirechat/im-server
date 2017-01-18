package io.moquette.parser.netty.performance;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@ChannelHandler.Sharable
class NettyPublishReceiverHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(NettyPublishReceiverHandler.class);
    Histogram forthNetworkTime = new Histogram(5);

    public NettyPublishReceiverHandler() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        MqttMessage msg = (MqttMessage) message;
        MqttMessageType type = msg.fixedHeader().messageType();

        try {
            switch (type) {
                case PUBLISH:
                    LOG.info("Received a message of type {}", type);
                    handlePublish((MqttPublishMessage) msg);
                    return;
                default:
                    LOG.info("Received a message of type {}",type);
            }
        } catch (Exception ex) {
            LOG.error("Bad error in processing the message", ex);
        }
    }

    private void handlePublish(MqttPublishMessage msg) {
        long start = System.nanoTime();
        LOG.debug("push forward message the topic {}", msg.variableHeader().topicName());
        LOG.debug("content <{}>", payload2Str(msg.content()));
        String decodedPayload = payload2Str(msg.content());
        long sentTime = Long.parseLong(decodedPayload.split("-")[1]);
        forthNetworkTime.recordValue(start - sentTime);

        long stop = System.nanoTime();
        LOG.info("Request processed in {} ns, matching {}", stop - start, decodedPayload);
    }

    static String payload2Str(ByteBuf content) {
        byte[] rawBytes;
        if (content.hasArray()) {
            rawBytes = content.array();
        } else {
            int size = content.readableBytes();
            rawBytes = new byte[size];
            content.getBytes(content.readerIndex(), rawBytes);
        }
        return new String(rawBytes);
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOG.info("Received channel inactive");
        ctx.channel().close();

        System.out.println("Network time histogram (microsecs)");
        this.forthNetworkTime.outputPercentileDistribution(System.out, 1000.0);
    }

}