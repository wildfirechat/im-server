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

package io.moquette.broker;

import io.moquette.BrokerConstants;
import io.moquette.server.config.IConfig;
import io.moquette.server.netty.BugSnagErrorsHandler;
import io.moquette.server.netty.MoquetteIdleTimeoutHandler;
import io.moquette.server.netty.metrics.*;
import io.moquette.spi.security.ISslContextCreator;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.moquette.BrokerConstants.*;
import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;

public class NewNettyAcceptor {

    private static final String MQTT_SUBPROTOCOL_CSV_LIST = "mqtt, mqttv3.1, mqttv3.1.1";

    static class WebSocketFrameToByteBufDecoder extends MessageToMessageDecoder<BinaryWebSocketFrame> {

        @Override
        protected void decode(ChannelHandlerContext chc, BinaryWebSocketFrame frame, List<Object> out)
                throws Exception {
            // convert the frame to a ByteBuf
            ByteBuf bb = frame.content();
            // System.out.println("WebSocketFrameToByteBufDecoder decode - " +
            // ByteBufUtil.hexDump(bb));
            bb.retain();
            out.add(bb);
        }
    }

    static class ByteBufToWebSocketFrameEncoder extends MessageToMessageEncoder<ByteBuf> {

        @Override
        protected void encode(ChannelHandlerContext chc, ByteBuf bb, List<Object> out) throws Exception {
            // convert the ByteBuf to a WebSocketFrame
            BinaryWebSocketFrame result = new BinaryWebSocketFrame();
            // System.out.println("ByteBufToWebSocketFrameEncoder encode - " +
            // ByteBufUtil.hexDump(bb));
            result.content().writeBytes(bb);
            out.add(result);
        }
    }

    private abstract static class PipelineInitializer {

        abstract void init(SocketChannel channel) throws Exception;
    }

    private static final Logger LOG = LoggerFactory.getLogger(NewNettyAcceptor.class);

    EventLoopGroup m_bossGroup;
    EventLoopGroup m_workerGroup;
    BytesMetricsCollector m_bytesMetricsCollector = new BytesMetricsCollector();
    MessageMetricsCollector m_metricsCollector = new MessageMetricsCollector();
    private Optional<? extends ChannelInboundHandler> metrics;
    private Optional<? extends ChannelInboundHandler> errorsCather;

    private int nettySoBacklog;
    private boolean nettySoReuseaddr;
    private boolean nettyTcpNodelay;
    private boolean nettySoKeepalive;
    private int nettyChannelTimeoutSeconds;
    private int maxBytesInMessage;

    private Class<? extends ServerSocketChannel> channelClass;

    public void initialize(NewNettyMQTTHandler mqttHandler, IConfig props, ISslContextCreator sslCtxCreator) {
        LOG.debug("Initializing Netty acceptor");

        nettySoBacklog = props.intProp(BrokerConstants.NETTY_SO_BACKLOG_PROPERTY_NAME, 128);
        nettySoReuseaddr = props.boolProp(BrokerConstants.NETTY_SO_REUSEADDR_PROPERTY_NAME, true);
        nettyTcpNodelay = props.boolProp(BrokerConstants.NETTY_TCP_NODELAY_PROPERTY_NAME, true);
        nettySoKeepalive = props.boolProp(BrokerConstants.NETTY_SO_KEEPALIVE_PROPERTY_NAME, true);
        nettyChannelTimeoutSeconds = props.intProp(BrokerConstants.NETTY_CHANNEL_TIMEOUT_SECONDS_PROPERTY_NAME, 10);
        maxBytesInMessage = props.intProp(BrokerConstants.NETTY_MAX_BYTES_PROPERTY_NAME,
                BrokerConstants.DEFAULT_NETTY_MAX_BYTES_IN_MESSAGE);

        boolean epoll = props.boolProp(BrokerConstants.NETTY_EPOLL_PROPERTY_NAME, false);
        if (epoll) {
            LOG.info("Netty is using Epoll");
            m_bossGroup = new EpollEventLoopGroup();
            m_workerGroup = new EpollEventLoopGroup();
            channelClass = EpollServerSocketChannel.class;
        } else {
            LOG.info("Netty is using NIO");
            m_bossGroup = new NioEventLoopGroup();
            m_workerGroup = new NioEventLoopGroup();
            channelClass = NioServerSocketChannel.class;
        }

        final boolean useFineMetrics = props.boolProp(METRICS_ENABLE_PROPERTY_NAME, false);
        if (useFineMetrics) {
            DropWizardMetricsHandler metricsHandler = new DropWizardMetricsHandler();
            metricsHandler.init(props);
            this.metrics = Optional.of(metricsHandler);
        } else {
            this.metrics = Optional.empty();
        }

        final boolean useBugSnag = props.boolProp(BUGSNAG_ENABLE_PROPERTY_NAME, false);
        if (useBugSnag) {
            BugSnagErrorsHandler bugSnagHandler = new BugSnagErrorsHandler();
            bugSnagHandler.init(props);
            this.errorsCather = Optional.of(bugSnagHandler);
        } else {
            this.errorsCather = Optional.empty();
        }
        initializePlainTCPTransport(mqttHandler, props);
//        initializeWebSocketTransport(mqttHandler, props);
//        String sslTcpPortProp = props.getProperty(BrokerConstants.SSL_PORT_PROPERTY_NAME);
//        String wssPortProp = props.getProperty(BrokerConstants.WSS_PORT_PROPERTY_NAME);
//        if (sslTcpPortProp != null || wssPortProp != null) {
//            SslContext sslContext = sslCtxCreator.initSSLContext();
//            if (sslContext == null) {
//                LOG.error("Can't initialize SSLHandler layer! Exiting, check your configuration of jks");
//                return;
//            }
//            initializeSSLTCPTransport(mqttHandler, props, sslContext);
//            initializeWSSTransport(mqttHandler, props, sslContext);
//        }
    }

