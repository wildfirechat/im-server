
package io.moquette.connections;

/**
 * A class that represents a MQTT subscription.
 *
 * @author lbarrios
 *
 */
public class MqttSubscription {

    private final String requestedQos;
    private final String clientId;
    private final String topicFilter;
    private final boolean active;

    public MqttSubscription(String requestedQos, String clientId, String topicFilter, boolean active) {
        this.requestedQos = requestedQos;
        this.clientId = clientId;
        this.topicFilter = topicFilter;
        this.active = active;
    }

    public String getRequestedQos() {
        return requestedQos;
    }

    public String getClientId() {
        return clientId;
    }

    public String getTopicFilter() {
        return topicFilter;
    }

    public boolean isActive() {
        return active;
    }

}
