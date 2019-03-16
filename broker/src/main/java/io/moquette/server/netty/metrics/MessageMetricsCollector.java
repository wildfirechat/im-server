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

package io.moquette.server.netty.metrics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects all the metrics from the various pipeline.
 */
public class MessageMetricsCollector {

    private AtomicLong readMsgs = new AtomicLong();
    private AtomicLong wroteMsgs = new AtomicLong();

    public MessageMetrics computeMetrics() {
        MessageMetrics allMetrics = new MessageMetrics();
        allMetrics.incrementRead(readMsgs.get());
        allMetrics.incrementWrote(wroteMsgs.get());
        return allMetrics;
    }

    public void sumReadMessages(long count) {
        readMsgs.getAndAdd(count);
    }

    public void sumWroteMessages(long count) {
        wroteMsgs.getAndAdd(count);
    }
}
