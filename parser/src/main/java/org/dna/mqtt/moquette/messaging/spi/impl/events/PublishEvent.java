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
    String m_clientID;
    //Optional attribute, available only fo QoS 1 and 2
    String m_msgID;
    
    public PublishEvent(String topic, QOSType qos, byte[] message, boolean retain,
            String clientID) {
        m_topic = topic;
        m_qos = qos;
        m_message = message;
        m_retain = retain;
        m_clientID = clientID;
    }

    public PublishEvent(String topic, QOSType qos, byte[] message, boolean retain,
                        String clientID, String msgID) {
        this(topic, qos, message, retain, clientID);
        m_msgID = msgID;
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
    
    public String getClientID() {
        return m_clientID;
    }

    public String getMessageID() {
        return m_msgID;
    }
}