    private void initFactory(String host, int port, String protocol, final PipelineInitializer pipeliner) {
        LOG.debug("Initializing server. Protocol={}", protocol);
        ServerBootstrap b = new ServerBootstrap();
        b.group(m_bossGroup, m_workerGroup).channel(channelClass)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        pipeliner.init(ch);
                    }
                })
                .option(ChannelOption.SO_BACKLOG, nettySoBacklog)
                .option(ChannelOption.SO_REUSEADDR, nettySoReuseaddr)
                .childOption(ChannelOption.TCP_NODELAY, nettyTcpNodelay)
                .childOption(ChannelOption.SO_KEEPALIVE, nettySoKeepalive);
        try {
            LOG.debug("Binding server. host={}, port={}", host, port);
            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(host, port);
            LOG.info("Server bound to host={}, port={}, protocol={}", host, port, protocol);
            f.sync().addListener(FIRE_EXCEPTION_ON_FAILURE);
        } catch (InterruptedException ex) {
            LOG.error("An interruptedException was caught while initializing server. Protocol={}", protocol, ex);
        }
    }

    private void initializePlainTCPTransport(NewNettyMQTTHandler handler, IConfig props) {
        LOG.debug("Configuring TCP MQTT transport");
        final MoquetteIdleTimeoutHandler timeoutHandler = new MoquetteIdleTimeoutHandler();
        String host = props.getProperty(BrokerConstants.HOST_PROPERTY_NAME);
        String tcpPortProp = props.getProperty(PORT_PROPERTY_NAME, DISABLED_PORT_BIND);
        if (DISABLED_PORT_BIND.equals(tcpPortProp)) {
            LOG.info("Property {} has been set to {}. TCP MQTT will be disabled", BrokerConstants.PORT_PROPERTY_NAME,
                    DISABLED_PORT_BIND);
            return;
        }
        int port = Integer.parseInt(tcpPortProp);
        initFactory(host, port, "TCP MQTT", new PipelineInitializer() {

            @Override
            void init(SocketChannel channel) {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addFirst("idleStateHandler", new IdleStateHandler(nettyChannelTimeoutSeconds, 0, 0));
                pipeline.addAfter("idleStateHandler", "idleEventHandler", timeoutHandler);
                // pipeline.addLast("logger", new LoggingHandler("Netty", LogLevel.ERROR));
                if (errorsCather.isPresent()) {
                    pipeline.addLast("bugsnagCatcher", errorsCather.get());
                }
                pipeline.addFirst("bytemetrics", new BytesMetricsHandler(m_bytesMetricsCollector));
                pipeline.addLast("decoder", new MqttDecoder(maxBytesInMessage));
                pipeline.addLast("encoder", MqttEncoder.INSTANCE);
                pipeline.addLast("metrics", new MessageMetricsHandler(m_metricsCollector));
                pipeline.addLast("messageLogger", new MQTTMessageLogger());
                if (metrics.isPresent()) {
                    pipeline.addLast("wizardMetrics", metrics.get());
                }
                pipeline.addLast("handler", handler);
            }
        });
    }

