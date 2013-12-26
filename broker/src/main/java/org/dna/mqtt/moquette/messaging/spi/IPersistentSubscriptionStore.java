package org.dna.mqtt.moquette.messaging.spi;

import java.util.List;
import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.Subscription;

/**
 * Store used to handle the persistence of the subscriptions tree.
 *
 * @author andrea
 */
public interface IPersistentSubscriptionStore {

    void addNewSubscription(Subscription newSubscription, String clientID);

    void removeAllSubscriptions(String clientID);

    List<Subscription> retrieveAllSubscriptions();
}
