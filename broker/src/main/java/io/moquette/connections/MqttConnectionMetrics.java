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
 * A class that represents the metrics of a given MQTT connection.
 */
public class MqttConnectionMetrics {

    private long readKb;
    private long writtenKb;
    private long readMessages;
    private long writtenMessages;

    public MqttConnectionMetrics(long readBytes, long writtenBytes, long readMessages, long writtenMessages) {
        this.readKb = readBytes / 1024;
        this.writtenKb = writtenBytes / 1024;
        this.readMessages = readMessages;
        this.writtenMessages = writtenMessages;
    }

    public long getReadKb() {
        return readKb;
    }

    public long getWrittenKb() {
        return writtenKb;
    }

    public long getReadMessages() {
        return readMessages;
    }

    public long getWrittenMessages() {
        return writtenMessages;
    }

}
