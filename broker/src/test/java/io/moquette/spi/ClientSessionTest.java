package io.moquette.spi;

import io.moquette.parser.proto.messages.AbstractMessage;
import io.moquette.spi.impl.MemoryStorageService;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.SubscriptionsStore;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Created by nedermail on 17/07/16.
 */
public class ClientSessionTest {

    ClientSession session1;
    ClientSession session2;
    ISessionsStore sessionsStore;
    SubscriptionsStore store;

    @Before
    public void setUp() {
        store = new SubscriptionsStore();
        MemoryStorageService storageService = new MemoryStorageService();
        storageService.initStore();
        this.sessionsStore = storageService.sessionsStore();
        store.init(sessionsStore);

        session1 = sessionsStore.createNewSession("SESSION_ID_1", true);
        session2 = sessionsStore.createNewSession("SESSION_ID_2", true);
    }

    @Test
    public void overridingSubscriptions() {
        // Subscribe on /topic with QOSType.MOST_ONE
        Subscription oldSubscription = new Subscription(session1.clientID, "/topic", AbstractMessage.QOSType.MOST_ONE);
        session1.subscribe(oldSubscription);
        store.add(oldSubscription.asClientTopicCouple());

        // Subscribe on /topic again that overrides the previous subscription.
        Subscription overrindingSubscription = new Subscription(session1.clientID, "/topic", AbstractMessage.QOSType.EXACTLY_ONCE);
        session1.subscribe(overrindingSubscription);
        store.add(overrindingSubscription.asClientTopicCouple());

        //Verify
        List<Subscription> subscriptions = store.matches("/topic");
        assertEquals(1, subscriptions.size());
        Subscription sub = subscriptions.get(0);
        assertEquals(overrindingSubscription.getRequestedQos(), sub.getRequestedQos());
    }
}
