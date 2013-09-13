package org.dna.mqtt.moquette.messaging.spi.impl.events;

import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;

import java.io.Serializable;
import org.dna.mqtt.moquette.server.ServerChannel;

/**
 *
 * @author andrea
 */
public class PublishEvent extends MessagingEvent implements Serializable {
    String m_topic;
    QOSType m_qos;
    byte[] m_message;
    boolean m_retain;
    String m_clientID;
    //Optional attribute, available only fo QoS 1 and 2
    int m_msgID;

    transient ServerChannel m_session;
    
    public PublishEvent(String topic, QOSType qos, byte[] message, boolean retain,
            String clientID, ServerChannel session) {
        m_topic = topic;
        m_qos = qos;
        m_message = message;
        m_retain = retain;
        m_clientID = clientID;
        m_session = session;
    }

    public PublishEvent(String topic, QOSType qos, byte[] message, boolean retain,
                        String clientID, int msgID, ServerChannel session) {
        this(topic, qos, message, retain, clientID, session);
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

    public int getMessageID() {
        return m_msgID;
    }

    public ServerChannel getSession() {
        return m_session;
    }

    @Override
    public String toString() {
        return "PublishEvent{" +
                "m_msgID=" + m_msgID +
                ", m_clientID='" + m_clientID + '\'' +
                ", m_retain=" + m_retain +
                ", m_qos=" + m_qos +
                ", m_topic='" + m_topic + '\'' +
                '}';
    }
}
