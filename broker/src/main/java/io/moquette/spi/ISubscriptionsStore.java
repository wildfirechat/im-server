package io.moquette.spi;

import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;

import java.util.Collection;
import java.util.List;

public interface ISubscriptionsStore {

    /**
     * Add a new subscription to the session
     *
     * @param newSubscription
     *            the subscription to add.
     */
    void addNewSubscription(Subscription newSubscription);

    /**
     * Removed a specific subscription
     *
     * @param topic
     *            the topic of the subscription.
     * @param clientID
     *            the session client.
     */
    void removeSubscription(Topic topic, String clientID);

    /**
     * Remove all the subscriptions of the session
     *
     * @param clientID
     *            the client ID
     */
    void wipeSubscriptions(String clientID);

    /**
     * Return all topic filters to recreate the subscription tree.
     */
    List<Subscription> listAllSubscriptions();

    /**
     * Load from storage all the subscriptions of the specified client.
     * */
    Collection<Subscription> listClientSubscriptions(String clientID);

    /**
     * @param subcription
     *            the subscription to reaload from storage.
     * @return the subscription stored by clientID and topicFilter, if any else null;
     */
    Subscription reload(Subscription subcription);
}
