package org.dna.mqtt.moquette.messaging.spi.impl;

import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;

/**
 * Maintain the information about which Topic a certain ClientID is subscribed 
 * and at which QoS
 * 
 * 
 * @author andrea
 */
public class Subscription {
    
    QOSType requestedQos;
    String clientId;
    String topic;
    
    public Subscription(String clientId, String topic, QOSType requestedQos) {
        this.requestedQos = requestedQos;
        this.clientId = clientId;
        this.topic = topic;
    }

    public String getClientId() {
        return clientId;
    }

    public QOSType getRequestedQos() {
        return requestedQos;
    }

    public String getTopic() {
        return topic;
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
        if ((this.topic == null) ? (other.topic != null) : !this.topic.equals(other.topic)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + (this.requestedQos != null ? this.requestedQos.hashCode() : 0);
        hash = 37 * hash + (this.clientId != null ? this.clientId.hashCode() : 0);
        hash = 37 * hash + (this.topic != null ? this.topic.hashCode() : 0);
        return hash;
    }

    /**
     * Trivial match method
     */
    boolean match(String topic) {
        return this.topic.equals(topic);
    }
}
