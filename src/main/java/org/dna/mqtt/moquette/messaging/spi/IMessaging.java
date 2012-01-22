package org.dna.mqtt.moquette.messaging.spi;

/**
 * Interface to the underling messaging system used to publish, subscribe.
 * 
 * @author andrea
 */
public class IMessaging {

    /**
     * Publish a message on the messaging system. The qos is the level of quality 
     * the retain flag is true iff the message should be persisted for future 
     * notification also after all the current subscriber to the given topic has
     * been notified.
     */
    public void publish(String topic, Object message, byte qos, boolean retain) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
    
}
