package org.dna.mqtt.moquette.messaging.spi;

import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.server.ServerChannel;

/**
 * Interface to the underling messaging system used to publish, subscribe.
 * 
 * @author andrea
 */
public interface IMessaging {

    void stop();

    void disconnect(ServerChannel session);
    
    void lostConnection(String clientID);

    void handleProtocolMessage(ServerChannel session, AbstractMessage msg);
}
