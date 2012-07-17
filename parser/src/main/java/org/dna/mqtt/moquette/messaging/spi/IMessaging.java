package org.dna.mqtt.moquette.messaging.spi;

import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;

/**
 * Interface to the underling messaging system used to publish, subscribe.
 * 
 * @author andrea
 */
public interface IMessaging {

    /**
     * Publish a message on the messaging system. The qos is the level of quality 
     * the retain flag is true iff the message should be persisted for future 
     * notification also after all the current subscriber to the given topic has
     * been notified.
     */
    void publish(String topic, byte[] message, QOSType qos, boolean retain);
    
    /**
     * Subscribe a client to a specified topic with a defined level
     */
    void subscribe(String clientId, String topic, QOSType qos);
}
