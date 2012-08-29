package org.dna.mqtt.moquette.messaging.spi.impl.events;

import org.apache.mina.core.session.IoSession;

/**
 *
 * @author andrea
 */
public class DisconnectEvent extends MessagingEvent {
    
    IoSession m_session;
    
    public DisconnectEvent(IoSession session) {
        m_session = session;
    }

    public IoSession getSession() {
        return m_session;
    }
    
    
}
