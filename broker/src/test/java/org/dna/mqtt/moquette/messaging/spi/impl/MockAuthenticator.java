package org.dna.mqtt.moquette.messaging.spi.impl;

import java.util.Map;
import org.dna.mqtt.moquette.server.IAuthenticator;

/**
 * Test utility to implements authenticator instance.
 * 
 * @author andrea
 */
class MockAuthenticator implements IAuthenticator {
    
    private Map<String, String> m_userPwds;
    
    MockAuthenticator(Map<String, String> userPwds) {
        m_userPwds = userPwds;
    }

    public boolean checkValid(String username, String password) {
        if (!m_userPwds.containsKey(username)) {
            return false;
        }
        return m_userPwds.get(username).equals(password);
    }
    
}
