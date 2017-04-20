package io.moquette.persistence;

import io.moquette.spi.*;
import io.moquette.spi.IMessagesStore.StoredMessage;
import io.moquette.spi.ISubscriptionsStore.ClientTopicCouple;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.junit.Test;

import java.util.List;

import static io.moquette.spi.impl.subscriptions.Topic.asTopic;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.EXACTLY_ONCE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Defines all test that an implementation of a IMessageStore should satisfy.
 * */
public abstract class MessageStoreTCK {

    public static final String TEST_CLIENT = "TestClient";
    protected ISessionsStore sessionsStore;
    protected IMessagesStore messagesStore;

    @Test
    public void testDropMessagesInSessionDoesntCleanAnyRetainedStoredMessages() {
        final ClientSession session = sessionsStore.createNewSession(TEST_CLIENT, true);
        StoredMessage publishToStore = new StoredMessage("Hello".getBytes(), EXACTLY_ONCE, "/topic");
        publishToStore.setClientID(TEST_CLIENT);
        publishToStore.setRetained(true);
        messagesStore.storeRetained(new Topic("/topic"), publishToStore);

        // Exercise
        session.cleanSession();

        // Verify the message store for session is empty.
        StoredMessage storedPublish = messagesStore.searchMatching(new IMatchingCondition() {

            @Override
            public boolean match(Topic key) {
                return key.match(new Topic("/topic"));
            }
        }).iterator().next();
        assertNotNull("The stored retained message must be present after client's session drop", storedPublish);
    }

    @Test
    public void testStoreRetained() {
        StoredMessage msgStored = new StoredMessage("Hello".getBytes(), MqttQoS.AT_LEAST_ONCE, "/topic");
        msgStored.setClientID(TEST_CLIENT);

        messagesStore.storeRetained(asTopic("/topic"), msgStored);

        //Verify the message is in the store
        StoredMessage msgRetrieved = messagesStore.searchMatching(new IMatchingCondition() {

            @Override
            public boolean match(Topic key) {
                return key.match(new Topic("/topic"));
            }
        }).iterator().next();

        final ByteBuf payload = msgRetrieved.getPayload();
        byte[] content = new byte[payload.readableBytes()];
        payload.readBytes(content);
        assertEquals("Hello", new String(content));
    }

    @Test
    public void givenSubscriptionAlreadyStoredIsOverwrittenByAnotherWithSameTopic() {
        ClientSession session1 = sessionsStore.createNewSession("SESSION_ID_1", true);

        // Subscribe on /topic with QOSType.MOST_ONE
        Subscription oldSubscription = new Subscription(session1.clientID, new Topic("/topic"), AT_MOST_ONCE);
        session1.subscribe(oldSubscription);

        // Subscribe on /topic again that overrides the previous subscription.
        Subscription overridingSubscription = new Subscription(session1.clientID, new Topic("/topic"), EXACTLY_ONCE);
        session1.subscribe(overridingSubscription);

        // Verify
        final ISubscriptionsStore subscriptionsStore = sessionsStore.subscriptionStore();
        List<ClientTopicCouple> subscriptions = subscriptionsStore.listAllSubscriptions();
        assertEquals(1, subscriptions.size());
        Subscription sub = subscriptionsStore.getSubscription(subscriptions.get(0));
        assertEquals(overridingSubscription.getRequestedQos(), sub.getRequestedQos());
    }
}
