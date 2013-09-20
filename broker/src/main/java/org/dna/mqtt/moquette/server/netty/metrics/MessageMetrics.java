package org.dna.mqtt.moquette.server.netty.metrics;

public class MessageMetrics {
    private long m_messagesRead = 0;
    private long m_messageWrote = 0;

    void incrementRead(long numMessages) {
        m_messagesRead += numMessages;
    }

    void incrementWrote(long numMessages) {
        m_messageWrote += numMessages;
    }

    public long messagesRead() {
        return m_messagesRead;
    }

    public long messagesWrote() {
        return m_messageWrote;
    }
}