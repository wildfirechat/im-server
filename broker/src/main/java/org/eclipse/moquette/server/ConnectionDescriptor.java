/*
 * Copyright (c) 2012-2014 The original author or authors
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
package org.eclipse.moquette.server;

/**
 * Maintains the information of single connection, like ClientID, IoSession,
 * and other connection related flags.
 * 
 * 
 * @author andrea
 */
public class ConnectionDescriptor {
    
    private String m_clientID;
    private ServerChannel m_session;
    private boolean m_cleanSession;
    
    public ConnectionDescriptor(String clientID, ServerChannel session, boolean cleanSession) {
        this.m_clientID = clientID;
        this.m_session = session;
        this.m_cleanSession = cleanSession;
    }
    
    public boolean isCleanSession() {
        return m_cleanSession;
    }

    public String getClientID() {
        return m_clientID;
    }

    public ServerChannel getSession() {
        return m_session;
    }

    @Override
    public String toString() {
        return "ConnectionDescriptor{" + "m_clientID=" + m_clientID + ", m_cleanSession=" + m_cleanSession + '}';
    }
}
