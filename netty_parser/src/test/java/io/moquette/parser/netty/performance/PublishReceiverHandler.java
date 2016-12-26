package io.moquette.parser.netty.performance;

import io.moquette.parser.proto.Utils;
import io.moquette.parser.proto.messages.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.AttributeKey;
import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static io.moquette.parser.proto.messages.AbstractMessage.*;

@ChannelHandler.Sharable
class PublishReceiverHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(PublishReceiverHandler.class);
    Histogram forthNetworkTime = new Histogram(5);

    public PublishReceiverHandler() {
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object message) {
        AbstractMessage msg = (AbstractMessage) message;

        try {
            switch (msg.getMessageType()) {
                case PUBLISH:
                    LOG.info("Received a message of type {}", Utils.msgType2String(msg.getMessageType()));
                    handlePublish((PublishMessage) msg);
                    return;
                default:
                    LOG.info("Received a message of type {}", Utils.msgType2String(msg.getMessageType()));
            }
        } catch (Exception ex) {
            LOG.error("Bad error in processing the message", ex);
        }
    }

    private void handlePublish(PublishMessage msg) {
        long start = System.nanoTime();
        LOG.debug("push forward message the topic {}", msg.getTopicName());
        if (LOG.isDebugEnabled()) {
            LOG.debug("content <{}>", payload2Str(msg.getPayload()));
        }
        String decodedPayload = payload2Str(msg.getPayload());
        long sentTime = Long.parseLong(decodedPayload.split("-")[1]);
        forthNetworkTime.recordValue(start - sentTime);

        long stop = System.nanoTime();
        LOG.info("Request processed in {} ns, matching {}", stop - start, payload2Str(msg.getPayload()));
    }

    static String payload2Str(ByteBuffer content) {
        byte[] b = new byte[content.remaining()];
        content.mark();
        content.get(b);
        content.reset();
        return new String(b);
    }


    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOG.info("Received channel inactive");
        ctx.channel().close();

        System.out.println("Network time histogram (microsecs)");
        this.forthNetworkTime.outputPercentileDistribution(System.out, 1000.0);
    }

}