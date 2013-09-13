package org.dna.mqtt.moquette.messaging.spi.impl.events;

import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.server.ServerChannel;

/**
 * Event used to carry ProtocolMessages from front handler to event processor
 */
public class ProtocolEvent extends MessagingEvent {
    ServerChannel m_session;
    AbstractMessage message;

    public ProtocolEvent(ServerChannel session, AbstractMessage message) {
        this.m_session = session;
        this.message = message;
    }

    public ServerChannel getSession() {
        return m_session;
    }

    public AbstractMessage getMessage() {
        return message;
    }
}
