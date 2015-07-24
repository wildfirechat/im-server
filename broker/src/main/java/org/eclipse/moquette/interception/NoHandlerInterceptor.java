package org.eclipse.moquette.interception;

import org.eclipse.moquette.proto.messages.ConnectMessage;
import org.eclipse.moquette.proto.messages.PublishMessage;
import org.eclipse.moquette.spi.impl.subscriptions.Subscription;

/**
 * @author Wagner Macedo
 */
public class NoHandlerInterceptor implements Interceptor {
    NoHandlerInterceptor() {
    }

    @Override
    public void notifyClientConnected(ConnectMessage msg) {
    }

    @Override
    public void notifyClientDisconnected(String clientID) {
    }

    @Override
    public void notifyTopicPublished(PublishMessage msg) {
    }

    @Override
    public void notifyTopicSubscribed(Subscription sub) {
    }

    @Override
    public void notifyTopicUnsubscribed(Subscription sub) {
    }
}
