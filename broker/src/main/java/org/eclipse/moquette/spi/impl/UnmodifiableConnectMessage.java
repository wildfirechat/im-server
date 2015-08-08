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
package org.eclipse.moquette.spi.impl;

import org.eclipse.moquette.proto.messages.ConnectMessage;

/**
 * A ConnectMessage that is unmodifiable with copy through copy constructor.
 *
 * @author andrea
 */
final class UnmodifiableConnectMessage extends ConnectMessage {

    /**
     * Copy constructor
     */
    UnmodifiableConnectMessage(ConnectMessage orig) {
        this.m_protocolName = orig.getProtocolName();
        this.m_protocolVersion = orig.getProtocolVersion();
        this.m_cleanSession = orig.isCleanSession();
        this.m_willFlag = orig.isWillFlag();
        this.m_willQos = orig.getWillQos();
        this.m_willRetain = orig.isWillRetain();
        this.m_passwordFlag = orig.isPasswordFlag();
        this.m_userFlag = orig.isUserFlag();
        this.m_keepAlive = orig.getKeepAlive();
        this.m_username = orig.getUsername();
        this.m_password = orig.getPassword();
        this.m_clientID = orig.getClientID();
        this.m_willtopic = orig.getWillTopic();
        this.m_willMessage = orig.getWillMessage();
    }

    @Override
    public void setCleanSession(boolean cleanSession) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setKeepAlive(int keepAlive) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPasswordFlag(boolean passwordFlag) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProtocolVersion(byte protocolVersion) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setProtocolName(String protocolName) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUserFlag(boolean userFlag) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setWillFlag(boolean willFlag) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setWillQos(byte willQos) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setWillRetain(boolean willRetain) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setPassword(String password) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setUsername(String username) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setClientID(String clientID) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setWillTopic(String topic) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setWillMessage(String willMessage) {
        throw new UnsupportedOperationException();
    }
}