//    private void initializeWebSocketTransport(final NettyMQTTHandler handler, IConfig props) {
//        LOG.debug("Configuring Websocket MQTT transport");
//        String webSocketPortProp = props.getProperty(WEB_SOCKET_PORT_PROPERTY_NAME, DISABLED_PORT_BIND);
//        if (DISABLED_PORT_BIND.equals(webSocketPortProp)) {
//            // Do nothing no WebSocket configured
//            LOG.info("Property {} has been setted to {}. Websocket MQTT will be disabled",
//                    BrokerConstants.WEB_SOCKET_PORT_PROPERTY_NAME, DISABLED_PORT_BIND);
//            return;
//        }
//        int port = Integer.parseInt(webSocketPortProp);
//
//        final MoquetteIdleTimeoutHandler timeoutHandler = new MoquetteIdleTimeoutHandler();
//
//        String host = props.getProperty(BrokerConstants.HOST_PROPERTY_NAME);
//        initFactory(host, port, "Websocket MQTT", new PipelineInitializer() {
//
//            @Override
//            void init(SocketChannel channel) {
//                ChannelPipeline pipeline = channel.pipeline();
//                pipeline.addLast(new HttpServerCodec());
//                pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
//                pipeline.addLast("webSocketHandler",
//                        new WebSocketServerProtocolHandler("/mqtt", MQTT_SUBPROTOCOL_CSV_LIST));
//                pipeline.addLast("ws2bytebufDecoder", new WebSocketFrameToByteBufDecoder());
//                pipeline.addLast("bytebuf2wsEncoder", new ByteBufToWebSocketFrameEncoder());
//                pipeline.addFirst("idleStateHandler", new IdleStateHandler(nettyChannelTimeoutSeconds, 0, 0));
//                pipeline.addAfter("idleStateHandler", "idleEventHandler", timeoutHandler);
//                pipeline.addFirst("bytemetrics", new BytesMetricsHandler(m_bytesMetricsCollector));
//                pipeline.addLast("decoder", new MqttDecoder(maxBytesInMessage));
//                pipeline.addLast("encoder", MqttEncoder.INSTANCE);
//                pipeline.addLast("metrics", new MessageMetricsHandler(m_metricsCollector));
//                pipeline.addLast("messageLogger", new MQTTMessageLogger());
//                pipeline.addLast("handler", handler);
//            }
//        });
//    }
//
//    private void initializeSSLTCPTransport(NettyMQTTHandler handler, IConfig props, SslContext sslContext) {
//        LOG.debug("Configuring SSL MQTT transport");
//        String sslPortProp = props.getProperty(SSL_PORT_PROPERTY_NAME, DISABLED_PORT_BIND);
//        if (DISABLED_PORT_BIND.equals(sslPortProp)) {
//            // Do nothing no SSL configured
//            LOG.info("Property {} has been set to {}. SSL MQTT will be disabled",
//                    BrokerConstants.SSL_PORT_PROPERTY_NAME, DISABLED_PORT_BIND);
//            return;
//        }
//
//        int sslPort = Integer.parseInt(sslPortProp);
//        LOG.debug("Starting SSL on port {}", sslPort);
//
//        final MoquetteIdleTimeoutHandler timeoutHandler = new MoquetteIdleTimeoutHandler();
//        String host = props.getProperty(BrokerConstants.HOST_PROPERTY_NAME);
//        String sNeedsClientAuth = props.getProperty(BrokerConstants.NEED_CLIENT_AUTH, "false");
//        final boolean needsClientAuth = Boolean.valueOf(sNeedsClientAuth);
//        initFactory(host, sslPort, "SSL MQTT", new PipelineInitializer() {
//
//            @Override
//            void init(SocketChannel channel) throws Exception {
//                ChannelPipeline pipeline = channel.pipeline();
//                pipeline.addLast("ssl", createSslHandler(channel, sslContext, needsClientAuth));
//                pipeline.addFirst("idleStateHandler", new IdleStateHandler(nettyChannelTimeoutSeconds, 0, 0));
//                pipeline.addAfter("idleStateHandler", "idleEventHandler", timeoutHandler);
//                // pipeline.addLast("logger", new LoggingHandler("Netty", LogLevel.ERROR));
//                pipeline.addFirst("bytemetrics", new BytesMetricsHandler(m_bytesMetricsCollector));
//                pipeline.addLast("decoder", new MqttDecoder(maxBytesInMessage));
//                pipeline.addLast("encoder", MqttEncoder.INSTANCE);
//                pipeline.addLast("metrics", new MessageMetricsHandler(m_metricsCollector));
//                pipeline.addLast("messageLogger", new MQTTMessageLogger());
//                pipeline.addLast("handler", handler);
//            }
//        });
//    }
//
//    private void initializeWSSTransport(NettyMQTTHandler handler, IConfig props, SslContext sslContext) {
//        LOG.debug("Configuring secure websocket MQTT transport");
//        String sslPortProp = props.getProperty(WSS_PORT_PROPERTY_NAME, DISABLED_PORT_BIND);
//        if (DISABLED_PORT_BIND.equals(sslPortProp)) {
//            // Do nothing no SSL configured
//            LOG.info("Property {} has been set to {}. Secure websocket MQTT will be disabled",
//                    BrokerConstants.WSS_PORT_PROPERTY_NAME, DISABLED_PORT_BIND);
//            return;
//        }
//        int sslPort = Integer.parseInt(sslPortProp);
//        final MoquetteIdleTimeoutHandler timeoutHandler = new MoquetteIdleTimeoutHandler();
//        String host = props.getProperty(BrokerConstants.HOST_PROPERTY_NAME);
//        String sNeedsClientAuth = props.getProperty(BrokerConstants.NEED_CLIENT_AUTH, "false");
//        final boolean needsClientAuth = Boolean.valueOf(sNeedsClientAuth);
//        initFactory(host, sslPort, "Secure websocket", new PipelineInitializer() {
//
//            @Override
//            void init(SocketChannel channel) throws Exception {
//                ChannelPipeline pipeline = channel.pipeline();
//                pipeline.addLast("ssl", createSslHandler(channel, sslContext, needsClientAuth));
//                pipeline.addLast("httpEncoder", new HttpResponseEncoder());
//                pipeline.addLast("httpDecoder", new HttpRequestDecoder());
//                pipeline.addLast("aggregator", new HttpObjectAggregator(65536));
//                pipeline.addLast("webSocketHandler",
//                        new WebSocketServerProtocolHandler("/mqtt", MQTT_SUBPROTOCOL_CSV_LIST));
//                pipeline.addLast("ws2bytebufDecoder", new WebSocketFrameToByteBufDecoder());
//                pipeline.addLast("bytebuf2wsEncoder", new ByteBufToWebSocketFrameEncoder());
//                pipeline.addFirst("idleStateHandler", new IdleStateHandler(nettyChannelTimeoutSeconds, 0, 0));
//                pipeline.addAfter("idleStateHandler", "idleEventHandler", timeoutHandler);
//                pipeline.addFirst("bytemetrics", new BytesMetricsHandler(m_bytesMetricsCollector));
//                pipeline.addLast("decoder", new MqttDecoder(maxBytesInMessage));
//                pipeline.addLast("encoder", MqttEncoder.INSTANCE);
//                pipeline.addLast("metrics", new MessageMetricsHandler(m_metricsCollector));
//                pipeline.addLast("messageLogger", new MQTTMessageLogger());
//                pipeline.addLast("handler", handler);
//            }
//        });
//    }

    @SuppressWarnings("FutureReturnValueIgnored")
    public void close() {
        LOG.debug("Closing Netty acceptor...");
        if (m_workerGroup == null || m_bossGroup == null) {
            LOG.error("Netty acceptor is not initialized");
            throw new IllegalStateException("Invoked close on an Acceptor that wasn't initialized");
        }
        Future<?> workerWaiter = m_workerGroup.shutdownGracefully();
        Future<?> bossWaiter = m_bossGroup.shutdownGracefully();

        /*
         * We shouldn't raise an IllegalStateException if we are interrupted. If we did so, the
         * broker is not shut down properly.
         */
        LOG.info("Waiting for worker and boss event loop groups to terminate...");
        try {
            workerWaiter.await(10, TimeUnit.SECONDS);
            bossWaiter.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException iex) {
            LOG.warn("An InterruptedException was caught while waiting for event loops to terminate...");
        }

        if (!m_workerGroup.isTerminated()) {
            LOG.warn("Forcing shutdown of worker event loop...");
            m_workerGroup.shutdownGracefully(0L, 0L, TimeUnit.MILLISECONDS);
        }

        if (!m_bossGroup.isTerminated()) {
            LOG.warn("Forcing shutdown of boss event loop...");
            m_bossGroup.shutdownGracefully(0L, 0L, TimeUnit.MILLISECONDS);
        }

        MessageMetrics metrics = m_metricsCollector.computeMetrics();
        BytesMetrics bytesMetrics = m_bytesMetricsCollector.computeMetrics();
        LOG.info("Metrics messages[read={}, write={}] bytes[read={}, write={}]", metrics.messagesRead(),
                metrics.messagesWrote(), bytesMetrics.readBytes(), bytesMetrics.wroteBytes());
    }

//    private ChannelHandler createSslHandler(SocketChannel channel, SslContext sslContext, boolean needsClientAuth) {
//        SSLEngine sslEngine = sslContext.newEngine(
//                channel.alloc(),
//                channel.remoteAddress().getHostString(),
//                channel.remoteAddress().getPort());
//        sslEngine.setUseClientMode(false);
//        if (needsClientAuth) {
//            sslEngine.setNeedClientAuth(true);
//        }
//        return new SslHandler(sslEngine);
//    }
}
