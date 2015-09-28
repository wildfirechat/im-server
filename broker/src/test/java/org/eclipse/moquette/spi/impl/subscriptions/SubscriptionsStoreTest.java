/*
 * Copyright (c) 2012-2015 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package org.eclipse.moquette.spi.impl.subscriptions;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import org.eclipse.moquette.spi.impl.MemoryStorageService;
import org.eclipse.moquette.proto.messages.AbstractMessage;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author andrea
 */
public class SubscriptionsStoreTest {
    
    private SubscriptionsStore store;

    public SubscriptionsStoreTest() {
    }

    @Before
    public void setUp() throws IOException {
        store = new SubscriptionsStore();
        store.init(new MemoryStorageService());
    }
    
    @Test
    public void testParseTopic() throws ParseException {
        List<Token> tokens = store.parseTopic("finance/stock/ibm");
        assertEqualsSeq(asArray("finance", "stock", "ibm"), tokens);

        tokens = store.parseTopic("/finance/stock/ibm");
        assertEqualsSeq(asArray(Token.EMPTY, "finance", "stock", "ibm"), tokens);

        tokens = store.parseTopic("/");
        assertEqualsSeq(asArray(Token.EMPTY, Token.EMPTY), tokens);
    }

//    @Test(expected = ParseException.class)
//    public void testSplitTopicTwinsSlashAvoided() throws ParseException {
//        store.parseTopic("/finance//stock/ibm");
//    }

    @Test
    public void testParseTopicMultiValid() throws ParseException {
        List<Token> tokens = store.parseTopic("finance/stock/#");
        assertEqualsSeq(asArray("finance", "stock", Token.MULTI), tokens);

        tokens = store.parseTopic("#");
        assertEqualsSeq(asArray(Token.MULTI), tokens);
    }

    @Test(expected = ParseException.class)
    public void testParseTopicMultiInTheMiddleNotValid() throws ParseException {
        store.parseTopic("finance/#/closingprice");
    }

    @Test(expected = ParseException.class)
    public void testParseTopicMultiNotAfterSeparatorNotValid() throws ParseException {
        store.parseTopic("finance#");
    }

    @Test(expected = ParseException.class)
    public void testParseTopicMultiNotAlone() throws ParseException {
        store.parseTopic("/finance/#closingprice");
    }

    @Test
    public void testParseTopicSingleValid() throws ParseException {
        List<Token> tokens = store.parseTopic("finance/stock/+");
        assertEqualsSeq(asArray("finance", "stock", Token.SINGLE), tokens);

        tokens = store.parseTopic("+");
        assertEqualsSeq(asArray(Token.SINGLE), tokens);

        tokens = store.parseTopic("finance/+/ibm");
        assertEqualsSeq(asArray("finance", Token.SINGLE, "ibm"), tokens);
    }

    @Test(expected = ParseException.class)
    public void testParseTopicSingleNotAferSeparatorNotValid() throws ParseException {
        store.parseTopic("finance+");
    }

    @Test
    public void testMatchSimple() {
        Subscription slashSub = new Subscription("FAKE_CLI_ID_1", "/", AbstractMessage.QOSType.MOST_ONE, false);
        store.add(slashSub);
        assertTrue(store.matches("finance").isEmpty());
        
        Subscription slashFinanceSub = new Subscription("FAKE_CLI_ID_1", "/finance", AbstractMessage.QOSType.MOST_ONE, false);
        store.add(slashFinanceSub);
        assertTrue(store.matches("finance").isEmpty());
        
        assertTrue(store.matches("/finance").contains(slashFinanceSub));
        assertTrue(store.matches("/").contains(slashSub));
    }
    
    @Test
    public void testMatchSimpleMulti() {
        Subscription anySub = new Subscription("FAKE_CLI_ID_1", "#", AbstractMessage.QOSType.MOST_ONE, false);
        store.add(anySub);
        assertTrue(store.matches("finance").contains(anySub));
        
        Subscription financeAnySub = new Subscription("FAKE_CLI_ID_2", "finance/#", AbstractMessage.QOSType.MOST_ONE, false);
        store.add(financeAnySub);
        assertTrue(store.matches("finance").containsAll(Arrays.asList(financeAnySub, anySub)));
    }
    
