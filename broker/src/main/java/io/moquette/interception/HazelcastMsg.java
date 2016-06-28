package io.moquette.interception;

import java.io.Serializable;

/**
 * Created by mackristof on 28/05/2016.
 */
public class HazelcastMsg implements Serializable{

    private final String clientId;
    private final String userName;
    private final byte qos;
    private final String payload;
    private final String topic;

    public HazelcastMsg(String clientId, String topic , byte qos, byte[] payload, String userName) {
        this.clientId = clientId;
        this.qos = qos;
        this.payload = new String(payload);
        this.userName = userName;
        this.topic = topic;
    }


    public String getClientId() {
        return clientId;
    }

    public byte getQos() {
        return qos;
    }

    public String getPayload() {
        return payload;
    }

    public String getUserName() {
        return userName;
    }

    public String getTopic() {
        return topic;
    }
}
