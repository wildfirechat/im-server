package io.moquette.interception.messages;

/**
 * @author Wagner Macedo
 */
public class InterceptDisconnectMessage {
    private final String clientID;

    public InterceptDisconnectMessage(String clientID) {
        this.clientID = clientID;
    }

    public String getClientID() {
        return clientID;
    }
}
