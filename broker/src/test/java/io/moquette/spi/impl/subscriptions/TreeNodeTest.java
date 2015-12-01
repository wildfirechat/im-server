package io.moquette.spi.impl.subscriptions;

import static io.moquette.proto.messages.AbstractMessage.QOSType.*;
import static junit.framework.Assert.assertEquals;

import org.junit.Test;

import java.util.Set;

public class TreeNodeTest {

    public static final String CLIENT_ID = "FAKE_CLIENT_ID";

    @Test
    public void testAddSubscriptionOverwriteAnExistingWithDifferentCleanSessionFlag() throws Exception {
        TreeNode root = new TreeNode();
        Subscription existingSub = new Subscription(CLIENT_ID, "/topic", LEAST_ONE, false);
        root.addSubscription(existingSub);
        Set<Subscription> subs = root.findAllByClientID(CLIENT_ID);
        assertEquals(1, subs.size());

        //Exercise
        Subscription overwritingSub = new Subscription(CLIENT_ID, "/topic", LEAST_ONE, true);
        root.addSubscription(overwritingSub);

        //Verify
        //the second subscription has overwritten the first one
        subs = root.findAllByClientID(CLIENT_ID);
        assertEquals(1, subs.size());
        Subscription storedSub = subs.iterator().next();
        assertEquals(overwritingSub.isCleanSession(), storedSub.isCleanSession());
    }
}