package io.moquette.interception.messages;

import io.moquette.proto.messages.AbstractMessage;
import io.moquette.spi.impl.subscriptions.Subscription;

/**
 * @author Wagner Macedo
 */
public class InterceptSubscribeMessage {
    private final Subscription subscription;

    public InterceptSubscribeMessage(Subscription subscription) {
        this.subscription = subscription;
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
}
