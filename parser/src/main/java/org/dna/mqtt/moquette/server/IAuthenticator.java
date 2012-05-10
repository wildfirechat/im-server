package org.dna.mqtt.moquette.server;

/**
 * username and password checker
 * 
 * @author andrea
 */
public interface IAuthenticator {

    boolean checkValid(String username, String password);
}
