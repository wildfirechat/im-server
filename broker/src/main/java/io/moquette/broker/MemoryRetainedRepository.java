package io.moquette.broker;

import io.moquette.spi.impl.subscriptions.Topic;
import io.netty.handler.codec.mqtt.MqttPublishMessage;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/*
* In memory retained messages store
* */
final class MemoryRetainedRepository implements IRetainedRepository {

    private final ConcurrentMap<String, MqttPublishMessage> storage = new ConcurrentHashMap<>();

    @Override
    public void cleanRetained(Topic topic) {
        storage.remove(topic.toString());
    }

    @Override
    public void retain(Topic topic, MqttPublishMessage msg) {
        storage.put(topic.toString(), msg);
    }

    @Override
    public boolean isEmtpy() {
        return storage.isEmpty();
    }

    @Override
    public MqttPublishMessage retainedOnTopic(String topic) {
        return storage.get(topic);
    }
}
