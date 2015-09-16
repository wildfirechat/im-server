package org.eclipse.moquette.spi.impl.security;

/**
 * Created by andrea on 8/23/14.
 */
public class AcceptAllAuthenticator implements IAuthenticator {
    public boolean checkValid(String username, byte[] password) {
        return true;
    }
}
