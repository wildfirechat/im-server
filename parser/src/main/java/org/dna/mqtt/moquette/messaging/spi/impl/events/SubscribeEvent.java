package org.dna.mqtt.moquette.messaging.spi.impl.events;

import org.dna.mqtt.moquette.messaging.spi.impl.Subscription;

/**
 *
 * @author andrea
 */
public class SubscribeEvent extends MessagingEvent {

    Subscription m_subscription;
    
    public SubscribeEvent(Subscription subscription) {
        m_subscription = subscription;
    }

    public Subscription getSubscription() {
        return m_subscription;
    }
    
}
