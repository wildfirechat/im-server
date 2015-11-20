/*
 * Copyright (c) 2012-2015 The original author or authors
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
public class BytesMetricsCollector {

    private AtomicLong readBytes = new AtomicLong();
    private AtomicLong wroteBytes = new AtomicLong();

    public BytesMetrics computeMetrics() {
        BytesMetrics allMetrics = new BytesMetrics();
        allMetrics.incrementRead(readBytes.get());
        allMetrics.incrementWrote(wroteBytes.get());
        return allMetrics;
    }

    public void sumReadBytes(long count) {
        readBytes.getAndAdd(count);
    }

    public void sumWroteBytes(long count) {
        wroteBytes.getAndAdd(count);
    }
}
