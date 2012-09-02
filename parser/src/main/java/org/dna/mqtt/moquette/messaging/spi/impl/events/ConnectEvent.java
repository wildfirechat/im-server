package org.dna.mqtt.moquette.messaging.spi.impl.events;

import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;

/**
 */
public class ConnectEvent extends MessagingEvent {

    IoSession m_session;
    ConnectMessage message;

    public ConnectEvent(IoSession session, ConnectMessage message) {
        this.m_session = session;
        this.message = message;
    }

    public IoSession getSession() {
        return m_session;
    }

    public ConnectMessage getMessage() {
        return message;
    }
}
