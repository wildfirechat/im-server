/*
 * Copyright (c) 2012-2018 The original author or authors
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
package io.moquette.broker;

import io.moquette.broker.subscriptions.Topic;
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
