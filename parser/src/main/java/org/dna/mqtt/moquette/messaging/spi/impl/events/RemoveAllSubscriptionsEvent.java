package org.dna.mqtt.moquette.messaging.spi.impl.events;

/**
 *
 * @author andrea
 */
public class RemoveAllSubscriptionsEvent extends MessagingEvent {

    String m_clientID;

    public RemoveAllSubscriptionsEvent(String clientID) {
        m_clientID = clientID;
    }

    public String getClientID() {
        return m_clientID;
    }
    
}
