/*
 * Copyright (c) 2012-2014 The original author or authors
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
package org.eclipse.moquette.server.netty.metrics;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Collects all the metrics from the various pipeline.
 */
public class BytesMetricsCollector {
    private Queue<BytesMetrics> m_allMetrics = new ConcurrentLinkedQueue<BytesMetrics>();

    void addMetrics(BytesMetrics metrics) {
        m_allMetrics.add(metrics);
    }

    public BytesMetrics computeMetrics() {
        BytesMetrics allMetrics = new BytesMetrics();
        for (BytesMetrics m : m_allMetrics) {
            allMetrics.incrementRead(m.readBytes());
            allMetrics.incrementWrote(m.wroteBytes());
        }
        return allMetrics;
    }
}
