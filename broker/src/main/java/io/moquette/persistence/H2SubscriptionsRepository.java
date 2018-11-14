package io.moquette.persistence;

import io.moquette.broker.ISubscriptionsRepository;
import io.moquette.broker.subscriptions.Subscription;
import org.h2.mvstore.Cursor;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class H2SubscriptionsRepository implements ISubscriptionsRepository {

    private static final Logger LOG = LoggerFactory.getLogger(H2SubscriptionsRepository.class);
    private static final String SUBSCRIPTIONS_MAP = "subscriptions";

    private MVMap<String, Subscription> subscriptions;

    H2SubscriptionsRepository(MVStore mvStore) {
        this.subscriptions = mvStore.openMap(SUBSCRIPTIONS_MAP);
    }

    @Override
    public List<Subscription> listAllSubscriptions() {
        LOG.debug("Retrieving existing subscriptions");

        List<Subscription> results = new ArrayList<>();
        Cursor<String, Subscription> mapCursor = subscriptions.cursor(null);
        while (mapCursor.hasNext()) {
            String subscriptionStr = mapCursor.next();
            results.add(mapCursor.getValue());
        }
        LOG.debug("Loaded {} subscriptions", results.size());
        return results;
    }

    @Override
    public void addNewSubscription(Subscription subscription) {
        subscriptions.put(subscription.getTopicFilter() + "-" + subscription.getClientId(), subscription);
    }

    @Override
    public void removeSubscription(String topicFilter, String clientID) {
        subscriptions.remove(topicFilter + "-" + clientID);
    }
}