    @Test
    public void testMatchingDeepMulti_one_layer() {
        Subscription anySub = new Subscription("FAKE_CLI_ID_1", "#", AbstractMessage.QOSType.MOST_ONE, false);
        Subscription financeAnySub = new Subscription("FAKE_CLI_ID_2", "finance/#", AbstractMessage.QOSType.MOST_ONE, false);
        store.add(anySub);
        store.add(financeAnySub);
        
        //Verify
        assertTrue(store.matches("finance/stock").containsAll(Arrays.asList(financeAnySub, anySub)));
        assertTrue(store.matches("finance/stock/ibm").containsAll(Arrays.asList(financeAnySub, anySub)));
    }
    
    
    @Test
    public void testMatchingDeepMulti_two_layer() {
        Subscription financeAnySub = new Subscription("FAKE_CLI_ID_1", "finance/stock/#", AbstractMessage.QOSType.MOST_ONE, false);
        store.add(financeAnySub);
        
        //Verify
        assertTrue(store.matches("finance/stock/ibm").contains(financeAnySub));
    }
    
    @Test
    public void testMatchSimpleSingle() {
        Subscription anySub = new Subscription("FAKE_CLI_ID_1", "+", AbstractMessage.QOSType.MOST_ONE, false);
        store.add(anySub);
        assertTrue(store.matches("finance").contains(anySub));
        
        Subscription financeOne = new Subscription("FAKE_CLI_ID_1", "finance/+", AbstractMessage.QOSType.MOST_ONE, false);
        store.add(financeOne);
        assertTrue(store.matches("finance/stock").contains(financeOne));
    }
    
    @Test
    public void testMatchManySingle() {
        Subscription manySub = new Subscription("FAKE_CLI_ID_1", "+/+", AbstractMessage.QOSType.MOST_ONE, false);
        store.add(manySub);
        
        //verify
        assertTrue(store.matches("/finance").contains(manySub));
    }
    
    
    @Test
    public void testMatchSlashSingle() {
        Subscription slashPlusSub = new Subscription("FAKE_CLI_ID_1", "/+", AbstractMessage.QOSType.MOST_ONE, false);
        store.add(slashPlusSub);
        Subscription anySub = new Subscription("FAKE_CLI_ID_1", "+", AbstractMessage.QOSType.MOST_ONE, false);
        store.add(anySub);
        
        //Verify
        assertEquals(1, store.matches("/finance").size());
        assertTrue(store.matches("/finance").contains(slashPlusSub));
        assertFalse(store.matches("/finance").contains(anySub));
    }
    
    
    @Test
    public void testMatchManyDeepSingle() {
        Subscription slashPlusSub = new Subscription("FAKE_CLI_ID_1", "/finance/+/ibm", AbstractMessage.QOSType.MOST_ONE, false);
        store.add(slashPlusSub);
        
        Subscription slashPlusDeepSub = new Subscription("FAKE_CLI_ID_2", "/+/stock/+", AbstractMessage.QOSType.MOST_ONE, false);
        store.add(slashPlusDeepSub);
        
        //Verify
        assertTrue(store.matches("/finance/stock/ibm").containsAll(Arrays.asList(slashPlusSub, slashPlusDeepSub)));
    }

    @Test
    public void testMatchSimpleMulti_allTheTree() {
        store.add(new Subscription("FAKE_CLI_ID_1", "#", AbstractMessage.QOSType.MOST_ONE, false));
        assertFalse(store.matches("finance").isEmpty());
        assertFalse(store.matches("finance/ibm").isEmpty());
    }

    @Test
    public void testMatchSimpleMulti_zeroLevel() {
        //check  MULTI in case of zero level match
        store.add(new Subscription("FAKE_CLI_ID_1", "finance/#", AbstractMessage.QOSType.MOST_ONE, false));
        assertFalse(store.matches("finance").isEmpty());
    }
    
    @Test
    public void rogerLightTopicMatches() {
        assertMatch("foo/bar", "foo/bar");
        assertMatch("foo/bar", "foo/bar");
        assertMatch("foo/+", "foo/bar");
        assertMatch("foo/+/baz", "foo/bar/baz");
        assertMatch("foo/+/#", "foo/bar/baz");
        assertMatch("#", "foo/bar/baz");

        assertNotMatch("foo/bar", "foo");
        assertNotMatch("foo/+", "foo/bar/baz");
        assertNotMatch("foo/+/baz", "foo/bar/bar");
        assertNotMatch("foo/+/#", "fo2/bar/baz");

        assertMatch("#", "/foo/bar");
        assertMatch("/#", "/foo/bar");
        assertNotMatch("/#", "foo/bar");

        assertMatch("foo//bar", "foo//bar");
        assertMatch("foo//+", "foo//bar");
        assertMatch("foo/+/+/baz", "foo///baz");
        assertMatch("foo/bar/+", "foo/bar/");
    }
    
    private void assertMatch(String subscription, String topic) {
        store = new SubscriptionsStore();
        store.init(new MemoryStorageService());
        Subscription sub = new Subscription("FAKE_CLI_ID_1", subscription, AbstractMessage.QOSType.MOST_ONE, false);
        store.add(sub);
        assertFalse(store.matches(topic).isEmpty());
    }
    
