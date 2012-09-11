package org.dna.mqtt.moquette.messaging.spi.impl.events;

import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;

/**
 * TODO it's identical to PublishEvent
 *
 * @author andrea
 */
public class NotifyEvent extends MessagingEvent {
    
    private String m_clientId;
    private String m_topic;
    private QOSType m_qos;
    private byte[] m_message;
    private boolean m_retaned;

    //Optional attribute, available only fo QoS 1 and 2
    int m_msgID;
    

    public NotifyEvent(String clientId, String topic, QOSType qos, byte[] message, boolean retained) {
        m_clientId = clientId;
        m_topic = topic;
        m_qos = qos;
        m_message = message;
        m_retaned = retained;
    }

    public NotifyEvent(String clientId, String topic, QOSType qos, byte[] message, boolean retained, int msgID) {
        this(clientId, topic, qos, message, retained);
        m_msgID = msgID;
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

    public int getMessageID() {
        return m_msgID;
    }

    @Override
    public String toString() {
        return "NotifyEvent{" +
                "m_retaned=" + m_retaned +
                ", m_msgID=" + m_msgID +
                ", m_qos=" + m_qos +
                ", m_topic='" + m_topic + '\'' +
                ", m_clientId='" + m_clientId + '\'' +
                '}';
    }
}
