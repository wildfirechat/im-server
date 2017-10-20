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
import io.moquette.spi.impl.SessionsRepository;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;
import java.util.Set;

import static io.moquette.spi.impl.subscriptions.Topic.asTopic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CTrieSubscriptionDirectoryTest {

    private CTrieSubscriptionDirectory sut;

    @Before
    public void setUp() {
        sut = new CTrieSubscriptionDirectory();
        MemoryStorageService memStore = new MemoryStorageService(null, null);
        ISessionsStore aSessionsStore = memStore.sessionsStore();
        SessionsRepository sessionsRepository = new SessionsRepository(aSessionsStore, null);
        sut.init(sessionsRepository);
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
        final Subscription existingSubscription = clientSubOnTopic("TempSensor1", "/temp");
        sut.add(existingSubscription);

        //Exercise
        final Optional<CNode> matchedNode = sut.lookup(asTopic("/humidity"));

        //Verify
        assertFalse("Node on path /humidity can't be present", matchedNode.isPresent());
    }

    @Test
    public void testAddNewSubscriptionOnExistingNode() {
        final Subscription existingSubscription = clientSubOnTopic("TempSensor1", "/temp");
        sut.add(existingSubscription);

        //Exercise
        final Subscription newSubscription = clientSubOnTopic("TempSensor2", "/temp");
        sut.add(newSubscription);

        //Verify
        final Optional<CNode> matchedNode = sut.lookup(asTopic("/temp"));
        assertTrue("Node on path /temp must be present", matchedNode.isPresent());
        final Set<Subscription> subscriptions = matchedNode.get().subscriptions;
        assertTrue(subscriptions.contains(newSubscription));
    }

    @Test
    public void testAddNewDeepNodes() {
        sut.add(clientSubOnTopic("TempSensorRM", "/italy/roma/temp"));
        sut.add(clientSubOnTopic("TempSensorFI", "/italy/firenze/temp"));
        sut.add(clientSubOnTopic("HumSensorFI", "/italy/roma/humidity"));
        final Subscription happinessSensor = clientSubOnTopic("HappinessSensor", "/italy/happiness");
        sut.add(happinessSensor);

        //Verify
        final Optional<CNode> matchedNode = sut.lookup(asTopic("/italy/happiness"));
        assertTrue("Node on path /italy/happiness must be present", matchedNode.isPresent());
        final Set<Subscription> subscriptions = matchedNode.get().subscriptions;
        assertTrue(subscriptions.contains(happinessSensor));
    }

    static Subscription clientSubOnTopic(String clientID, String topicName) {
        return new Subscription(clientID, asTopic(topicName), null);
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
    public void givenTreeWithSomeNodeUnsubscribeAndResubscribeCleanTomb() {
        sut.add(clientSubOnTopic("TempSensor1", "test"));
        sut.removeSubscription(asTopic("test"), "TempSensor1");

        sut.add(clientSubOnTopic("TempSensor1", "test"));
        assertTrue(sut.root.mainNode().allChildren().size() == 1);  // looking to see if TNode is cleaned up
    }

    @Test
    public void givenTreeWithSomeNodeWhenRemoveMultipleTimes() {
        sut.add(clientSubOnTopic("TempSensor1", "test"));
        
        // make sure no TNode exceptions 
        sut.removeSubscription(asTopic("test"), "TempSensor1");
        sut.removeSubscription(asTopic("test"), "TempSensor1");
        sut.removeSubscription(asTopic("test"), "TempSensor1");
        sut.removeSubscription(asTopic("test"), "TempSensor1");

        //Verify
        final Optional<CNode> matchedNode = sut.lookup(asTopic("/temp"));
        assertFalse("Node on path /temp can't be present", matchedNode.isPresent());
    }

    @Test
    public void givenTreeWithSomeDeepNodeWhenRemoveMultipleTimes() {
        sut.add(clientSubOnTopic("TempSensor1", "/test/me/1/2/3"));

        // make sure no TNode exceptions
        sut.removeSubscription(asTopic("/test/me/1/2/3"), "TempSensor1");
        sut.removeSubscription(asTopic("/test/me/1/2/3"), "TempSensor1");
        sut.removeSubscription(asTopic("/test/me/1/2/3"), "TempSensor1");

        //Verify
        final Optional<CNode> matchedNode = sut.lookup(asTopic("/temp"));
        assertFalse("Node on path /temp can't be present", matchedNode.isPresent());
    }

    @Test
    public void givenTreeWithSomeNodeHierarchWhenRemoveContainedSubscriptionThenNodeIsUpdated() {
        sut.add(clientSubOnTopic("TempSensor1", "/temp/1"));
        sut.add(clientSubOnTopic("TempSensor1", "/temp/2"));

        //Exercise
        sut.removeSubscription(asTopic("/temp/1"), "TempSensor1");

        sut.removeSubscription(asTopic("/temp/1"), "TempSensor1");
        final Set<Subscription> matchingSubs = sut.recursiveMatch(asTopic("/temp/2"), sut.root);

        //Verify
        final Subscription expectedMatchingsub = new Subscription("TempSensor1", asTopic("/temp/2"));
        assertThat(matchingSubs).contains(expectedMatchingsub);
    }

    @Test
    public void givenTreeWithSomeNodeHierarchWhenRemoveContainedSubscriptionSmallerThenNodeIsNotUpdated() {
        sut.add(clientSubOnTopic("TempSensor1", "/temp/1"));
        sut.add(clientSubOnTopic("TempSensor1", "/temp/2"));

        //Exercise

        sut.removeSubscription(asTopic("/temp"), "TempSensor1");

        final Set<Subscription> matchingSubs1 = sut.recursiveMatch(asTopic("/temp/1"), sut.root);
        final Set<Subscription> matchingSubs2 = sut.recursiveMatch(asTopic("/temp/2"), sut.root);

        //Verify

        // not clear to me, but I believe /temp unsubscribe should not unsub you from downstream /temp/1 or /temp/2
        
        final Subscription expectedMatchingsub1 = new Subscription("TempSensor1", asTopic("/temp/1"));
        assertThat(matchingSubs1).contains(expectedMatchingsub1);
        final Subscription expectedMatchingsub2 = new Subscription("TempSensor1", asTopic("/temp/2"));
        assertThat(matchingSubs2).contains(expectedMatchingsub2);
    }


    @Test
    public void givenTreeWithDeepNodeWhenRemoveContainedSubscriptionThenNodeIsUpdated() {
        sut.add(clientSubOnTopic("TempSensor1", "/bah/bin/bash"));

        sut.removeSubscription(asTopic("/bah/bin/bash"),"TempSensor1");

        //Verify
        final Optional<CNode> matchedNode = sut.lookup(asTopic("/bah/bin/bash"));
        assertFalse("Node on path /temp can't be present", matchedNode.isPresent());
    }


    @Test
    public void testMatchSubscriptionNoWildcards() {
        sut.add(clientSubOnTopic("TempSensor1", "/temp"));

        //Exercise
        final Set<Subscription> matchingSubs = sut.recursiveMatch(asTopic("/temp"), sut.root);

        //Verify
        final Subscription expectedMatchingsub = new Subscription("TempSensor1", asTopic("/temp"));
        assertThat(matchingSubs).contains(expectedMatchingsub);
    }

    @Test
    public void testRemovalInnerTopicOffRootSameClient() {
        sut.add(clientSubOnTopic("TempSensor1", "temp"));
        sut.add(clientSubOnTopic("TempSensor1", "temp/1"));
        
        //Exercise
        final Set<Subscription> matchingSubs1 = sut.recursiveMatch(asTopic("temp"), sut.root);
        final Set<Subscription> matchingSubs2 = sut.recursiveMatch(asTopic("temp/1"), sut.root);

        //Verify
        final Subscription expectedMatchingsub1 = new Subscription("TempSensor1", asTopic("temp"));
        final Subscription expectedMatchingsub2 = new Subscription("TempSensor1", asTopic("temp/1"));

        assertThat(matchingSubs1).contains(expectedMatchingsub1);
        assertThat(matchingSubs2).contains(expectedMatchingsub2);

        sut.removeSubscription(asTopic("temp"),"TempSensor1");

        //Exercise
        final Set<Subscription> matchingSubs3 = sut.recursiveMatch(asTopic("temp"), sut.root);
        final Set<Subscription> matchingSubs4 = sut.recursiveMatch(asTopic("temp/1"), sut.root);

        assertThat(matchingSubs3).doesNotContain(expectedMatchingsub1);
        assertThat(matchingSubs4).contains(expectedMatchingsub2);
    }

    @Test
    public void testRemovalInnerTopicOffRootDiffClient() {
        sut.add(clientSubOnTopic("TempSensor1", "temp"));
        sut.add(clientSubOnTopic("TempSensor2", "temp/1"));

        //Exercise
        final Set<Subscription> matchingSubs1 = sut.recursiveMatch(asTopic("temp"), sut.root);
        final Set<Subscription> matchingSubs2 = sut.recursiveMatch(asTopic("temp/1"), sut.root);

        //Verify
        final Subscription expectedMatchingsub1 = new Subscription("TempSensor1", asTopic("temp"));
        final Subscription expectedMatchingsub2 = new Subscription("TempSensor2", asTopic("temp/1"));

        assertThat(matchingSubs1).contains(expectedMatchingsub1);
        assertThat(matchingSubs2).contains(expectedMatchingsub2);

        sut.removeSubscription(asTopic("temp"),"TempSensor1");

        //Exercise
        final Set<Subscription> matchingSubs3 = sut.recursiveMatch(asTopic("temp"), sut.root);
        final Set<Subscription> matchingSubs4 = sut.recursiveMatch(asTopic("temp/1"), sut.root);

        assertThat(matchingSubs3).doesNotContain(expectedMatchingsub1);
        assertThat(matchingSubs4).contains(expectedMatchingsub2);
    }

    @Test
    public void testRemovalOuterTopicOffRootDiffClient() {
        sut.add(clientSubOnTopic("TempSensor1", "temp"));
        sut.add(clientSubOnTopic("TempSensor2", "temp/1"));

        //Exercise
        final Set<Subscription> matchingSubs1 = sut.recursiveMatch(asTopic("temp"), sut.root);
        final Set<Subscription> matchingSubs2 = sut.recursiveMatch(asTopic("temp/1"), sut.root);

        //Verify
        final Subscription expectedMatchingsub1 = new Subscription("TempSensor1", asTopic("temp"));
        final Subscription expectedMatchingsub2 = new Subscription("TempSensor2", asTopic("temp/1"));

        assertThat(matchingSubs1).contains(expectedMatchingsub1);
        assertThat(matchingSubs2).contains(expectedMatchingsub2);

        sut.removeSubscription(asTopic("temp/1"),"TempSensor2");

        //Exercise
        final Set<Subscription> matchingSubs3 = sut.recursiveMatch(asTopic("temp"), sut.root);
        final Set<Subscription> matchingSubs4 = sut.recursiveMatch(asTopic("temp/1"), sut.root);

        assertThat(matchingSubs3).contains(expectedMatchingsub1);
        assertThat(matchingSubs4).doesNotContain(expectedMatchingsub2);
    }
}


