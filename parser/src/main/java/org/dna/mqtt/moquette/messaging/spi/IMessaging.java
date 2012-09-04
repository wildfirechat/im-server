package org.dna.mqtt.moquette.messaging.spi;

import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.messaging.spi.impl.events.MessagingEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.events.PubAckEvent;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;

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
    void publish(String topic, byte[] message, QOSType qos, boolean retain, String clientID, IoSession session);

    /**
     * Used to publish messages with QoS > 0
     * */
    void publish(String topic, byte[] message, QOSType qos, boolean retain, String clientID, int messageID, IoSession session);
    
    /**
     * Subscribe a client to a specified topic with a defined level
     */
    void subscribe(String clientId, String topic, QOSType qos, boolean cleanSession, int messageID);

    /**
     * Remove all subscription to any topic the client (identified by clientID)
     * was subscribed
     */
    void removeSubscriptions(String clientID);
    
    void stop();

    /**
     * Remove the clientID from topic subscription, if not previously subscribed,
     * doesn't reply any error
     */
    void unsubscribe(String topic, String clientID);

    void disconnect(IoSession session);

    void republishStored(String clientID);

    void connect(IoSession session, ConnectMessage msg);
}
