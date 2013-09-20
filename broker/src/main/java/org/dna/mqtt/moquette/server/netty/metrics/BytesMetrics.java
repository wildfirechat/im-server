package org.dna.mqtt.moquette.server.netty.metrics;

public class BytesMetrics {
    private long m_readBytes = 0;
    private long m_wroteBytes = 0;

    void incrementRead(long numBytes) {
        m_readBytes += numBytes;
    }

    void incrementWrote(long numBytes) {
        m_wroteBytes += numBytes;
    }

    public long readBytes() {
        return m_readBytes;
    }

    public long wroteBytes() {
        return m_wroteBytes;
    }
}