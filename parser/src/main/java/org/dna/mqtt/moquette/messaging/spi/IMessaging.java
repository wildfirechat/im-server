package org.dna.mqtt.moquette.messaging.spi;

import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.messaging.spi.impl.events.MessagingEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.events.PubAckEvent;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;

/**
 * Interface to the underling messaging system used to publish, subscribe.
 * 
 * @author andrea
 */
public interface IMessaging {

    /**
     * Subscribe a client to a specified topic with a defined level
     */
    void subscribe(String clientId, String topic, QOSType qos, boolean cleanSession, int messageID);

    void stop();

    /**
     * Remove the clientID from topic subscription, if not previously subscribed,
     * doesn't reply any error
     */
    //void unsubscribe(String topic, String clientID);

    void disconnect(IoSession session);

    void republishStored(String clientID);

    void handleProtocolMessage(IoSession session, AbstractMessage msg);
}
