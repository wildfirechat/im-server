package org.dna.mqtt.moquette.messaging.spi.impl.events;

import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;

/**
 *
 * @author andrea
 */
public class NotifyEvent extends MessagingEvent {
    
    private String m_clientId;
    private String m_topic;
    private QOSType m_qos;
    private byte[] m_message;
    private boolean m_retaned;
    

    public NotifyEvent(String clientId, String topic, QOSType qos, byte[] message, boolean retained) {
        m_clientId = clientId;
        m_topic = topic;
        m_qos = qos;
        m_message = message;
        m_retaned = retained;
    }

    public String getClientId() {
        return m_clientId;
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

    public boolean isRetained() {
        return m_retaned;
    }
    
}
