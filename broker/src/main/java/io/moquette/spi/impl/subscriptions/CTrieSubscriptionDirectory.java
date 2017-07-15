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
import io.moquette.spi.ISubscriptionsStore;
import io.moquette.spi.ISubscriptionsStore.ClientTopicCouple;
import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class CTrieSubscriptionDirectory implements ISubscriptionsDirectory {

    private static final Logger LOG = LoggerFactory.getLogger(CTrieSubscriptionDirectory.class);

    private static final Token ROOT = new Token("root");

    protected INode root;
    private volatile ISubscriptionsStore subscriptionsStore;

    private interface IVisitor<T> {

        void visit(CNode node, int deep);

        T getResult();
    }

    private class DumpTreeVisitor implements IVisitor<String> {

        String s = "";

        @Override
        public void visit(CNode node, int deep) {
            String indentTabs = indentTabs(deep);
            s += indentTabs + (node.token == null ? "" : node.token.toString()) + prettySubscriptions(node) + "\n";
        }

        private String prettySubscriptions(CNode node) {
            if (node.subscriptions.isEmpty()) {
                return StringUtil.EMPTY_STRING;
            }
            StringBuilder subScriptionsStr = new StringBuilder(" ~~[");
            int counter = 0;
            for (ClientTopicCouple couple : node.subscriptions) {
                subScriptionsStr
                    .append("{filter=").append(couple.topicFilter).append(", ")
                    .append("client='").append(couple.clientID).append("'}");
                counter++;
                if (counter < node.subscriptions.size()) {
                    subScriptionsStr.append(";");
                }
            }
            return subScriptionsStr.append("]").toString();
        }

        private String indentTabs(int deep) {
            StringBuilder s = new StringBuilder();
            if (deep > 0) {
                for (int i = 0; i < deep - 1; i++) {
                    s.append("| ");
                }
                s.append("|-");
            }
            return s.toString();
        }

        @Override
        public String getResult() {
            return s;
        }
    }

    private enum Action {
        OK, REPEAT
    }

    public void init(ISessionsStore sessionsStore) {
        LOG.info("Initializing CTrie");
        final CNode mainNode = new CNode();
        mainNode.token = ROOT;
        this.root = new INode(mainNode);

        LOG.info("Initializing subscriptions store...");
        this.subscriptionsStore = sessionsStore.subscriptionStore();
        List<ClientTopicCouple> subscriptions = this.subscriptionsStore.listAllSubscriptions();
        // reload any subscriptions persisted
        if (LOG.isTraceEnabled()) {
            LOG.trace("Reloading all stored subscriptions. SubscriptionTree = {}", dumpTree());
        }

        for (ClientTopicCouple clientTopic : subscriptions) {
            LOG.info("Re-subscribing client to topic CId={}, topicFilter={}", clientTopic.clientID,
                clientTopic.topicFilter);
            add(clientTopic);
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("Stored subscriptions have been reloaded. SubscriptionTree = {}", dumpTree());
        }
    }

    Optional<CNode> lookup(Topic topic) {
        INode inode = this.root;
        Token token = topic.headToken();
        while (!topic.isEmpty() && (inode.mainNode().anyChildrenMatch(token))) {
            topic = topic.exceptHeadToken();
            inode = inode.mainNode().childOf(token);
            token = topic.headToken();
        }
        if (inode == null || !topic.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(inode.mainNode());
    }


    @Override
    public List<Subscription> matches(Topic topic) {
        return new ArrayList<>(match(topic));
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
     Set<Subscription> match(Topic topic) {
        final Set<ClientTopicCouple> matchingSubs = recursiveMatch(topic, this.root);
        // remove the overlapping subscriptions, selecting ones with greatest qos
        Map<String, Subscription> subsForClient = new HashMap<>();
        for (ClientTopicCouple matchingCouple : matchingSubs) {
            Subscription existingSub = subsForClient.get(matchingCouple.clientID);
            Subscription sub = this.subscriptionsStore.getSubscription(matchingCouple);
            if (sub == null) {
                // if the m_sessionStore hasn't the sub because the client disconnected
                continue;
            }
            // update the selected subscriptions if not present or if has a greater qos
            if (existingSub == null || existingSub.getRequestedQos().value() < sub.getRequestedQos().value()) {
                subsForClient.put(matchingCouple.clientID, sub);
            }
        }
        return new HashSet<>(subsForClient.values());
    }

    Set<ClientTopicCouple> recursiveMatch(Topic topic, INode inode) {
        CNode cnode = inode.mainNode();
        if (cnode.token == Token.MULTI) {
            return cnode.subscriptions;
        }
        if (topic.isEmpty()) {
            return Collections.emptySet();
        }
        if (cnode instanceof TNode) {
            return Collections.emptySet();
        }
        final Token token = topic.headToken();
        if (!(cnode.token == Token.SINGLE || cnode.token.equals(token) || cnode.token == ROOT)) {
            return Collections.emptySet();
        }
        Topic remainingTopic = (cnode.token == ROOT) ? topic : topic.exceptHeadToken();
        Set<ClientTopicCouple> subscriptions = new HashSet<>();
        if (remainingTopic.isEmpty()) {
            subscriptions.addAll(cnode.subscriptions);
        }
        for (INode subInode : cnode.allChildren()) {
            subscriptions.addAll(recursiveMatch(remainingTopic, subInode));
        }
        return subscriptions;
    }


    public void add(ClientTopicCouple newSubscription) {
        Action res;
        do {
            res = insert(newSubscription.clientID, newSubscription.topicFilter, this.root, newSubscription.topicFilter);
        } while (res == Action.REPEAT);
    }

    private Action insert(String clientId, Topic topic, final INode inode, Topic fullpath) {
        Token token = topic.headToken();
        if (!topic.isEmpty() && inode.mainNode().anyChildrenMatch(token)) {
            Topic remainingTopic = topic.exceptHeadToken();
            INode nextInode = inode.mainNode().childOf(token);
            return insert(clientId, remainingTopic, nextInode, fullpath);
        } else {
            if (topic.isEmpty()) {
                return insertSubscription(clientId, fullpath, inode);
            } else {
                return createNodeAndInsertSubscription(clientId, topic, inode, fullpath);
            }
        }
    }

    private Action insertSubscription(String clientId, Topic topic, INode inode) {
        CNode cnode = inode.mainNode();
        CNode updatedCnode = cnode.copy().addSubscription(clientId, topic);
        if (inode.compareAndSet(cnode, updatedCnode)) {
            return Action.OK;
        } else {
            return Action.REPEAT;
        }
    }

    private Action createNodeAndInsertSubscription(String clientId, Topic topic, INode inode, Topic fullpath) {
        INode newInode = createPathRec(clientId, topic, fullpath);
        CNode cnode = inode.mainNode();
        CNode updatedCnode = cnode.copy();
        updatedCnode.add(newInode);

        return inode.compareAndSet(cnode, updatedCnode) ? Action.OK : Action.REPEAT;
    }

    private INode createLeafNodes(String clientId, Topic fullpath, Token token) {
        CNode newLeafCnode = new CNode();
        newLeafCnode.token = token;
        newLeafCnode.addSubscription(clientId, fullpath);

        return new INode(newLeafCnode);
    }

    private INode createPathRec(String clientId, Topic topic, Topic fullpath) {
        Topic remainingTopic = topic.exceptHeadToken();
        if (!remainingTopic.isEmpty()) {
            INode inode = createPathRec(clientId, remainingTopic, fullpath);
            CNode cnode = new CNode();
            cnode.token = topic.headToken();
            cnode.add(inode);
            return new INode(cnode);
        } else {
            return createLeafNodes(clientId, fullpath, topic.headToken());
        }
    }

    public void removeSubscription(Topic topic, String clientID) {
        Action res;
        do {
            res = remove(clientID, topic, this.root);
        } while (res == Action.REPEAT);
    }

    private Action remove(String clientId, Topic topic, INode inode) {
        Token token = topic.headToken();
        if (!topic.isEmpty() && (inode.mainNode().anyChildrenMatch(token))) {
            Topic remainingTopic = topic.exceptHeadToken();
            INode nextInode = inode.mainNode().childOf(token);
            return remove(clientId, remainingTopic, nextInode);
        } else {
            final CNode cnode = inode.mainNode();
            if (cnode.containsOnly(clientId)) {
                TNode tnode = new TNode();
                return inode.compareAndSet(cnode, tnode) ? Action.OK : Action.REPEAT;
            } else if (cnode.contains(clientId)) {
                CNode updatedCnode = cnode.copy();
                updatedCnode.removeSubscriptionsFor(clientId);
                return inode.compareAndSet(cnode, updatedCnode) ? Action.OK : Action.REPEAT;
            } else {
                //someone else already removed
                return Action.OK;
            }
        }
    }

    private class SubscriptionCounterVisitor implements IVisitor<Integer> {

        AtomicInteger accumulator = new AtomicInteger(0);

        @Override
        public void visit(CNode node, int deep) {
            accumulator.addAndGet(node.subscriptions.size());
        }

        @Override
        public Integer getResult() {
            return accumulator.get();
        }
    }

    public int size() {
        SubscriptionCounterVisitor visitor = new SubscriptionCounterVisitor();
        dfsVisit(this.root, visitor, 0);
        return visitor.getResult();
    }

    public String dumpTree() {
        DumpTreeVisitor visitor = new DumpTreeVisitor();
        dfsVisit(this.root, visitor, 0);
        return visitor.getResult();
    }

    private void dfsVisit(INode node, IVisitor<?> visitor, int deep) {
        if (node == null) {
            return;
        }

        visitor.visit(node.mainNode(), deep);
        ++deep;
        for (INode child : node.mainNode().allChildren()) {
            dfsVisit(child, visitor, deep);
        }
    }
}
