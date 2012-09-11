package org.dna.mqtt.moquette.messaging.spi.impl.events;

import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;

/**
 * Event used to carry ProtocolMessages from front handler to event processor
 */
public class ProtocolEvent extends MessagingEvent {
    IoSession m_session;
    AbstractMessage message;

    public ProtocolEvent(IoSession session, AbstractMessage message) {
        this.m_session = session;
        this.message = message;
    }

    public IoSession getSession() {
        return m_session;
    }

    public AbstractMessage getMessage() {
        return message;
    }
}
