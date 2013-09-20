package org.dna.mqtt.moquette.server.netty.metrics;

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
