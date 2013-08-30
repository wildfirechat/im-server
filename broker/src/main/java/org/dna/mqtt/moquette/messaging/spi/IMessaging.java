package org.dna.mqtt.moquette.messaging.spi;

import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;

/**
 * Interface to the underling messaging system used to publish, subscribe.
 * 
 * @author andrea
 */
public interface IMessaging {

    void stop();

    void disconnect(IoSession session);

//    void republishStored(String clientID);

    void handleProtocolMessage(IoSession session, AbstractMessage msg);
}
