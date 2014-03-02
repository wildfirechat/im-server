package org.dna.mqtt.moquette.messaging.spi.impl.events;

import java.util.Properties;

/**
 */
public final class InitEvent extends MessagingEvent {
    
    private Properties config;

    public InitEvent(Properties config) {
        this.config = config;
    }

    public Properties getConfig() {
        return config;
    }
    
}
