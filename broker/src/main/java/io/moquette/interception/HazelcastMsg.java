package io.moquette.interception;

import io.moquette.interception.messages.InterceptPublishMessage;

import java.io.Serializable;

/**
 * Created by mackristof on 28/05/2016.
 */
public class HazelcastMsg implements Serializable {

    private final String clientId;
    private final byte qos;
    private final byte[] payload;
    private final String topic;

    public HazelcastMsg(InterceptPublishMessage msg) {
        this.clientId = msg.getClientID();
        this.topic = msg.getTopicName();
        this.qos = msg.getQos().byteValue();
        this.payload = msg.getPayload().array();
    }

    public String getClientId() {
        return clientId;
    }

    public byte getQos() {
        return qos;
    }

    public byte[] getPayload() {
        return payload;
    }

    public String getTopic() {
        return topic;
    }
}
