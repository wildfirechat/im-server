package io.moquette.parser.netty.performance;

import io.moquette.commons.Constants;
import io.moquette.parser.netty.MQTTDecoder;
import io.moquette.parser.netty.MQTTEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Emulates a broker, but doesn't apply any protocol logic, just forward the qos0 publishes
 * to the lone subscriber, it's used just to measure the protocol decoding/encoding overhead.
 *
 */
public class ProtocolDecodingServer {

    class MoquetteIdleTimeoutHandler extends ChannelDuplexHandler {
        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleState e = ((IdleStateEvent) evt).state();
                if (e == IdleState.ALL_IDLE) {
                    //fire a channelInactive to trigger publish of Will
                    ctx.fireChannelInactive();
                    ctx.close();
                } /*else if (e.getState() == IdleState.WRITER_IDLE) {
                    ctx.writeAndFlush(new PingMessage());
                }*/
            }
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ProtocolDecodingServer.class);

    EventLoopGroup m_bossGroup;
    EventLoopGroup m_workerGroup;

    static final class SharedState {
        private volatile ChannelHandlerContext subscriberCh;
        private volatile ChannelHandlerContext publisherCh;
        private volatile boolean forwardPublish = false;

        public boolean isForwardable() {
            return forwardPublish;
        }

        void setForwardable(boolean forwardPublish) {
            this.forwardPublish = forwardPublish;
        }

        public void setSubscriberCh(ChannelHandlerContext subscriberCh) {
            this.subscriberCh = subscriberCh;
        }

        public ChannelHandlerContext getSubscriberCh() {
            return subscriberCh;
        }

        public void setPublisherCh(ChannelHandlerContext publisherCh) {
            this.publisherCh = publisherCh;
        }
    }

    final SharedState state = new SharedState();

    void init() {
        String host = "0.0.0.0";
        int port = 1883;
        m_bossGroup = new NioEventLoopGroup();
        m_workerGroup = new NioEventLoopGroup();

        ServerBootstrap b = new ServerBootstrap();
        b.group(m_bossGroup, m_workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        try {
                            pipeline.addFirst("idleStateHandler", new IdleStateHandler(0, 0, Constants.DEFAULT_CONNECT_TIMEOUT));
                            pipeline.addAfter("idleStateHandler", "idleEventHandler", new MoquetteIdleTimeoutHandler());
                            pipeline.addLast("decoder", new MQTTDecoder());
                            pipeline.addLast("encoder", new MQTTEncoder());
                            pipeline.addLast("handler", new LoopMQTTHandler(state));
                        } catch (Throwable th) {
                            LOG.error("Severe error during pipeline creation", th);
                            throw th;
                        }
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        try {
            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(host, port);
            LOG.info("Server binded host: {}, port: {}", host, port);
            f.sync();
        } catch (InterruptedException ex) {
            LOG.error(null, ex);
        }
    }


    public void stop() {
        if (m_workerGroup == null) {
            throw new IllegalStateException("Invoked close on an Acceptor that wasn't initialized");
        }
        if (m_bossGroup == null) {
            throw new IllegalStateException("Invoked close on an Acceptor that wasn't initialized");
        }
        m_workerGroup.shutdownGracefully();
        m_bossGroup.shutdownGracefully();

        LOG.info("Closed boss and worker Event loops");
        System.out.println("Server stopped");
    }

    public static final void main(String[] args) {
        final ProtocolDecodingServer server = new ProtocolDecodingServer();
        server.init();

        System.out.println("Loop server started");
        //Bind  a shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.stop();
            }
        });
    }
}
