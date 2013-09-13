package org.dna.mqtt.moquette.messaging.spi.impl.events;

import org.dna.mqtt.moquette.server.ServerChannel;

/**
 *
 * @author andrea
 */
public class DisconnectEvent extends MessagingEvent {
    
    ServerChannel m_session;
    
    public DisconnectEvent(ServerChannel session) {
        m_session = session;
    }

    public ServerChannel getSession() {
        return m_session;
    }
    
    
}
