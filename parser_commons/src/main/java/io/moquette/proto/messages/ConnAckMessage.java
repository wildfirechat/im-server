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
package io.moquette.proto.messages;

/**
 * The attributes Qos, Dup and Retain aren't used.
 * 
 * @author andrea
 */
public class ConnAckMessage extends AbstractMessage {
    public static final byte CONNECTION_ACCEPTED = 0x00;
    public static final byte UNNACEPTABLE_PROTOCOL_VERSION = 0x01;
    public static final byte IDENTIFIER_REJECTED = 0x02;
    public static final byte SERVER_UNAVAILABLE = 0x03;
    public static final byte BAD_USERNAME_OR_PASSWORD = 0x04;
    public static final byte NOT_AUTHORIZED = 0x05;
    
    private byte m_returnCode;
    private boolean sessionPresent;
    
    public ConnAckMessage() {
        m_messageType = CONNACK;
    }

    public byte getReturnCode() {
        return m_returnCode;
    }

    public void setReturnCode(byte returnCode) {
        this.m_returnCode = returnCode;
    }

    public boolean isSessionPresent() {
        return this.sessionPresent;
    }

    public void setSessionPresent(boolean present) {
        this.sessionPresent = present;
    }
}
