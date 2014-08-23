package org.dna.mqtt.moquette.messaging.spi.impl;

import org.dna.mqtt.moquette.server.IAuthenticator;

/**
 * Created by andrea on 8/23/14.
 */
class AcceptAllAuthenticator implements IAuthenticator {
    public boolean checkValid(String username, String password) {
        return true;
    }
}
