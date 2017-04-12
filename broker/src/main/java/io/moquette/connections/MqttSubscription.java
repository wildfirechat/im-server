/*
 * Copyright (c) 2012-2017 The original author or authors
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

package io.moquette.connections;

/**
 * A class that represents a MQTT subscription.
 */
public class MqttSubscription {

    private final String requestedQos;
    private final String clientId;
    private final String topicFilter;
    private final boolean active;

    public MqttSubscription(String requestedQos, String clientId, String topicFilter, boolean active) {
        this.requestedQos = requestedQos;
        this.clientId = clientId;
        this.topicFilter = topicFilter;
        this.active = active;
    }

    public String getRequestedQos() {
        return requestedQos;
    }

    public String getClientId() {
        return clientId;
    }

    public String getTopicFilter() {
        return topicFilter;
    }

    public boolean isActive() {
        return active;
    }

}
