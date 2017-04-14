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

import io.moquette.spi.ISessionsStore;
import org.junit.Test;

import static org.junit.Assert.*;

public class TreeNodeTest {

    @Test
    public void testRemoveTreeNodesAfterEmptySubscriptions() {
        TreeNode root = new TreeNode();
        TreeNode subNode1 = new TreeNode();

        subNode1.addSubscription(new ISessionsStore.ClientTopicCouple("Client1", new Topic("/topic1")));
        root.addChild(subNode1);

        TreeNode updatedRoot = root.removeClientSubscriptions("Client1");

        assertTrue("Empty subscriptions node should be removed", updatedRoot.m_children.isEmpty());
    }

    @Test
    public void testRemoveTreeNodesAfterEmptySubscriptions_multiLayer() {
        TreeNode root = new TreeNode();
        TreeNode intermediateEmptyNode = new TreeNode();
        TreeNode subNode1 = new TreeNode();

        subNode1.addSubscription(new ISessionsStore.ClientTopicCouple("Client1", new Topic("/sub/topic1")));
        intermediateEmptyNode.addChild(subNode1);
        root.addChild(intermediateEmptyNode);

        TreeNode updatedRoot = root.removeClientSubscriptions("Client1");

        assertTrue("Empty subscriptions node should be removed", updatedRoot.m_children.isEmpty());
    }

    @Test
    public void testRemoveTreeNodesAfterEmptySubscriptions_multiLayer_doesntRemoveIntermediateNodes() {
        TreeNode root = new TreeNode();
        TreeNode intermediateEmptyNode = new TreeNode();

        TreeNode subNode1 = new TreeNode();
        subNode1.addSubscription(new ISessionsStore.ClientTopicCouple("Client1", new Topic("/sub/topic1")));
        intermediateEmptyNode.addChild(subNode1);

        TreeNode subNode2 = new TreeNode();
        subNode2.addSubscription(new ISessionsStore.ClientTopicCouple("Client2", new Topic("/sub/topic2")));
        intermediateEmptyNode.addChild(subNode2);
        root.addChild(intermediateEmptyNode);

        TreeNode updatedRoot = root.removeClientSubscriptions("Client1");

        assertFalse("Remove a subscription shouldn't remove intermediate empty nodes",
                updatedRoot.m_children.isEmpty());
    }

}
