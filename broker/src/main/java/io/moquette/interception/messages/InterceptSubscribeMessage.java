package io.moquette.interception.messages;

import io.moquette.parser.proto.messages.AbstractMessage;
import io.moquette.spi.impl.subscriptions.Subscription;

/**
 * @author Wagner Macedo
 */
public class InterceptSubscribeMessage implements InterceptMessage {
    private final Subscription subscription;
    private final String username;

    public InterceptSubscribeMessage(Subscription subscription, String username) {
        this.subscription = subscription;
        this.username = username;
    }

    public String getClientID() {
        return subscription.getClientId();
    }

    public AbstractMessage.QOSType getRequestedQos() {
        return subscription.getRequestedQos();
    }

    public String getTopicFilter() {
        return subscription.getTopicFilter();
    }

	public String getUsername() {
		return username;
	}
}
