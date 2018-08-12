package io.moquette.broker;

import io.moquette.spi.impl.subscriptions.Topic;
import io.netty.handler.codec.mqtt.MqttPublishMessage;

public interface IRetainedRepository {
    void cleanRetained(Topic topic);

    void retain(Topic topic, MqttPublishMessage msg);

    boolean isEmtpy();

    MqttPublishMessage retainedOnTopic(String topic);
}
