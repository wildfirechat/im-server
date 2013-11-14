package org.dna.mqtt.moquette.messaging.spi.impl.events;

import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.server.ServerChannel;

/**
 *
 * @author andrea
 */
public class OutputMessagingEvent extends MessagingEvent {
    private ServerChannel m_channel;
    private AbstractMessage m_message;

    public OutputMessagingEvent(ServerChannel channel, AbstractMessage message) {
        m_channel = channel;
        m_message = message;
    }

    public ServerChannel getChannel() {
        return m_channel;
    }

    public AbstractMessage getMessage() {
        return m_message;
    }
    
    
}
