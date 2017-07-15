/*
 * Copyright (c) 2012-2017 The original author or authors
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
 */package io.moquette.spi.impl.subscriptions;

import io.moquette.persistence.MemoryStorageService;
import io.moquette.spi.ClientSession;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.ISubscriptionsStore;
import io.moquette.spi.ISubscriptionsStore.ClientTopicCouple;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static io.moquette.spi.impl.subscriptions.CTrieSubscriptionDirectoryTest.clientSubOnTopic;
import static io.moquette.spi.impl.subscriptions.Topic.asTopic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CTrieSubscriptionDirectoryMatchingTest {

    private CTrieSubscriptionDirectory sut;
    private ISessionsStore sessionsStore;
    private ISubscriptionsStore subscriptionsStore;
    private MemoryStorageService storageService;

    @Before
    public void setUp() {
        sut = new CTrieSubscriptionDirectory();

        this.storageService = new MemoryStorageService(null, null);
        this.sessionsStore = storageService.sessionsStore();
        sut.init(sessionsStore);

        this.subscriptionsStore = this.sessionsStore.subscriptionStore();
    }

    @Test
    public void testMatchSimple() {
        ClientTopicCouple slashSub = clientSubOnTopic("TempSensor1", "/");
        sut.add(slashSub);
        assertThat(sut.recursiveMatch(asTopic("finance"), sut.root)).isEmpty();

        ClientTopicCouple slashFinanceSub = clientSubOnTopic("TempSensor1", "/finance");
        sut.add(slashFinanceSub);
        assertThat(sut.recursiveMatch(asTopic("finance"), sut.root)).isEmpty();

        assertThat(sut.recursiveMatch(asTopic("/finance"), sut.root)).contains(slashFinanceSub);
        assertThat(sut.recursiveMatch(asTopic("/"), sut.root)).contains(slashSub);
    }

    @Test
    public void testMatchSimpleMulti() {
        ClientTopicCouple anySub = clientSubOnTopic("TempSensor1", "#");
        sut.add(anySub);
        assertThat(sut.recursiveMatch(asTopic("finance"), sut.root)).contains(anySub);

        ClientTopicCouple financeAnySub = clientSubOnTopic("TempSensor1", "finance/#");
        sut.add(financeAnySub);
        assertThat(sut.recursiveMatch(asTopic("finance"), sut.root)).containsExactlyInAnyOrder(financeAnySub, anySub);
    }

    @Test
    public void testMatchingDeepMulti_one_layer() {
        ClientTopicCouple anySub = clientSubOnTopic("AllSensor1", "#");
        ClientTopicCouple financeAnySub = clientSubOnTopic("FinanceSensor", "finance/#");
        sut.add(anySub);
        sut.add(financeAnySub);

        // Verify
        assertThat(sut.recursiveMatch(asTopic("finance/stock"), sut.root)).containsExactlyInAnyOrder(financeAnySub, anySub);
        assertThat(sut.recursiveMatch(asTopic("finance/stock/ibm"), sut.root)).containsExactlyInAnyOrder(financeAnySub, anySub);

        System.out.println(sut.dumpTree());
    }

    @Test
    public void testMatchingDeepMulti_two_layer() {
        ClientTopicCouple financeAnySub = clientSubOnTopic("FinanceSensor", "finance/stock/#");
        sut.add(financeAnySub);

        // Verify
        assertThat(sut.recursiveMatch(asTopic("finance/stock/ibm"), sut.root)).containsExactly(financeAnySub);
    }

    @Test
    public void testMatchSimpleSingle() {
        ClientTopicCouple anySub = clientSubOnTopic("AnySensor", "+");
        sut.add(anySub);
        assertThat(sut.recursiveMatch(asTopic("finance"), sut.root)).containsExactly(anySub);

        ClientTopicCouple financeOne = clientSubOnTopic("AnySensor", "finance/+");
        sut.add(financeOne);
        assertThat(sut.recursiveMatch(asTopic("finance/stock"), sut.root)).containsExactly(financeOne);
    }

    @Test
    public void testMatchManySingle() {
        ClientTopicCouple manySub = clientSubOnTopic("AnySensor", "+/+");
        sut.add(manySub);

        // verify
        assertThat(sut.recursiveMatch(asTopic("/finance"), sut.root)).contains(manySub);
    }

    @Test
    public void testMatchSlashSingle() {
        ClientTopicCouple slashPlusSub = clientSubOnTopic("AnySensor", "/+");
        sut.add(slashPlusSub);
        ClientTopicCouple anySub = clientSubOnTopic("AnySensor", "+");
        sut.add(anySub);

        // Verify
        assertThat(sut.recursiveMatch(asTopic("/finance"), sut.root)).containsOnly(slashPlusSub);
        assertThat(sut.recursiveMatch(asTopic("/finance"), sut.root)).doesNotContain(anySub);
    }

    @Test
    public void testMatchManyDeepSingle() {
        ClientTopicCouple slashPlusSub = clientSubOnTopic("FinanceSensor1", "/finance/+/ibm");
        sut.add(slashPlusSub);
        ClientTopicCouple slashPlusDeepSub = clientSubOnTopic("FinanceSensor2", "/+/stock/+");
        sut.add(slashPlusDeepSub);

        // Verify
        assertThat(sut.recursiveMatch(asTopic("/finance/stock/ibm"), sut.root))
            .containsExactlyInAnyOrder(slashPlusSub, slashPlusDeepSub);
    }

    @Test
    public void testMatchSimpleMulti_allTheTree() {
        ClientTopicCouple sub = clientSubOnTopic("AnySensor1", "#");
        sut.add(sub);

        assertThat(sut.recursiveMatch(asTopic("finance"), sut.root)).isNotEmpty();
        assertThat(sut.recursiveMatch(asTopic("finance/ibm"), sut.root)).isNotEmpty();
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

    private void assertMatch(String s, String t) {
        sut = new CTrieSubscriptionDirectory();
        MemoryStorageService memStore = new MemoryStorageService(null, null);
        ISessionsStore aSessionsStore = memStore.sessionsStore();
        sut.init(aSessionsStore);

        ClientTopicCouple sub = clientSubOnTopic("AnySensor1", s);
        sut.add(sub);

        assertThat(sut.recursiveMatch(asTopic(t), sut.root)).isNotEmpty();
    }

    private void assertNotMatch(String subscription, String topic) {
        sut = new CTrieSubscriptionDirectory();
        MemoryStorageService memStore = new MemoryStorageService(null, null);
        ISessionsStore aSessionsStore = memStore.sessionsStore();
        sut.init(aSessionsStore);

        ClientTopicCouple sub = clientSubOnTopic("AnySensor1", subscription);
        sut.add(sub);

        assertThat(sut.recursiveMatch(asTopic(topic), sut.root)).isEmpty();
    }

    @Test
    public void testOverlappingSubscriptions() {
        Subscription sub = new Subscription("Sensor1", asTopic("a/+"), MqttQoS.AT_MOST_ONCE);
        ClientTopicCouple genericSub = sub.asClientTopicCouple();
        this.subscriptionsStore.addNewSubscription(sub);
        storageService.sessionsStore().createNewSession("Sensor1", false);
        sut.add(genericSub);

        Subscription sub2 = new Subscription("Sensor1", asTopic("a/b"), MqttQoS.AT_MOST_ONCE);
        ClientTopicCouple specificSub = sub2.asClientTopicCouple();
        this.subscriptionsStore.addNewSubscription(sub2);
        sut.add(specificSub);

        // Verify
        assertThat(sut.match(asTopic("a/b")).size()).isEqualTo(1);
    }

    @Test
    public void removeSubscription_withDifferentClients_subscribedSameTopic() {
        ClientTopicCouple slashSub = clientSubOnTopic("Sensor1", "/topic");
        sut.add(slashSub);
        ClientTopicCouple slashSub2 = clientSubOnTopic("Sensor2", "/topic");
        sut.add(slashSub2);

        // Exercise
        sut.removeSubscription(asTopic("/topic"), slashSub2.clientID);

        // Verify
        ClientTopicCouple remainedSubscription = sut.recursiveMatch(asTopic("/topic"), sut.root).iterator().next();
        assertThat(remainedSubscription.clientID).isEqualTo(slashSub.clientID);
        assertEquals(slashSub.clientID, remainedSubscription.clientID);
    }

    @Test
    public void removeSubscription_sameClients_subscribedSameTopic() {
        ClientTopicCouple slashSub = clientSubOnTopic("Sensor1", "/topic");
        sut.add(slashSub);

        // Exercise
        sut.removeSubscription(asTopic("/topic"), slashSub.clientID);

        // Verify
        final Set<ClientTopicCouple> matchingSubscriptions = sut.recursiveMatch(asTopic("/topic"), sut.root);
        assertThat(matchingSubscriptions).isEmpty();
    }

    /*
     * Test for Issue #49
     */
    @Test
    public void duplicatedSubscriptionsWithDifferentQos() {
        ClientSession session2 = sessionsStore.createNewSession("client2", true);
        Subscription client2Sub = new Subscription("client2", asTopic("client/test/b"), MqttQoS.AT_MOST_ONCE);
        session2.subscribe(client2Sub);
        this.sut.add(client2Sub.asClientTopicCouple());
        ClientSession session1 = sessionsStore.createNewSession("client1", true);
        Subscription client1SubQoS0 = new Subscription("client1", asTopic("client/test/b"), MqttQoS.AT_MOST_ONCE);
        session1.subscribe(client1SubQoS0);
        this.sut.add(client1SubQoS0.asClientTopicCouple());

        Subscription client1SubQoS2 = new Subscription("client1", asTopic("client/test/b"), MqttQoS.EXACTLY_ONCE);
        session1.subscribe(client1SubQoS2);
        this.sut.add(client1SubQoS2.asClientTopicCouple());

        // Verify
        Set<Subscription> subscriptions = this.sut.match(asTopic("client/test/b"));
        assertThat(subscriptions).contains(client1SubQoS2);
        assertThat(subscriptions).contains(client2Sub);

        final Optional<Subscription> matchingClient1Sub = subscriptions
            .stream()
            .filter(s -> s.equals(client1SubQoS0))
            .findFirst();
        assertTrue(matchingClient1Sub.isPresent());
        Subscription client1Sub = matchingClient1Sub.get();

        assertThat(client1SubQoS0.getRequestedQos()).isNotEqualTo(client1Sub.getRequestedQos());

        // client1SubQoS2 should override client1SubQoS0
        assertThat(client1Sub.getRequestedQos()).isEqualTo(client1SubQoS2.getRequestedQos());
    }
}
