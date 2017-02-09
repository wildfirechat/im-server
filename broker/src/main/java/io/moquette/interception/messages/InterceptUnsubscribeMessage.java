
package io.moquette.interception.messages;

/**
 * @author Wagner Macedo
 */
public class InterceptUnsubscribeMessage implements InterceptMessage {

    private final String topicFilter;
    private final String clientID;
    private final String username;

    public InterceptUnsubscribeMessage(String topicFilter, String clientID, String username) {
        this.topicFilter = topicFilter;
        this.clientID = clientID;
        this.username = username;
    }

    public String getTopicFilter() {
        return topicFilter;
    }

    public String getClientID() {
        return clientID;
    }

    public String getUsername() {
        return username;
    }
}