    private void assertNotMatch(String subscription, String topic) {
        store = new SubscriptionsStore();
        store.init(new MemoryStorageService());
        Subscription sub = new Subscription("FAKE_CLI_ID_1", subscription, AbstractMessage.QOSType.MOST_ONE, false);
        store.add(sub);
        assertTrue(store.matches(topic).isEmpty());
    }
    
    
    @Test
    public void testRemoveClientSubscriptions_existingClientID() {
        String cliendID = "FAKE_CLID_1";
        store.add(new Subscription(cliendID, "finance/#", AbstractMessage.QOSType.MOST_ONE, false));
        
        //Exercise
        store.removeForClient(cliendID);
        
        //Verify
        assertEquals(0, store.size());
    }
    
    @Test
    public void testRemoveClientSubscriptions_notexistingClientID() {
        String cliendID = "FAKE_CLID_1";
        store.add(new Subscription(cliendID, "finance/#", AbstractMessage.QOSType.MOST_ONE, false));
        
        //Exercise
        store.removeForClient("FAKE_CLID_2");
        
        //Verify
        assertEquals(1, store.size());
    }

    @Test
    public void testOverlappingSubscriptions() {
        Subscription genericSub = new Subscription("FAKE_CLI_ID_1", "a/+", AbstractMessage.QOSType.EXACTLY_ONCE, false);
        store.add(genericSub);
        Subscription specificSub = new Subscription("FAKE_CLI_ID_1", "a/b", AbstractMessage.QOSType.LEAST_ONE, false);
        store.add(specificSub);

        //Verify
        assertEquals(1, store.matches("a/b").size());
        //assertTrue(store.matches("/finance").contains(slashPlusSub));
        //assertFalse(store.matches("/finance").contains(anySub));
    }

    @Test
    public void testMatchTopics_simple() {
        assertTrue(SubscriptionsStore.matchTopics("/", "/"));
        assertTrue(SubscriptionsStore.matchTopics("/finance", "/finance"));
    }

    @Test
    public void testMatchTopics_multi() {
        assertTrue(SubscriptionsStore.matchTopics("finance", "#"));
        assertTrue(SubscriptionsStore.matchTopics("finance", "finance/#"));
        assertTrue(SubscriptionsStore.matchTopics("finance/stock", "finance/#"));
        assertTrue(SubscriptionsStore.matchTopics("finance/stock/ibm", "finance/#"));
    }


    @Test
    public void testMatchTopics_single() {
        assertTrue(SubscriptionsStore.matchTopics("finance", "+"));
        assertTrue(SubscriptionsStore.matchTopics("finance/stock", "finance/+"));
        assertFalse(SubscriptionsStore.matchTopics("finance", "finance/+"));
        assertTrue(SubscriptionsStore.matchTopics("/finance", "/+"));
        assertFalse(SubscriptionsStore.matchTopics("/finance", "+"));
        assertTrue(SubscriptionsStore.matchTopics("/finance", "+/+"));
        assertTrue(SubscriptionsStore.matchTopics("/finance/stock/ibm", "/finance/+/ibm"));
        assertTrue(SubscriptionsStore.matchTopics("/", "+/+"));
        assertTrue(SubscriptionsStore.matchTopics("sport/", "sport/+"));
        assertFalse(SubscriptionsStore.matchTopics("/finance/stock", "+"));
    }
    
    @Test
    public void rogerLightMatchTopics() {
        assertTrue(SubscriptionsStore.matchTopics("foo/bar", "foo/bar"));
        assertTrue(SubscriptionsStore.matchTopics("foo/bar", "foo/+"));
        assertTrue(SubscriptionsStore.matchTopics("foo/bar/baz", "foo/+/baz"));
        assertTrue(SubscriptionsStore.matchTopics("foo/bar/baz", "foo/+/#"));
        assertTrue(SubscriptionsStore.matchTopics("foo/bar/baz", "#"));
        
        assertFalse(SubscriptionsStore.matchTopics("foo", "foo/bar"));
        assertFalse(SubscriptionsStore.matchTopics("foo/bar/baz", "foo/+"));
        assertFalse(SubscriptionsStore.matchTopics("foo/bar/bar", "foo/+/baz"));
        assertFalse(SubscriptionsStore.matchTopics("fo2/bar/baz", "foo/+/#"));
        
        assertTrue(SubscriptionsStore.matchTopics("/foo/bar", "#"));
        assertTrue(SubscriptionsStore.matchTopics("/foo/bar", "/#"));
        assertFalse(SubscriptionsStore.matchTopics("foo/bar", "/#"));
        
        assertTrue(SubscriptionsStore.matchTopics("foo//bar", "foo//bar"));
        assertTrue(SubscriptionsStore.matchTopics("foo//bar", "foo//+"));
        assertTrue(SubscriptionsStore.matchTopics("foo///baz", "foo/+/+/baz"));
        assertTrue(SubscriptionsStore.matchTopics("foo/bar/", "foo/bar/+"));
    }
    
