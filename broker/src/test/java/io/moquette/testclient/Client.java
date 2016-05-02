/*
 * Copyright (c) 2012-2015 The original author or authors
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
package io.moquette.testclient;

import io.moquette.BrokerConstants;
import io.moquette.parser.netty.MQTTDecoder;
import io.moquette.parser.netty.MQTTEncoder;
import io.moquette.parser.proto.messages.ConnAckMessage;
import io.moquette.parser.proto.messages.ConnectMessage;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.moquette.parser.proto.messages.AbstractMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;

/**
 * Class used just to send and receive MQTT messages without any protocol login 
 * in action, just use the encoder/decoder part.
 * 
 * @author andrea
 */
public class Client {
    
    public interface ICallback {
        void call(AbstractMessage msg);
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(Client.class);
    final ClientNettyMQTTHandler handler = new ClientNettyMQTTHandler();
    EventLoopGroup workerGroup;
    Channel m_channel;
    private boolean m_connectionLost = false;
    private ICallback callback;
    private String clientId;
    private AbstractMessage receivedMsg;
    
    public Client(String host) {
        this(host, BrokerConstants.PORT);
    }
    
    public Client(String host, int port) {
        handler.setClient(this);
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
                    pipeline.addLast("handler", handler);
                }
            });
            
            // Start the client.
            m_channel = b.connect(host, port).sync().channel();
        } catch (Exception ex) {    
            LOG.error("Error received in client setup", ex);
            workerGroup.shutdownGracefully();
        }
    }

    public Client clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public void connect(String willTestamentTopic, String willTestamentMsg) {
        ConnectMessage connectMessage = new ConnectMessage();
        connectMessage.setProtocolVersion((byte) 3);
        connectMessage.setClientID(this.clientId);
        connectMessage.setKeepAlive(2); //secs
        connectMessage.setWillFlag(true);
        connectMessage.setWillMessage(willTestamentMsg.getBytes());
        connectMessage.setWillTopic(willTestamentTopic);
        connectMessage.setWillQos(AbstractMessage.QOSType.MOST_ONE.byteValue());

        doConnect(connectMessage);
    }

    public void connect() {
        ConnectMessage connectMessage = new ConnectMessage();
        connectMessage.setProtocolVersion((byte) 4);
        connectMessage.setClientID("");
        connectMessage.setKeepAlive(2); //secs
        connectMessage.setWillFlag(false);
        connectMessage.setWillQos(AbstractMessage.QOSType.MOST_ONE.byteValue());

        doConnect(connectMessage);
    }

    private void doConnect(ConnectMessage connectMessage) {
        final CountDownLatch latch = new CountDownLatch(1);
        this.setCallback(new Client.ICallback() {

            public void call(AbstractMessage msg) {
                receivedMsg = msg;
                latch.countDown();
            }
        });

        this.sendMessage(connectMessage);

        try {
            latch.await(200, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException("Cannot receive message in 200 ms", e);
        }
        if (!(this.receivedMsg instanceof ConnAckMessage)) {
            throw new RuntimeException("Expected a CONN_ACK message but received " + this.receivedMsg.getMessageType());
        }
    }

    public void setCallback(ICallback callback) {
        this.callback = callback;
    }
    
    public void sendMessage(AbstractMessage msg) {
        m_channel.writeAndFlush(msg);
    }

    public AbstractMessage lastReceivedMessage() {
        return this.receivedMsg;
    }

    void messageReceived(AbstractMessage msg) {
        LOG.info("Received message {}", msg);
        if (this.callback != null) {
            this.callback.call(msg);
        }
    }
    
    void setConnectionLost(boolean status) {
        m_connectionLost = status;
    }
    
    public boolean isConnectionLost() {
        return m_connectionLost;
    }

    public void close() throws InterruptedException {
        // Wait until the connection is closed.
        m_channel.closeFuture().sync();
        if (workerGroup == null) {
            throw new IllegalStateException("Invoked close on an Acceptor that wasn't initialized");
        }
        workerGroup.shutdownGracefully();
    }
}
