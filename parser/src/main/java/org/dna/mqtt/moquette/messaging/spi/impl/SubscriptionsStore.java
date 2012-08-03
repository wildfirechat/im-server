package org.dna.mqtt.moquette.messaging.spi.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a tree of published topics
 *
 * @author andrea
 */
public class SubscriptionsStore {

    private List<Subscription> subscriptions = new ArrayList<Subscription>();

    public void add(Subscription newSubscription) {
        if (subscriptions.contains(newSubscription)) {
            return;
        }
        subscriptions.add(newSubscription);
    }

    public List<Subscription> matches(String topic) {
        List<Subscription> m = new ArrayList<Subscription>();
        for (Subscription sub : subscriptions) {
            if (sub.match(topic)) {
                m.add(sub);
            }
        }
        return m;
    }

    public boolean contains(Subscription sub) {
        return subscriptions.contains(sub);
    }

    public int size() {
        return subscriptions.size();
    }
}
