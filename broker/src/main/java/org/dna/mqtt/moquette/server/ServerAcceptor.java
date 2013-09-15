package org.dna.mqtt.moquette.server;

import java.io.IOException;
import org.dna.mqtt.moquette.messaging.spi.IMessaging;

/**
 *
 * @author andrea
 */
public interface ServerAcceptor {
    
    void initialize(IMessaging messaging) throws IOException;
    
    void close();
}
