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

package io.moquette.interception;

import io.moquette.interception.messages.InterceptPublishMessage;
import java.io.Serializable;
import static io.moquette.spi.impl.Utils.readBytesAndRewind;

public class HazelcastMsg implements Serializable {

    private static final long serialVersionUID = -1431584750134928273L;
    private final String clientId;
    private final int qos;
    private final byte[] payload;
    private final String topic;

    public HazelcastMsg(InterceptPublishMessage msg) {
        this.clientId = msg.getClientID();
        this.topic = msg.getTopicName();
        this.qos = msg.getQos().value();
        this.payload = readBytesAndRewind(msg.getPayload());
    }

    public String getClientId() {
        return clientId;
    }

    public int getQos() {
        return qos;
    }

    public byte[] getPayload() {
        return payload;
    }

    public String getTopic() {
        return topic;
    }
}