    @Test
    public void removeSubscription_withDifferentClients_subscribedSameTopic() {
        SubscriptionsStore aStore = new SubscriptionsStore();
        aStore.init(new MemoryStorageService());
        //subscribe a not active clientID1 to /topic
        Subscription slashSub = new Subscription("FAKE_CLI_ID_1", "/topic", AbstractMessage.QOSType.MOST_ONE, false);
        aStore.add(slashSub);
        aStore.deactivate(slashSub.getClientId());
        
        //subscribe an active clientID2 to /topic
        Subscription slashSub2 = new Subscription("FAKE_CLI_ID_2", "/topic", AbstractMessage.QOSType.MOST_ONE, false);
        aStore.add(slashSub2);
        
        //Exercise
        aStore.removeSubscription("/topic", slashSub2.getClientId());
        
        //Verify
        Subscription remainedSubscription = aStore.matches("/topic").get(0);
        assertEquals(slashSub.getClientId(), remainedSubscription.getClientId());
    }
    
    @Test
    public void overridingSubscriptions() {
        Subscription oldSubscription = new Subscription("FAKE_CLI_ID_1", "/topic", AbstractMessage.QOSType.MOST_ONE, false);
        store.add(oldSubscription);
        Subscription overrindingSubscription = new Subscription("FAKE_CLI_ID_1", "/topic", AbstractMessage.QOSType.EXACTLY_ONCE, false);
        store.add(overrindingSubscription);
        
        //Verify
        List<Subscription> subscriptions = store.matches("/topic");
        assertEquals(1, subscriptions.size());
        Subscription sub = subscriptions.get(0);
        assertEquals(overrindingSubscription.getRequestedQos(), sub.getRequestedQos());
    }

    /*
    Test for Issue #49
    * */
    @Test
    public void duplicatedSubscriptionsWithDifferentQos() {
        Subscription client2Sub = new Subscription("client2", "client/test/b", AbstractMessage.QOSType.MOST_ONE, true);
        store.add(client2Sub);
        Subscription client1SubQoS0 = new Subscription("client1", "client/test/b", AbstractMessage.QOSType.MOST_ONE, true);
        store.add(client1SubQoS0);

        Subscription client1SubQoS2 = new Subscription("client1", "client/test/b", AbstractMessage.QOSType.EXACTLY_ONCE, true);
        store.add(client1SubQoS2);

        System.out.println(store.dumpTree());

        //Verify
        List<Subscription> subscriptions = store.matches("client/test/b");
        assertTrue(subscriptions.contains(client1SubQoS2));
        assertTrue(subscriptions.contains(client2Sub));
        assertFalse(subscriptions.contains(client1SubQoS0)); //client1SubQoS2 should override client1SubQoS0
    }

    @Test
    public void testRecreatePath_emptyRoot() {
        TreeNode oldRoot = new TreeNode(null);
        final SubscriptionsStore.NodeCouple resp = store.recreatePath("/finance", oldRoot);

        //Verify
        assertNotNull(resp.root);
        assertNull(resp.root.m_token);
        assertEquals(1, resp.root.m_children.size());
        assertEquals(resp.createdNode, resp.root.m_children.get(0).m_children.get(0));
    }

    @Test
    public void testRecreatePath_1layer_tree() {
        TreeNode oldRoot = new TreeNode(null);
        final SubscriptionsStore.NodeCouple respFinance = store.recreatePath("/finance", oldRoot);
        final SubscriptionsStore.NodeCouple respPlus = store.recreatePath("/+", respFinance.root);

        //Verify
        assertNotNull(respPlus.root);
        assertNull(respPlus.root.m_token);
        assertEquals(1, respPlus.root.m_children.size());
        assertTrue(respPlus.root.m_children.get(0).m_children.contains(respPlus.createdNode));
        assertTrue(respPlus.root.m_children.get(0).m_children.contains(respFinance.createdNode));
    }

    private static Token[] asArray(Object... l) {
        Token[] tokens = new Token[l.length];
        for (int i = 0; i < l.length; i++) {
            Object o = l[i];
            if (o instanceof Token) {
                tokens[i] = (Token) o;
            } else {
                tokens[i] = new Token(o.toString());
            }
        }

        return tokens;
    }

    private void assertEqualsSeq(Token[] exptected, List<Token> result) {
        List<Token> expectedList = Arrays.asList(exptected);
        assertEquals(expectedList, result);
    }
}
