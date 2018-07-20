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

package io.moquette.server;

import io.moquette.server.netty.NettyUtils;
import io.moquette.server.netty.metrics.BytesMetrics;
import io.moquette.server.netty.metrics.BytesMetricsHandler;
import io.moquette.server.netty.metrics.MessageMetrics;
import io.moquette.server.netty.metrics.MessageMetricsHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import static io.netty.channel.ChannelFutureListener.CLOSE_ON_FAILURE;
import static io.netty.channel.ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE;

/**
 * Value object to maintain the information of single connection, like ClientID, Channel, and clean
 * session flag.
 */
public class ConnectionDescriptor {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionDescriptor.class);

    public enum ConnectionState {
        // Connection states
        DISCONNECTED,
        INIT_SESSION,
        SENDACK,
        MESSAGES_REPUBLISHED,
        ESTABLISHED,
        // Disconnection states
        SUBSCRIPTIONS_REMOVED,
        MESSAGES_DROPPED,
        INTERCEPTORS_NOTIFIED;
    }

    public final String clientID;
    private final Channel channel;
    public final boolean cleanSession;
    private final AtomicReference<ConnectionState> channelState = new AtomicReference<>(ConnectionState.DISCONNECTED);

    public ConnectionDescriptor(String clientID, Channel session, boolean cleanSession) {
        this.clientID = clientID;
        this.channel = session;
        this.cleanSession = cleanSession;
    }

    public void writeAndFlush(Object payload) {
        this.channel.writeAndFlush(payload).addListener(FIRE_EXCEPTION_ON_FAILURE);
    }

    public void writeAndFlush(Object payload, ChannelFutureListener listener) {
        this.channel.writeAndFlush(payload).addListener(listener);
    }

    public boolean doesNotUseChannel(Channel channel) {
        return !(this.channel.equals(channel));
    }

    public boolean close() {
        LOG.debug("Closing connection descriptor. MqttClientId = {}.", clientID);
        final boolean success = assignState(ConnectionState.INTERCEPTORS_NOTIFIED, ConnectionState.DISCONNECTED);
        if (!success) {
            return false;
        }
        this.channel.close().addListener(CLOSE_ON_FAILURE);
        return true;
    }

    public String getUsername() {
        return NettyUtils.userName(this.channel);
    }

    public void abort() {
        LOG.info("Closing connection descriptor clientId = {}.", clientID);
        this.channel.close().addListener(CLOSE_ON_FAILURE);
    }

    public boolean assignState(ConnectionState expected, ConnectionState newState) {
        LOG.debug("Updating state of connection descriptor. MqttClientId = {}, expectedState = {}, newState = {}.",
                  clientID, expected, newState);
        boolean retval = channelState.compareAndSet(expected, newState);
        if (!retval) {
            LOG.error("Unable to update state of connection descriptor. CId = {}, expectedState = {}, newState = {}.",
                      clientID, expected, newState);
        }
        return retval;
    }

    public SocketAddress getRemoteAddress() {
        return this.channel.remoteAddress();
    }

    @Override
    public String toString() {
        return "ConnectionDescriptor{" + "clientID=" + clientID + ", removeTemporaryQoS2=" + cleanSession + ", state="
                + channelState.get() + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ConnectionDescriptor that = (ConnectionDescriptor) o;

        if (clientID != null ? !clientID.equals(that.clientID) : that.clientID != null)
            return false;
        return !(channel != null ? !channel.equals(that.channel) : that.channel != null);
    }

    public BytesMetrics getBytesMetrics() {
        return BytesMetricsHandler.getBytesMetrics(channel);
    }

    public MessageMetrics getMessageMetrics() {
        return MessageMetricsHandler.getMessageMetrics(channel);
    }

    @Override
    public int hashCode() {
        int result = clientID != null ? clientID.hashCode() : 0;
        result = 31 * result + (channel != null ? channel.hashCode() : 0);
        return result;
    }
}
