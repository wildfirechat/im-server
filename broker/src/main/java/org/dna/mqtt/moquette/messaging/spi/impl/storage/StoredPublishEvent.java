package org.dna.mqtt.moquette.messaging.spi.impl.storage;

import java.io.Serializable;
import java.nio.ByteBuffer;
import org.dna.mqtt.moquette.messaging.spi.impl.events.PublishEvent;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;

/**
 * Publish event serialized to the DB.
 * 
 * @author andrea
 */
public class StoredPublishEvent implements Serializable {
    String m_topic;
    QOSType m_qos;
    byte[] m_message;
    boolean m_retain;
    String m_clientID;
    //Optional attribute, available only fo QoS 1 and 2
    int m_msgID;
    
    public StoredPublishEvent(PublishEvent wrapped) {
        m_topic = wrapped.getTopic();
        m_qos = wrapped.getQos();
        m_retain = wrapped.isRetain();
        m_clientID = wrapped.getClientID();
        m_msgID = wrapped.getMessageID();
        
        ByteBuffer buffer = wrapped.getMessage();
        m_message = new byte[buffer.remaining()];
        buffer.get(m_message);
        buffer.rewind();
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
}
