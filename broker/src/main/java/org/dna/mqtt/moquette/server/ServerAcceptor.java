package org.dna.mqtt.moquette.server;

import java.io.IOException;
import java.util.Properties;
import org.dna.mqtt.moquette.messaging.spi.IMessaging;

/**
 *
 * @author andrea
 */
public interface ServerAcceptor {
    
    void initialize(IMessaging messaging, Properties props) throws IOException;
    
    void close();
}
