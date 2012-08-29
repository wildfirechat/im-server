package org.dna.mqtt.moquette.messaging.spi.impl.events;

/**
 *
 * @author andrea
 */
public class UnsubscribeEvent extends MessagingEvent {

    String m_topic;
    String m_clientID;
    
    public UnsubscribeEvent(String topic, String clientID) {
        m_topic = topic;
        m_clientID = clientID;
    }

    public String getTopic() {
        return m_topic;
    }

    public String getClientID() {
        return m_clientID;
    }
    
}
