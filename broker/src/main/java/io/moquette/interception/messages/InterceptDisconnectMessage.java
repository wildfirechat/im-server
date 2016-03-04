package io.moquette.interception.messages;

/**
 * @author Wagner Macedo
 */
public class InterceptDisconnectMessage {
    private final String clientID;
    private final String username;

    public InterceptDisconnectMessage(String clientID, String username) {
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
