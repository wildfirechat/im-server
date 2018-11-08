package io.moquette.broker;

import io.moquette.spi.impl.subscriptions.Subscription;

import java.util.List;

public interface ISubscriptionsRepository {

    List<Subscription> listAllSubscriptions();

    void addNewSubscription(Subscription subscription);
}
