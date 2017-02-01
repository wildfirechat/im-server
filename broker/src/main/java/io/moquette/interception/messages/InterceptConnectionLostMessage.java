
package io.moquette.interception.messages;

/**
 * @author Wagner Macedo
 */
public class InterceptConnectionLostMessage implements InterceptMessage {

    private final String clientID;
    private final String username;

    public InterceptConnectionLostMessage(String clientID, String username) {
        this.clientID = clientID;
        this.username = username;
    }

    public String getClientID() {
        return clientID;
    }

    public String getUsername() {
        return username;
    }
}
