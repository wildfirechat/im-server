/*
 * Copyright (c) 2012-2018 The original author or authors
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

import io.moquette.spi.ClientSession;
import io.moquette.spi.impl.SessionsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static java.lang.String.format;

public class CTrieSubscriptionDirectory implements ISubscriptionsDirectory {

    private static final Logger LOG = LoggerFactory.getLogger(CTrieSubscriptionDirectory.class);

    private static final Token ROOT = new Token("root");
    private static final INode NO_PARENT = null;

    INode root;
    private volatile SessionsRepository sessionsRepository;

    interface IVisitor<T> {

        void visit(CNode node, int deep);

        T getResult();
    }

    private enum Action {
        OK, REPEAT
    }

    @Override
    public void init(SessionsRepository sessionsRepository) {
        LOG.info("Initializing CTrie");
        final CNode mainNode = new CNode();
        mainNode.token = ROOT;
        this.root = new INode(mainNode);

        LOG.info("Initializing subscriptions store...");
        this.sessionsRepository = sessionsRepository;
        // reload any subscriptions persisted
        if (LOG.isTraceEnabled()) {
            LOG.trace("Reloading all stored subscriptions. SubscriptionTree = {}", dumpTree());
        }
        for (ClientSession session : this.sessionsRepository.getAllSessions()) {
            for (Subscription subscription : session.getSubscriptions()) {
                LOG.info("Re-subscribing client to topic CId={}, topicFilter={}", subscription.clientId,
                    subscription.topicFilter);
                add(subscription);
            }
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
    @Override
    public Set<Subscription> matchWithoutQosSharpening(Topic topic) {
        return recursiveMatch(topic, this.root);
    }

    @Override
    public Set<Subscription> matchQosSharpening(Topic topic) {
        final Set<Subscription> subscriptions = matchWithoutQosSharpening(topic);

        Map<String, Subscription> subsGroupedByClient = new HashMap<>();
        for (Subscription sub : subscriptions) {
            Subscription existingSub = subsGroupedByClient.get(sub.clientId);
            // update the selected subscriptions if not present or if has a greater qos
            if (existingSub == null || existingSub.qosLessThan(sub)) {
                subsGroupedByClient.put(sub.clientId, sub);
            }
        }
        return new HashSet<>(subsGroupedByClient.values());
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
    @Deprecated
    Set<Subscription> match(Topic topic) {
        final Set<Subscription> matchingSubs = matchWithoutQosSharpening(topic);
        // remove the overlapping subscriptions, selecting ones with greatest qos
        Map<String, Subscription> subsForClient = loadSubscriptionsFromStorage(matchingSubs);
        return new HashSet<>(subsForClient.values());
    }

    @Deprecated
    private Map<String, Subscription> loadSubscriptionsFromStorage(Set<Subscription> matchingSubs) {
        Map<String, Subscription> subsForClient = new HashMap<>();
        for (Subscription matchingSub : matchingSubs) {
            Subscription existingSub = subsForClient.get(matchingSub.clientId);
            final ClientSession subscribedSession = this.sessionsRepository.sessionForClient(matchingSub.clientId);
            if (subscribedSession == null) {
                //clean session disconnected
                continue;
            }
            Subscription sub = subscribedSession.findSubscriptionByTopicFilter(matchingSub);
            if (sub == null) {
                final String excpMesg = format("Target session %s is connected but doesn't anymore subscribed to %s",
                    matchingSub.clientId, matchingSub);
                throw new IllegalStateException(excpMesg);
            }
            // update the selected subscriptions if not present or if has a greater qos
            if (existingSub == null || existingSub.qosLessThan(sub)) {
                subsForClient.put(sub.clientId, sub);
            }
        }
        return subsForClient;
    }

    Set<Subscription> recursiveMatch(Topic topic, INode inode) {
        CNode cnode = inode.mainNode();
        if (Token.MULTI.equals(cnode.token)) {
            return cnode.subscriptions;
        }
        if (topic.isEmpty()) {
            return Collections.emptySet();
        }
        if (cnode instanceof TNode) {
            return Collections.emptySet();
        }
        final Token token = topic.headToken();
        if (!(Token.SINGLE.equals(cnode.token) || cnode.token.equals(token) || ROOT.equals(cnode.token))) {
            return Collections.emptySet();
        }
        Topic remainingTopic = (ROOT.equals(cnode.token)) ? topic : topic.exceptHeadToken();
        Set<Subscription> subscriptions = new HashSet<>();
        if (remainingTopic.isEmpty()) {
            subscriptions.addAll(cnode.subscriptions);
        }
        for (INode subInode : cnode.allChildren()) {
            subscriptions.addAll(recursiveMatch(remainingTopic, subInode));
        }
        return subscriptions;
    }

    /**
     *
     * Cleans Disposes of TNode in separate Atomic CAS operation per
     * http://bravenewgeek.com/breaking-and-entering-lose-the-lock-while-embracing-concurrency/
     *
     * We roughly follow this theory above, but we allow CNode with no Subscriptions to linger (for now).
     *
     *
     * @param inode
     * @param iParent
     * @return
     */
    public Action cleanTomb(INode inode, INode iParent) {
        CNode updatedCnode = iParent.mainNode().copy();
        updatedCnode.remove(inode);
        return iParent.compareAndSet(iParent.mainNode(), updatedCnode) ? Action.OK : Action.REPEAT;
    }

    @Override
    public void add(Subscription newSubscription) {
        Action res;
        do {
            res = insert(newSubscription.topicFilter, this.root, newSubscription);
        } while (res == Action.REPEAT);
    }

    private Action insert(Topic topic, final INode inode, Subscription newSubscription) {
        Token token = topic.headToken();
        if (!topic.isEmpty() && inode.mainNode().anyChildrenMatch(token)) {
            Topic remainingTopic = topic.exceptHeadToken();
            INode nextInode = inode.mainNode().childOf(token);
            return insert(remainingTopic, nextInode, newSubscription);
        } else {
            if (topic.isEmpty()) {
                return insertSubscription(inode, newSubscription);
            } else {
                return createNodeAndInsertSubscription(topic, inode, newSubscription);
            }
        }
    }

    private Action insertSubscription(INode inode, Subscription newSubscription) {
        CNode cnode = inode.mainNode();
        CNode updatedCnode = cnode.copy().addSubscription(newSubscription);
        if (inode.compareAndSet(cnode, updatedCnode)) {
            return Action.OK;
        } else {
            return Action.REPEAT;
        }
    }

    private Action createNodeAndInsertSubscription(Topic topic, INode inode, Subscription newSubscription) {
        INode newInode = createPathRec(topic, newSubscription);
        CNode cnode = inode.mainNode();
        CNode updatedCnode = cnode.copy();
        updatedCnode.add(newInode);

        return inode.compareAndSet(cnode, updatedCnode) ? Action.OK : Action.REPEAT;
    }

    private INode createLeafNodes(Token token, Subscription newSubscription) {
        CNode newLeafCnode = new CNode();
        newLeafCnode.token = token;
        newLeafCnode.addSubscription(newSubscription);

        return new INode(newLeafCnode);
    }

    private INode createPathRec(Topic topic, Subscription newSubscription) {
        Topic remainingTopic = topic.exceptHeadToken();
        if (!remainingTopic.isEmpty()) {
            INode inode = createPathRec(remainingTopic, newSubscription);
            CNode cnode = new CNode();
            cnode.token = topic.headToken();
            cnode.add(inode);
            return new INode(cnode);
        } else {
            return createLeafNodes(topic.headToken(), newSubscription);
        }
    }

    /**
     * Removes subscription from CTrie, adds TNode when the last client unsubscribes, then calls for cleanTomb in a
     * separate atomic CAS operation.
     *
     * @param topic
     * @param clientID
     */
    @Override
    public void removeSubscription(Topic topic, String clientID) {
        Action res;
        do {
            res = remove(clientID, topic, this.root, NO_PARENT);
        } while (res == Action.REPEAT);
    }

    private Action remove(String clientId, Topic topic, INode inode, INode iParent) {
        Token token = topic.headToken();
        if (!topic.isEmpty() && (inode.mainNode().anyChildrenMatch(token))) {
            Topic remainingTopic = topic.exceptHeadToken();
            INode nextInode = inode.mainNode().childOf(token);
            return remove(clientId, remainingTopic, nextInode, inode);
        } else {
            final CNode cnode = inode.mainNode();
            if (cnode instanceof TNode) {
                // this inode is a tomb, has no clients and should be cleaned up
                // Because we implemented cleanTomb below, this should be rare, but possible
                // Consider calling cleanTomb here too
                return Action.OK;
            }
            if (cnode.containsOnly(clientId) && topic.isEmpty() && cnode.allChildren().isEmpty()) {
                // last client to leave this node, AND there are no downstream children, remove via TNode tomb
                if (inode == this.root) {
                    return inode.compareAndSet(cnode, inode.mainNode().copy()) ? Action.OK : Action.REPEAT;
                }
                TNode tnode = new TNode();
                return inode.compareAndSet(cnode, tnode) ? cleanTomb(inode, iParent) : Action.REPEAT;
            } else if (cnode.contains(clientId) && topic.isEmpty()) {
                CNode updatedCnode = cnode.copy();
                updatedCnode.removeSubscriptionsFor(clientId);
                return inode.compareAndSet(cnode, updatedCnode) ? Action.OK : Action.REPEAT;
            } else {
                //someone else already removed
                return Action.OK;
            }
        }
    }

    @Override
    public int size() {
        SubscriptionCounterVisitor visitor = new SubscriptionCounterVisitor();
        dfsVisit(this.root, visitor, 0);
        return visitor.getResult();
    }

    @Override
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
