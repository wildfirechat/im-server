/*
 * Copyright (c) 2012-2017 The original author or authors
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
import io.moquette.server.netty.MessageBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Class used just to send and receive MQTT messages without any protocol login in action, just use
 * the encoder/decoder part.
 */
public class Client {

    public interface ICallback {

        void call(MqttMessage msg);
    }

    private static final Logger LOG = LoggerFactory.getLogger(Client.class);
    final ClientNettyMQTTHandler handler = new ClientNettyMQTTHandler();
    EventLoopGroup workerGroup;
    Channel m_channel;
    private boolean m_connectionLost;
    private ICallback callback;
    private String clientId;
    private MqttMessage receivedMsg;

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
                    pipeline.addLast("decoder", new MqttDecoder());
                    pipeline.addLast("encoder", MqttEncoder.INSTANCE);
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
        MqttFixedHeader mqttFixedHeader = new MqttFixedHeader(
                MqttMessageType.CONNECT,
                false,
                MqttQoS.AT_MOST_ONCE,
                false,
                0);
        MqttConnectVariableHeader mqttConnectVariableHeader = new MqttConnectVariableHeader(
                MqttVersion.MQTT_3_1.protocolName(),
                MqttVersion.MQTT_3_1.protocolLevel(),
                false,
                false,
                false,
                MqttQoS.AT_MOST_ONCE.value(),
                true,
                true,
                2);
        MqttConnectPayload mqttConnectPayload = new MqttConnectPayload(
                this.clientId,
                willTestamentTopic,
                willTestamentMsg,
                null,
                null);
        MqttConnectMessage connectMessage = new MqttConnectMessage(
                mqttFixedHeader,
                mqttConnectVariableHeader,
                mqttConnectPayload);

        /*
         * ConnectMessage connectMessage = new ConnectMessage();
         * connectMessage.setProtocolVersion((byte) 3); connectMessage.setClientID(this.clientId);
         * connectMessage.setKeepAlive(2); //secs connectMessage.setWillFlag(true);
         * connectMessage.setWillMessage(willTestamentMsg.getBytes());
         * connectMessage.setWillTopic(willTestamentTopic);
         * connectMessage.setWillQos(MqttQoS.AT_MOST_ONCE.byteValue());
         */

        doConnect(connectMessage);
    }

    public void connect() {
        MqttConnectMessage connectMessage = MessageBuilder.connect().protocolVersion(MqttVersion.MQTT_3_1_1)
                .clientId("").keepAlive(2) // secs
                .willFlag(false).willQoS(MqttQoS.AT_MOST_ONCE).build();

        doConnect(connectMessage);
    }

    private void doConnect(MqttConnectMessage connectMessage) {
        final CountDownLatch latch = new CountDownLatch(1);
        this.setCallback(new Client.ICallback() {

            public void call(MqttMessage msg) {
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
        if (!(this.receivedMsg instanceof MqttConnAckMessage)) {
            MqttMessageType messageType = this.receivedMsg.fixedHeader().messageType();
            throw new RuntimeException("Expected a CONN_ACK message but received " + messageType);
        }
    }

    public void setCallback(ICallback callback) {
        this.callback = callback;
    }

    public void sendMessage(MqttMessage msg) {
        m_channel.writeAndFlush(msg);
    }

    public MqttMessage lastReceivedMessage() {
        return this.receivedMsg;
    }

    void messageReceived(MqttMessage msg) {
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
