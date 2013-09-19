package org.dna.mqtt.moquette.server.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import java.io.IOException;

import io.netty.handler.timeout.IdleStateHandler;
import org.dna.mqtt.commons.Constants;
import org.dna.mqtt.moquette.messaging.spi.IMessaging;
import org.dna.mqtt.moquette.parser.netty.MQTTDecoder;
import org.dna.mqtt.moquette.parser.netty.MQTTEncoder;
import org.dna.mqtt.moquette.server.ServerAcceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
public class NettyAcceptor implements ServerAcceptor {
    
    private static final Logger LOG = LoggerFactory.getLogger(NettyAcceptor.class);
    
    EventLoopGroup m_bossGroup;
    EventLoopGroup m_workerGroup;
    

    public void initialize(IMessaging messaging) throws IOException {
        m_bossGroup = new NioEventLoopGroup();
        m_workerGroup = new NioEventLoopGroup();
        
        final NettyMQTTHandler handler = new NettyMQTTHandler();
        handler.setMessaging(messaging);
        
        ServerBootstrap b = new ServerBootstrap(); 
            b.group(m_bossGroup, m_workerGroup)
             .channel(NioServerSocketChannel.class) 
             .childHandler(new ChannelInitializer<SocketChannel>() { 
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addFirst("idleStateHandler", new IdleStateHandler(0, 0, Constants.DEFAULT_CONNECT_TIMEOUT));
                    pipeline.addAfter("idleStateHandler", "idleEventHandler", new MoquetteIdleTimoutHandler());
                    //pipeline.addLast("logger", new LoggingHandler("Netty", LogLevel.ERROR));
                    pipeline.addLast("decoder", new MQTTDecoder());
                    pipeline.addLast("encoder", new MQTTEncoder());
                    pipeline.addLast("handler", handler);
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)
             .option(ChannelOption.SO_REUSEADDR, true)
             .childOption(ChannelOption.SO_KEEPALIVE, true); 
        try {    
            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(Constants.PORT);
            LOG.info("Server binded");
            f.sync();
        } catch (InterruptedException ex) {
            LOG.error(null, ex);
        }
    }

    public void close() {
        if (m_workerGroup == null) {
            throw new IllegalStateException("Invoked close on an Acceptor that wasn't initialized");
        }
        if (m_bossGroup == null) {
            throw new IllegalStateException("Invoked close on an Acceptor that wasn't initialized");
        }
        m_workerGroup.shutdownGracefully();
        m_bossGroup.shutdownGracefully();
    }
    
}
