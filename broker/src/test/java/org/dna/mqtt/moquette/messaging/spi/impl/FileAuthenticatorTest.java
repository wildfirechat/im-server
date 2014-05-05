package org.dna.mqtt.moquette.messaging.spi.impl;

import org.dna.mqtt.moquette.server.IAuthenticator;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author andrea
 */
public class FileAuthenticatorTest {
    
    @Test
    public void loadPasswordFile_verifyValid() {
        String file = getClass().getResource("/password_file.conf").getPath();        
        IAuthenticator auth = new FileAuthenticator(null, file);
        
        assertTrue(auth.checkValid("testuser", "passwd"));
    }
    
    @Test
    public void loadPasswordFile_verifyInvalid() {
        String file = getClass().getResource("/password_file.conf").getPath();        
        IAuthenticator auth = new FileAuthenticator(null, file);
        
        assertFalse(auth.checkValid("testuser2", "passwd"));
    }
    
    @Test
    public void loadPasswordFile_verifyDirectoryRef() {
        IAuthenticator auth = new FileAuthenticator("", "");
        
        assertFalse(auth.checkValid("testuser2", "passwd"));
    }
    
}