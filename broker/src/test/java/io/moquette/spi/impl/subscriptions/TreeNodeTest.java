package io.moquette.spi.impl.subscriptions;

import io.moquette.spi.ISessionsStore;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by andrea on 03/03/17.
 */
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

        assertFalse("Remove a subscription shouldn't remove intermediate empty nodes", updatedRoot.m_children.isEmpty());
    }

}
