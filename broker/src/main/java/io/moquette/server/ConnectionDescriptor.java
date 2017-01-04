/*
 * Copyright (c) 2012-2017 The original author or authorsgetRockQuestions()
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

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Value object to maintain the information of single connection, like ClientID, Channel,
 * and clean session flag.
 *
 *
 * @author andrea
 */
public class ConnectionDescriptor {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionDescriptor.class);

    public enum ConnectionState {
        //Connection states
        DISCONNECTED, SENDACK, SESSION_CREATED, MESSAGES_REPUBLISHED, ESTABLISHED,
        //Disconnection states
        SUBSCRIPTIONS_REMOVED, MESSAGES_DROPPED, INTERCEPTORS_NOTIFIED;
    }

    public final String clientID;
    public final Channel channel;
    public final boolean cleanSession;
    private final AtomicReference<ConnectionState> channelState = new AtomicReference<>(ConnectionState.DISCONNECTED);

    public ConnectionDescriptor(String clientID, Channel session, boolean cleanSession) {
        this.clientID = clientID;
        this.channel = session;
        this.cleanSession = cleanSession;
    }

    public void abort() {
        LOG.info("closing the channel");
//        try {
            //this.channel.disconnect().sync();
            this.channel.close();//.sync();
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }

    public boolean assignState(ConnectionState expected, ConnectionState newState) {
        return channelState.compareAndSet(expected, newState);
    }

    @Override
    public String toString() {
        return "ConnectionDescriptor{" + "clientID=" + clientID +
                ", cleanSession=" + cleanSession +
                ", state=" + channelState.get() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ConnectionDescriptor that = (ConnectionDescriptor) o;

        if (clientID != null ? !clientID.equals(that.clientID) : that.clientID != null) return false;
        return !(channel != null ? !channel.equals(that.channel) : that.channel != null);

    }

    @Override
    public int hashCode() {
        int result = clientID != null ? clientID.hashCode() : 0;
        result = 31 * result + (channel != null ? channel.hashCode() : 0);
        return result;
    }
}
