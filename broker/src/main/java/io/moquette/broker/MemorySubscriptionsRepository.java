package io.moquette.broker;

import io.moquette.spi.impl.subscriptions.Subscription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MemorySubscriptionsRepository implements ISubscriptionsRepository {

    private final List<Subscription> subscriptions = new ArrayList<>();

    @Override
    public List<Subscription> listAllSubscriptions() {
        return Collections.unmodifiableList(subscriptions);
    }

    @Override
    public void addNewSubscription(Subscription subscription) {
        subscriptions.add(subscription);
    }
}
