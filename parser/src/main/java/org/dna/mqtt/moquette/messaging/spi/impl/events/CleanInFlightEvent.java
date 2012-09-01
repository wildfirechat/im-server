package org.dna.mqtt.moquette.messaging.spi.impl.events;

/**
 * Message to remove the message of  type QoS 1 or 2 from in flight store
 */
public class CleanInFlightEvent extends MessagingEvent {

    private String m_msgId;

    public CleanInFlightEvent(String msgId) {
        m_msgId = msgId;
    }

    public String getMsgId() {
        return m_msgId;
    }
}
