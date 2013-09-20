package org.dna.mqtt.moquette.server.netty.metrics;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Collects all the metrics from the various pipeline.
 */
public class MessageMetricsCollector {
    private Queue<MessageMetrics> m_allMetrics = new ConcurrentLinkedQueue<MessageMetrics>();

    void addMetrics(MessageMetrics metrics) {
        m_allMetrics.add(metrics);
    }

    public MessageMetrics computeMetrics() {
        MessageMetrics allMetrics = new MessageMetrics();
        for (MessageMetrics m : m_allMetrics) {
            allMetrics.incrementRead(m.messagesRead());
            allMetrics.incrementWrote(m.messagesWrote());
        }
        return allMetrics;
    }
}
