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
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.ISubscriptionsStore.ClientTopicCouple;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;

import static io.moquette.spi.impl.subscriptions.Topic.asTopic;
import static org.junit.Assert.*;
import static org.assertj.core.api.Assertions.assertThat;

public class CTrieSubscriptionDirectoryTest {

    private CTrieSubscriptionDirectory sut;

    @Before
    public void setUp() {
        sut = new CTrieSubscriptionDirectory();
        MemoryStorageService memStore = new MemoryStorageService(null, null);
        ISessionsStore aSessionsStore = memStore.sessionsStore();
        sut.init(aSessionsStore);
    }

    @Test
    public void testAddOnSecondLayerWithEmptyTokenOnEmptyTree() {
        //Exercise
        sut.add(clientSubOnTopic("TempSensor1", "/"));

        //Verify
        final Optional<CNode> matchedNode = sut.lookup(asTopic("/"));
        assertTrue("Node on path / must be present", matchedNode.isPresent());
        //verify structure, only root INode and the first CNode should be present
        assertThat(this.sut.root.mainNode().subscriptions).isEmpty();
        assertThat(this.sut.root.mainNode().allChildren()).isNotEmpty();

        INode firstLayer = this.sut.root.mainNode().allChildren().get(0);
        assertThat(firstLayer.mainNode().subscriptions).isEmpty();
        assertThat(firstLayer.mainNode().allChildren()).isNotEmpty();

        INode secondLayer = firstLayer.mainNode().allChildren().get(0);
        assertThat(secondLayer.mainNode().subscriptions).isNotEmpty();
        assertThat(secondLayer.mainNode().allChildren()).isEmpty();
    }

    @Test
    public void testAddFirstLayerNodeOnEmptyTree() {
        //Exercise
        sut.add(clientSubOnTopic("TempSensor1", "/temp"));

        //Verify
        final Optional<CNode> matchedNode = sut.lookup(asTopic("/temp"));
        assertTrue("Node on path /temp must be present", matchedNode.isPresent());
        assertFalse(matchedNode.get().subscriptions.isEmpty());
    }

    @Test
    public void testLookup() {
        final ClientTopicCouple existingSubscription = clientSubOnTopic("TempSensor1", "/temp");
        sut.add(existingSubscription);

        //Exercise
        final Optional<CNode> matchedNode = sut.lookup(asTopic("/humidity"));

        //Verify
        assertFalse("Node on path /humidity can't be present", matchedNode.isPresent());
    }

    @Test
    public void testAddNewSubscriptionOnExistingNode() {
        final ClientTopicCouple existingSubscription = clientSubOnTopic("TempSensor1", "/temp");
        sut.add(existingSubscription);

        //Exercise
        final ClientTopicCouple newSubscription = clientSubOnTopic("TempSensor2", "/temp");
        sut.add(newSubscription);

        //Verify
        final Optional<CNode> matchedNode = sut.lookup(asTopic("/temp"));
        assertTrue("Node on path /temp must be present", matchedNode.isPresent());
        final Set<ClientTopicCouple> subscriptions = matchedNode.get().subscriptions;
        assertTrue(subscriptions.contains(newSubscription));
    }

    @Test
    public void testAddNewDeepNodes() {
        sut.add(clientSubOnTopic("TempSensorRM", "/italy/roma/temp"));
        sut.add(clientSubOnTopic("TempSensorFI", "/italy/firenze/temp"));
        sut.add(clientSubOnTopic("HumSensorFI", "/italy/roma/humidity"));
        final ClientTopicCouple happinessSensor = clientSubOnTopic("HappinessSensor", "/italy/happiness");
        sut.add(happinessSensor);

        //Verify
        final Optional<CNode> matchedNode = sut.lookup(asTopic("/italy/happiness"));
        assertTrue("Node on path /italy/happiness must be present", matchedNode.isPresent());
        final Set<ClientTopicCouple> subscriptions = matchedNode.get().subscriptions;
        assertTrue(subscriptions.contains(happinessSensor));
    }

    static ClientTopicCouple clientSubOnTopic(String clientID, String topicName) {
        return new ClientTopicCouple(clientID, asTopic(topicName));
    }

    @Test
    public void givenTreeWithSomeNodeWhenRemoveContainedSubscriptionThenNodeIsUpdated() {
        sut.add(clientSubOnTopic("TempSensor1", "/temp"));

        //Exercise
        sut.removeSubscription(asTopic("/temp"), "TempSensor1");

        //Verify
        final Optional<CNode> matchedNode = sut.lookup(asTopic("/temp"));
        assertFalse("Node on path /temp can't be present", matchedNode.isPresent());
    }

    @Test
    public void testMatchSubscriptionNoWildcards() {
        sut.add(clientSubOnTopic("TempSensor1", "/temp"));

        //Exercise
        final Set<ClientTopicCouple> matchingSubs = sut.recursiveMatch(asTopic("/temp"), sut.root);

        //Verify
        final ClientTopicCouple expectedMatchingsub = new ClientTopicCouple("TempSensor1", asTopic("/temp"));
        assertThat(matchingSubs).contains(expectedMatchingsub);
    }
}
