package io.moquette.spi.impl;

import io.netty.handler.codec.mqtt.MqttQoS;

import java.nio.ByteBuffer;

public final class WillMessage {

    private final String topic;
    private final ByteBuffer payload;
    private final boolean retained;
    private final MqttQoS qos;

    public WillMessage(String topic, ByteBuffer payload, boolean retained, MqttQoS qos) {
        this.topic = topic;
        this.payload = payload;
        this.retained = retained;
        this.qos = qos;
    }

    public String getTopic() {
        return topic;
    }

    public ByteBuffer getPayload() {
        return payload;
    }

    public boolean isRetained() {
        return retained;
    }

    public MqttQoS getQos() {
        return qos;
    }
}
