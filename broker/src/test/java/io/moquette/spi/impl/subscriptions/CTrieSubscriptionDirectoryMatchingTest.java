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
 */
package io.moquette.spi.impl.subscriptions;

import io.moquette.persistence.MemoryStorageService;
import io.moquette.spi.ClientSession;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.ISubscriptionsStore;
import io.moquette.spi.impl.SessionsRepository;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;

import static io.moquette.spi.impl.subscriptions.CTrieSubscriptionDirectoryTest.clientSubOnTopic;
import static io.moquette.spi.impl.subscriptions.Topic.asTopic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CTrieSubscriptionDirectoryMatchingTest {

    private CTrieSubscriptionDirectory sut;
    private ISubscriptionsStore subscriptionsStore;
    private SessionsRepository sessionsRepository;

    @Before
    public void setUp() {
        sut = new CTrieSubscriptionDirectory();

        MemoryStorageService storageService = new MemoryStorageService(null, null);
        ISessionsStore sessionsStore = storageService.sessionsStore();
        this.subscriptionsStore = sessionsStore.subscriptionStore();
        this.sessionsRepository = new SessionsRepository(sessionsStore, null);
        sut.init(this.sessionsRepository);
    }

    @Test
    public void testMatchSimple() {
        Subscription slashSub = clientSubOnTopic("TempSensor1", "/");
        sut.add(slashSub);
        assertThat(sut.recursiveMatch(asTopic("finance"), sut.root)).isEmpty();

        Subscription slashFinanceSub = clientSubOnTopic("TempSensor1", "/finance");
        sut.add(slashFinanceSub);
        assertThat(sut.recursiveMatch(asTopic("finance"), sut.root)).isEmpty();

        assertThat(sut.recursiveMatch(asTopic("/finance"), sut.root)).contains(slashFinanceSub);
        assertThat(sut.recursiveMatch(asTopic("/"), sut.root)).contains(slashSub);
    }

    @Test
    public void testMatchSimpleMulti() {
        Subscription anySub = clientSubOnTopic("TempSensor1", "#");
        sut.add(anySub);
        assertThat(sut.recursiveMatch(asTopic("finance"), sut.root)).contains(anySub);

        Subscription financeAnySub = clientSubOnTopic("TempSensor1", "finance/#");
        sut.add(financeAnySub);
        assertThat(sut.recursiveMatch(asTopic("finance"), sut.root)).containsExactlyInAnyOrder(financeAnySub, anySub);
    }

    @Test
    public void testMatchingDeepMulti_one_layer() {
        Subscription anySub = clientSubOnTopic("AllSensor1", "#");
        Subscription financeAnySub = clientSubOnTopic("FinanceSensor", "finance/#");
        sut.add(anySub);
        sut.add(financeAnySub);

        // Verify
        assertThat(sut.recursiveMatch(asTopic("finance/stock"), sut.root)).containsExactlyInAnyOrder(financeAnySub, anySub);
        assertThat(sut.recursiveMatch(asTopic("finance/stock/ibm"), sut.root)).containsExactlyInAnyOrder(financeAnySub, anySub);

        System.out.println(sut.dumpTree());
    }

    @Test
    public void testMatchingDeepMulti_two_layer() {
        Subscription financeAnySub = clientSubOnTopic("FinanceSensor", "finance/stock/#");
        sut.add(financeAnySub);

        // Verify
        assertThat(sut.recursiveMatch(asTopic("finance/stock/ibm"), sut.root)).containsExactly(financeAnySub);
    }

    @Test
    public void testMatchSimpleSingle() {
        Subscription anySub = clientSubOnTopic("AnySensor", "+");
        sut.add(anySub);
        assertThat(sut.recursiveMatch(asTopic("finance"), sut.root)).containsExactly(anySub);

        Subscription financeOne = clientSubOnTopic("AnySensor", "finance/+");
        sut.add(financeOne);
        assertThat(sut.recursiveMatch(asTopic("finance/stock"), sut.root)).containsExactly(financeOne);
    }

    @Test
    public void testMatchManySingle() {
        Subscription manySub = clientSubOnTopic("AnySensor", "+/+");
        sut.add(manySub);

        // verify
        assertThat(sut.recursiveMatch(asTopic("/finance"), sut.root)).contains(manySub);
    }

    @Test
    public void testMatchSlashSingle() {
        Subscription slashPlusSub = clientSubOnTopic("AnySensor", "/+");
        sut.add(slashPlusSub);
        Subscription anySub = clientSubOnTopic("AnySensor", "+");
        sut.add(anySub);

        // Verify
        assertThat(sut.recursiveMatch(asTopic("/finance"), sut.root)).containsOnly(slashPlusSub);
        assertThat(sut.recursiveMatch(asTopic("/finance"), sut.root)).doesNotContain(anySub);
    }

    @Test
    public void testMatchManyDeepSingle() {
        Subscription slashPlusSub = clientSubOnTopic("FinanceSensor1", "/finance/+/ibm");
        sut.add(slashPlusSub);
        Subscription slashPlusDeepSub = clientSubOnTopic("FinanceSensor2", "/+/stock/+");
        sut.add(slashPlusDeepSub);

        // Verify
        assertThat(sut.recursiveMatch(asTopic("/finance/stock/ibm"), sut.root))
            .containsExactlyInAnyOrder(slashPlusSub, slashPlusDeepSub);
    }

    @Test
    public void testMatchSimpleMulti_allTheTree() {
        Subscription sub = clientSubOnTopic("AnySensor1", "#");
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
        SessionsRepository sessionsRepository = new SessionsRepository(aSessionsStore, null);
        sut.init(sessionsRepository);

        Subscription sub = clientSubOnTopic("AnySensor1", s);
        sut.add(sub);

        assertThat(sut.recursiveMatch(asTopic(t), sut.root)).isNotEmpty();
    }

    private void assertNotMatch(String subscription, String topic) {
        sut = new CTrieSubscriptionDirectory();
        MemoryStorageService memStore = new MemoryStorageService(null, null);
        ISessionsStore aSessionsStore = memStore.sessionsStore();
        SessionsRepository sessionsRepository = new SessionsRepository(aSessionsStore, null);
        sut.init(sessionsRepository);

        Subscription sub = clientSubOnTopic("AnySensor1", subscription);
        sut.add(sub);

        assertThat(sut.recursiveMatch(asTopic(topic), sut.root)).isEmpty();
    }

    @Test
    public void testOverlappingSubscriptions() {
        Subscription genericSub = new Subscription("Sensor1", asTopic("a/+"), MqttQoS.AT_MOST_ONCE);
        this.subscriptionsStore.addNewSubscription(genericSub);
        final ClientSession sensorSession = this.sessionsRepository.createNewSession("Sensor1", false);
        sensorSession.subscribe(genericSub);
        sut.add(genericSub);

        Subscription specificSub = new Subscription("Sensor1", asTopic("a/b"), MqttQoS.AT_MOST_ONCE);
        sensorSession.subscribe(specificSub);
        this.subscriptionsStore.addNewSubscription(specificSub);
        sut.add(specificSub);

        //Exercise
        final Set<Subscription> matchingForSpecific = sut.match(asTopic("a/b"));

        // Verify
        assertThat(matchingForSpecific.size()).isEqualTo(1);
    }

    @Test
    public void removeSubscription_withDifferentClients_subscribedSameTopic() {
        Subscription slashSub = clientSubOnTopic("Sensor1", "/topic");
        sut.add(slashSub);
        Subscription slashSub2 = clientSubOnTopic("Sensor2", "/topic");
        sut.add(slashSub2);

        // Exercise
        sut.removeSubscription(asTopic("/topic"), slashSub2.clientId);

        // Verify
        Subscription remainedSubscription = sut.recursiveMatch(asTopic("/topic"), sut.root).iterator().next();
        assertThat(remainedSubscription.clientId).isEqualTo(slashSub.clientId);
        assertEquals(slashSub.clientId, remainedSubscription.clientId);
    }

    @Test
    public void removeSubscription_sameClients_subscribedSameTopic() {
        Subscription slashSub = clientSubOnTopic("Sensor1", "/topic");
        sut.add(slashSub);

        // Exercise
        sut.removeSubscription(asTopic("/topic"), slashSub.clientId);

        // Verify
        final Set<Subscription> matchingSubscriptions = sut.recursiveMatch(asTopic("/topic"), sut.root);
        assertThat(matchingSubscriptions).isEmpty();
    }

    /*
     * Test for Issue #49
     */
    @Test
    public void duplicatedSubscriptionsWithDifferentQos() {
        ClientSession session2 = this.sessionsRepository.createNewSession("client2", true);
        Subscription client2Sub = new Subscription("client2", asTopic("client/test/b"), MqttQoS.AT_MOST_ONCE);
        session2.subscribe(client2Sub);
        this.sut.add(client2Sub);
        ClientSession session1 = this.sessionsRepository.createNewSession("client1", true);
        Subscription client1SubQoS0 = new Subscription("client1", asTopic("client/test/b"), MqttQoS.AT_MOST_ONCE);
        session1.subscribe(client1SubQoS0);
        this.sut.add(client1SubQoS0);

        Subscription client1SubQoS2 = new Subscription("client1", asTopic("client/test/b"), MqttQoS.EXACTLY_ONCE);
        session1.subscribe(client1SubQoS2);
        this.sut.add(client1SubQoS2);

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
