package org.dna.mqtt.moquette.messaging.spi.impl.events;

import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;

/**
 *
 * @author andrea
 */
public class PublishEvent extends MessagingEvent {
    String m_topic;
    QOSType m_qos;
    byte[] m_message;
    boolean m_retain;
    
    public PublishEvent(String topic, QOSType qos, byte[] message, boolean retain) {
        m_topic = topic;
        m_qos = qos;
        m_message = message;
        m_retain = retain;
    }
    
    public String getTopic() {
        return m_topic;
    }

    public QOSType getQos() {
        return m_qos;
    }

    public byte[] getMessage() {
        return m_message;
    }

    public boolean isRetain() {
        return m_retain;
    }
}
