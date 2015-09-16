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
package org.eclipse.moquette.proto.messages;

/**
 * The attributes Qos, Dup and Retain aren't used for Connect message
 * 
 * @author andrea
 */
public class ConnectMessage extends AbstractMessage {
    protected String m_protocolName;
    protected byte m_protocolVersion;
    
    //Connection flags
    protected boolean m_cleanSession;
    protected boolean m_willFlag;
    protected byte m_willQos;
    protected boolean m_willRetain;
    protected boolean m_passwordFlag;
    protected boolean m_userFlag;
    protected int m_keepAlive;
    
    //Variable part
    protected String m_username;
    protected byte[] m_password;
    protected String m_clientID;
    protected String m_willtopic;
    protected byte[] m_willMessage;
    
    public ConnectMessage() {
        m_messageType = AbstractMessage.CONNECT;
    }

    public boolean isCleanSession() {
        return m_cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.m_cleanSession = cleanSession;
    }

    public int getKeepAlive() {
        return m_keepAlive;
    }

    public void setKeepAlive(int keepAlive) {
        this.m_keepAlive = keepAlive;
    }

    public boolean isPasswordFlag() {
        return m_passwordFlag;
    }

    public void setPasswordFlag(boolean passwordFlag) {
        this.m_passwordFlag = passwordFlag;
    }

    public byte getProtocolVersion() {
        return m_protocolVersion;
    }

    public void setProtocolVersion(byte protocolVersion) {
        this.m_protocolVersion = protocolVersion;
    }

    public String getProtocolName() {
        return m_protocolName;
    }

    public void setProtocolName(String protocolName) {
        this.m_protocolName = protocolName;
    }

    public boolean isUserFlag() {
        return m_userFlag;
    }

    public void setUserFlag(boolean userFlag) {
        this.m_userFlag = userFlag;
    }

    public boolean isWillFlag() {
        return m_willFlag;
    }

    public void setWillFlag(boolean willFlag) {
        this.m_willFlag = willFlag;
    }

    public byte getWillQos() {
        return m_willQos;
    }

    public void setWillQos(byte willQos) {
        this.m_willQos = willQos;
    }

    public boolean isWillRetain() {
        return m_willRetain;
    }

    public void setWillRetain(boolean willRetain) {
        this.m_willRetain = willRetain;
    }

    public byte[] getPassword() {
        return m_password;
    }

    public void setPassword(byte[] password) {
        this.m_password = password;
    }

    public String getUsername() {
        return m_username;
    }

    public void setUsername(String username) {
        this.m_username = username;
    }

    public String getClientID() {
        return m_clientID;
    }

    public void setClientID(String clientID) {
        this.m_clientID = clientID;
    }

    public String getWillTopic() {
        return m_willtopic;
    }

    public void setWillTopic(String topic) {
        this.m_willtopic = topic;
    }

    public byte[] getWillMessage() {
        return m_willMessage;
    }

    public void setWillMessage(byte[] willMessage) {
        this.m_willMessage = willMessage;
    }

    @Override
    public String toString() {
        String base = String.format("Connect [clientID: %s, prot: %s, ver: %02X, clean: %b]", m_clientID, m_protocolName, m_protocolVersion, m_cleanSession);
        if (m_willFlag) {
             base += String.format(" Will [QoS: %d, retain: %b]", m_willQos, m_willRetain);
        }
        return base;
    }

}
