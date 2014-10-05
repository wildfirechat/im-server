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
package org.eclipse.moquette.spi.impl.subscriptions;

import java.io.Serializable;
import org.eclipse.moquette.proto.messages.AbstractMessage.QOSType;

/**
 * Maintain the information about which Topic a certain ClientID is subscribed 
 * and at which QoS
 * 
 * 
 * @author andrea
 */
public class Subscription implements Serializable {
    
    QOSType requestedQos; //max QoS acceptable
    String clientId;
    String topicFilter;
    boolean cleanSession;
    boolean active = true;
    
    public Subscription(String clientId, String topicFilter, QOSType requestedQos, boolean cleanSession) {
        this.requestedQos = requestedQos;
        this.clientId = clientId;
        this.topicFilter = topicFilter;
        this.cleanSession = cleanSession;
    }
    
    /**
     * Factory method for empty subscriptions
     */
    public static final Subscription createEmptySubscription(String clientId, boolean cleanSession) {
        return new Subscription(clientId, "", QOSType.MOST_ONE, cleanSession);
    }

    public String getClientId() {
        return clientId;
    }

    public QOSType getRequestedQos() {
        return requestedQos;
    }

    public String getTopicFilter() {
        return topicFilter;
    }

    public boolean isCleanSession() {
        return this.cleanSession;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Subscription other = (Subscription) obj;
        if (this.requestedQos != other.requestedQos) {
            return false;
        }
        if ((this.clientId == null) ? (other.clientId != null) : !this.clientId.equals(other.clientId)) {
            return false;
        }
        if ((this.topicFilter == null) ? (other.topicFilter != null) : !this.topicFilter.equals(other.topicFilter)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + (this.requestedQos != null ? this.requestedQos.hashCode() : 0);
        hash = 37 * hash + (this.clientId != null ? this.clientId.hashCode() : 0);
        hash = 37 * hash + (this.topicFilter != null ? this.topicFilter.hashCode() : 0);
        return hash;
    }

    /**
     * Trivial match method
     */
    boolean match(String topic) {
        return this.topicFilter.equals(topic);
    }
    
    @Override
    public String toString() {
        return String.format("[filter:%s, cliID: %s, qos: %s, active: %s]", this.topicFilter, this.clientId, this.requestedQos, this.active);
    }
}
