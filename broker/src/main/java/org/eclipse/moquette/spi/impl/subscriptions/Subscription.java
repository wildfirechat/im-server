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

    public Subscription(Subscription orig) {
        this.requestedQos = orig.requestedQos;
        this.clientId = orig.clientId;
        this.topicFilter = orig.topicFilter;
        this.cleanSession = orig.cleanSession;
        this.active = orig.active;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Subscription that = (Subscription) o;

        if (cleanSession != that.cleanSession) return false;
        if (clientId != null ? !clientId.equals(that.clientId) : that.clientId != null) return false;
        if (requestedQos != that.requestedQos) return false;
        if (topicFilter != null ? !topicFilter.equals(that.topicFilter) : that.topicFilter != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = requestedQos.hashCode();
        result = 31 * result + (clientId != null ? clientId.hashCode() : 0);
        result = 31 * result + (topicFilter != null ? topicFilter.hashCode() : 0);
        result = 31 * result + (cleanSession ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("[filter:%s, cliID: %s, qos: %s, active: %s]", this.topicFilter, this.clientId, this.requestedQos, this.active);
    }

    @Override
    public Subscription clone() {
        try {
            return (Subscription) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
