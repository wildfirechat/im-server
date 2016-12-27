package io.moquette.parser.netty.performance;


import io.moquette.parser.netty.MQTTDecoder;
import io.moquette.parser.netty.MQTTEncoder;
import io.moquette.parser.proto.messages.AbstractMessage;
import io.moquette.parser.proto.messages.PublishMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.eclipse.jetty.toolchain.perf.PlatformTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

class PublishBomber {

    private static final Logger LOG = LoggerFactory.getLogger(PublishBomber.class);

    private final EventLoopGroup workerGroup;
    private Channel channel;

    PublishBomber(String host, int port) {
        workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("decoder", new MQTTDecoder());
                    pipeline.addLast("encoder", new MQTTEncoder());
                }
            });

            // Start the client.
            channel = b.connect(host, port).sync().channel();
        } catch (Exception ex) {
            LOG.error("Error received in client setup", ex);
            workerGroup.shutdownGracefully();
        }
    }

    private void sendMessage(AbstractMessage msg) {
        try {
            channel.writeAndFlush(msg).await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void publishLoop(int messagesPerSecond, int numToSend) {
        long pauseMicroseconds = (int)((1.0 / messagesPerSecond) * 1000 * 1000);
        LOG.warn("PUB: Pause over the each message sent {} microsecs", pauseMicroseconds);

        LOG.info("PUB: publishing..");
        final long startTime = System.currentTimeMillis();

        //initialize the timer
        PlatformTimer timer = PlatformTimer.detect();
        for (int i=0; i < numToSend; i++) {
            long nanos = System.nanoTime();

            PublishMessage pubMessage = new PublishMessage();
            pubMessage.setQos(AbstractMessage.QOSType.MOST_ONE);
            pubMessage.setTopicName("/topic");
            byte[] rawContent = ("Hello world!!-" + nanos).getBytes();
            ByteBuffer payload = (ByteBuffer) ByteBuffer.allocate(rawContent.length).put(rawContent).flip();
            pubMessage.setPayload(payload);
            sendMessage(pubMessage);
            timer.sleep(pauseMicroseconds);
        }
        LOG.info("PUB: published in {} ms", System.currentTimeMillis() - startTime);
    }

    public void disconnect() {
        try {
            this.channel.disconnect().await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
