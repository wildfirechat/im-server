package io.moquette.parser.netty.performance;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.moquette.parser.netty.performance.NettyPublishReceiverHandler.payload2Str;

@ChannelHandler.Sharable
class PublishReceiverHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(PublishReceiverHandler.class);
    Histogram forthNetworkTime = new Histogram(5);

    public PublishReceiverHandler() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        MqttMessage msg = (MqttMessage) message;
        MqttMessageType messageType = msg.fixedHeader().messageType();

        try {
            switch (messageType) {
                case PUBLISH:
                    LOG.info("Received a message of type {}", messageType);
                    handlePublish((MqttPublishMessage) msg);
                    return;
                default:
                    LOG.info("Received a message of type {}", messageType);
            }
        } catch (Exception ex) {
            LOG.error("Bad error in processing the message", ex);
        }
    }

    private void handlePublish(MqttPublishMessage msg) {
        long start = System.nanoTime();
        LOG.debug("push forward message the topic {}", msg.variableHeader().topicName());
        LOG.debug("content <{}>", payload2Str(msg.payload()));
        String decodedPayload = payload2Str(msg.payload());
        long sentTime = Long.parseLong(decodedPayload.split("-")[1]);
        forthNetworkTime.recordValue(start - sentTime);

        long stop = System.nanoTime();
        LOG.info("Request processed in {} ns, matching {}", stop - start, payload2Str(msg.payload()));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOG.info("Received channel inactive");
        ctx.channel().close();

        System.out.println("Network time histogram (microsecs)");
        this.forthNetworkTime.outputPercentileDistribution(System.out, 1000.0);
    }

}