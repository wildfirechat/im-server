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
import io.moquette.spi.ISessionsStore.ClientTopicCouple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a tree of topics subscriptions.
 */
public class SubscriptionsStore {

    public static class NodeCouple {

        final TreeNode root;
        final TreeNode createdNode;

        public NodeCouple(TreeNode root, TreeNode createdNode) {
            this.root = root;
            this.createdNode = createdNode;
        }
    }

    public interface IVisitor<T> {

        void visit(TreeNode node, int deep);

        T getResult();
    }

    private class DumpTreeVisitor implements IVisitor<String> {

        String s = "";

        @Override
        public void visit(TreeNode node, int deep) {
            String subScriptionsStr = "";
            String indentTabs = indentTabs(deep);
            for (ClientTopicCouple couple : node.m_subscriptions) {
                subScriptionsStr += indentTabs + couple.toString() + "\n";
            }
            s += node.getToken() == null ? "" : node.getToken().toString();
            s += "\n" + (node.m_subscriptions.isEmpty() ? indentTabs : "")
                    + subScriptionsStr /* + "\n" */;
        }

        private String indentTabs(int deep) {
            String s = "";
            for (int i = 0; i < deep; i++) {
                s += "\t";
                // s += "--";
            }
            return s;
        }

        @Override
        public String getResult() {
            return s;
        }
    }

    private AtomicReference<TreeNode> subscriptions = new AtomicReference<>(new TreeNode());
    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionsStore.class);
    private volatile ISessionsStore m_sessionsStore;

    /**
     * Initialize the subscription tree with the list of subscriptions. Maintained for compatibility
     * reasons.
     *
     * @param sessionsStore
     *            to be used as backing store from the subscription store.
     */
    public void init(ISessionsStore sessionsStore) {
        LOG.info("Initializing subscriptions store...");
        m_sessionsStore = sessionsStore;
        List<ClientTopicCouple> subscriptions = sessionsStore.listAllSubscriptions();
        // reload any subscriptions persisted
        if (LOG.isTraceEnabled()) {
            LOG.trace("Reloading all stored subscriptions. SubscriptionTree = {}.", dumpTree());
        }

        for (ClientTopicCouple clientTopic : subscriptions) {
            LOG.info(
                    "Re-subscribing client to topic. ClientId = {}, topicFilter = {}.",
                    clientTopic.clientID,
                    clientTopic.topicFilter);
            add(clientTopic);
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("The stored subscriptions have been reloaded. SubscriptionTree = {}.", dumpTree());
        }
    }

    public void add(ClientTopicCouple newSubscription) {
        /*
         * The topic filters have already been validated at the ProtocolProcessor. We can assume
         * they are valid.
         */
        TreeNode oldRoot;
        NodeCouple couple;
        do {
            oldRoot = subscriptions.get();
            couple = recreatePath(newSubscription.topicFilter, oldRoot);
            couple.createdNode.addSubscription(newSubscription); //createdNode could be null?
            couple.root.recalculateSubscriptionsSize();
            //spin lock repeating till we can, swap root, if can't swap just re-do the operation
        } while (!subscriptions.compareAndSet(oldRoot, couple.root));
        LOG.debug("A subscription has been added. Root = {}, oldRoot = {}.", couple.root, oldRoot);
    }

    protected NodeCouple recreatePath(Topic topic, final TreeNode oldRoot) {
        final TreeNode newRoot = oldRoot.copy();
        TreeNode parent = newRoot;
        TreeNode current = newRoot;
        for (Token token : topic.getTokens()) {
            TreeNode matchingChildren;

            // check if a children with the same token already exists
            if ((matchingChildren = current.childWithToken(token)) != null) {
                // copy the traversed node
                current = matchingChildren.copy();
                // update the child just added in the children list
                parent.updateChild(matchingChildren, current);
                parent = current;
            } else {
                // create a new node for the newly inserted token
                matchingChildren = new TreeNode();
                matchingChildren.setToken(token);
                current.addChild(matchingChildren);
                current = matchingChildren;
            }
        }
        return new NodeCouple(newRoot, current);
    }

    public void removeSubscription(Topic topic, String clientID) {
        /*
         * The topic filters have already been validated at the ProtocolProcessor. We can assume
         * they are valid.
         */
        TreeNode oldRoot;
        NodeCouple couple;
        do {
            oldRoot = subscriptions.get();
            couple = recreatePath(topic, oldRoot);

            couple.createdNode.remove(new ClientTopicCouple(clientID, topic));
            couple.root.recalculateSubscriptionsSize();
            //spin lock repeating till we can, swap root, if can't swap just re-do the operation
        } while (!subscriptions.compareAndSet(oldRoot, couple.root));
    }

    /**
     * Visit the topics tree to remove matching subscriptions with clientID. It's a mutating
     * structure operation so create a new subscription tree (partial or total).
     *
     * @param clientID
     *            the client ID to remove.
     */
    public void removeForClient(String clientID) {
        TreeNode oldRoot;
        TreeNode newRoot;
        do {
            oldRoot = subscriptions.get();
            newRoot = oldRoot.removeClientSubscriptions(clientID);
            // spin lock repeating till we can, swap root, if can't swap just re-do the operation
        } while (!subscriptions.compareAndSet(oldRoot, newRoot));
    }

    /**
     * Given a topic string return the clients subscriptions that matches it. Topic string can't
     * contain character # and + because they are reserved to listeners subscriptions, and not topic
     * publishing.
     *
     * @param topic
     *            to use fo searching matching subscriptions.
     * @return the list of matching subscriptions, or empty if not matching.
     */
    public List<Subscription> matches(Topic topic) {
        Queue<Token> tokenQueue = new LinkedBlockingDeque<>(topic.getTokens());
        List<ClientTopicCouple> matchingSubs = new ArrayList<>();
        subscriptions.get().matches(tokenQueue, matchingSubs);

        // remove the overlapping subscriptions, selecting ones with greatest qos
        Map<String, Subscription> subsForClient = new HashMap<>();
        for (ClientTopicCouple matchingCouple : matchingSubs) {
            Subscription existingSub = subsForClient.get(matchingCouple.clientID);
            Subscription sub = m_sessionsStore.getSubscription(matchingCouple);
            if (sub == null) {
                // if the m_sessionStore hasn't the sub because the client disconnected
                continue;
            }
            // update the selected subscriptions if not present or if has a greater qos
            if (existingSub == null || existingSub.getRequestedQos().value() < sub.getRequestedQos().value()) {
                subsForClient.put(matchingCouple.clientID, sub);
            }
        }
        return new ArrayList<>(subsForClient.values());
    }

    public boolean contains(Subscription sub) {
        return !matches(sub.topicFilter).isEmpty();
    }

    public int size() {
        return subscriptions.get().size();
    }

    public String dumpTree() {
        DumpTreeVisitor visitor = new DumpTreeVisitor();
        bfsVisit(subscriptions.get(), visitor, 0);
        return visitor.getResult();
    }

    private void bfsVisit(TreeNode node, IVisitor<?> visitor, int deep) {
        if (node == null) {
            return;
        }
        visitor.visit(node, deep);
        for (TreeNode child : node.m_children) {
            bfsVisit(child, visitor, ++deep);
        }
    }
}
