package org.dna.mqtt.moquette.messaging.spi.impl;

import com.lmax.disruptor.EventFactory;
import org.dna.mqtt.moquette.messaging.spi.impl.events.MessagingEvent;

/**
 * Carrier value object for the RingBuffer.
 *
 * @author andrea
 */
public final class ValueEvent {

    private MessagingEvent m_event;

    public MessagingEvent getEvent() {
        return m_event;
    }

    public void setEvent(MessagingEvent event) {
        m_event = event;
    }
    
    public final static EventFactory<ValueEvent> EVENT_FACTORY = new EventFactory<ValueEvent>() {

        public ValueEvent newInstance() {
            return new ValueEvent();
        }
    };
}
