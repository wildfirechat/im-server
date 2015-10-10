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

import java.text.ParseException;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.moquette.spi.ISessionsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a tree of topics subscriptions.
 *
 * @author andrea
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

    /**
     * Check if the topic filter of the subscription is well formed
     * */
    public static boolean validate(String topicFilter) {
        try {
            parseTopic(topicFilter);
            return true;
        } catch (ParseException pex) {
            LOG.info("Bad matching topic filter <{}>", topicFilter);
            return false;
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
            for (Subscription sub : node.m_subscriptions) {
                subScriptionsStr += indentTabs + sub.toString() + "\n";
            }
            s += node.getToken() == null ? "" : node.getToken().toString();
            s +=  "\n" + (node.m_subscriptions.isEmpty() ? indentTabs : "") + subScriptionsStr /*+ "\n"*/;
        }

        private String indentTabs(int deep) {
            String s = "";
            for (int i=0; i < deep; i++) {
                s += "\t";
//                s += "--";
            }
            return s;
        }

        @Override
        public String getResult() {
            return s;
        }
    }
    
    private class SubscriptionTreeCollector implements IVisitor<List<Subscription>> {
        
        private List<Subscription> m_allSubscriptions = new ArrayList<>();

        @Override
        public void visit(TreeNode node, int deep) {
            m_allSubscriptions.addAll(node.subscriptions());
        }

        @Override
        public List<Subscription> getResult() {
            return m_allSubscriptions;
        }
    }

    private AtomicReference<TreeNode> subscriptions = new AtomicReference<>(new TreeNode(null));
    private ISessionsStore m_sessionsStore;
    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionsStore.class);

    /**
     * Initialize the subscription tree with the list of subscriptions.
     * Maintained for compatibility reasons.
     */
    public void init(ISessionsStore sessionsStore) {
        LOG.debug("init invoked");
        m_sessionsStore = sessionsStore;
        List<Subscription> subscriptions = sessionsStore.listAllSubscriptions();
        //reload any subscriptions persisted
        if (LOG.isDebugEnabled()) {
            LOG.debug("Reloading all stored subscriptions...subscription tree before {}", dumpTree());
        }

        for (Subscription subscription : subscriptions) {
            LOG.debug("Re-subscribing {} to topic {}", subscription.getClientId(), subscription.getTopicFilter());
            addDirect(subscription);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finished loading. Subscription tree after {}", dumpTree());
        }
    }

    protected void addDirect(Subscription newSubscription) {
        TreeNode oldRoot;
        NodeCouple couple;
        do {
            oldRoot = subscriptions.get();
            couple = recreatePath(newSubscription.topicFilter, oldRoot);
            couple.createdNode.addSubscription(newSubscription); //createdNode could be null?
            //spin lock repeating till we can, swap root, if can't swap just re-do the operation
        } while(!subscriptions.compareAndSet(oldRoot, couple.root));
        LOG.debug("root ref {}, original root was {}", couple.root, oldRoot);
    }


    protected NodeCouple recreatePath(String topic, final TreeNode oldRoot) {
        List<Token> tokens = new ArrayList<>();
        try {
            tokens = parseTopic(topic);
        } catch (ParseException ex) {
            //TODO handle the parse exception
            LOG.error(null, ex);
        }

        final TreeNode newRoot = oldRoot.copy();
        TreeNode parent = newRoot;
        TreeNode current = newRoot;
        for (Token token : tokens) {
            TreeNode matchingChildren;

            //check if a children with the same token already exists
            if ((matchingChildren = current.childWithToken(token)) != null) {
                //copy the traversed node
                current = matchingChildren.copy();
                current.m_parent = parent;
                //update the child just added in the children list
                parent.updateChild(matchingChildren, current);
                parent = current;
            } else {
                //create a new node for the newly inserted token
                matchingChildren = new TreeNode(current);
                matchingChildren.setToken(token);
                current.addChild(matchingChildren);
                current = matchingChildren;
            }
        }
        return new NodeCouple(newRoot, current);
    }

    public void add(Subscription newSubscription) {
        addDirect(newSubscription);
    }


    public void removeSubscription(String topic, String clientID) {
        TreeNode oldRoot;
        NodeCouple couple;
        do {
            oldRoot = subscriptions.get();
            couple = recreatePath(topic, oldRoot);

            //do the job
            //search for the subscription to remove
            Subscription toBeRemoved = null;
            for (Subscription sub : couple.createdNode.subscriptions()) {
                if (sub.topicFilter.equals(topic) && sub.getClientId().equals(clientID)) {
                    toBeRemoved = sub;
                    break;
                }
            }

            if (toBeRemoved != null) {
                couple.createdNode.subscriptions().remove(toBeRemoved);
            }
            //spin lock repeating till we can, swap root, if can't swap just re-do the operation
        } while(!subscriptions.compareAndSet(oldRoot, couple.root));
    }
    
    /**
     * It's public because needed in tests :-( bleah
     */
    public void clearAllSubscriptions() {
        SubscriptionTreeCollector subsCollector = new SubscriptionTreeCollector();
        bfsVisit(subscriptions.get(), subsCollector, 0);
        
        List<Subscription> allSubscriptions = subsCollector.getResult();
        for (Subscription subscription : allSubscriptions) {
            removeSubscription(subscription.getTopicFilter(), subscription.getClientId());
        }
    }

    /**
     * Visit the topics tree to remove matching subscriptions with clientID.
     * It's a mutating structure operation so create a new subscription tree (partial or total).
     */
    public void removeForClient(String clientID) {
        TreeNode oldRoot;
        TreeNode newRoot;
        do {
            oldRoot = subscriptions.get();
            newRoot = oldRoot.removeClientSubscriptions(clientID);
            //spin lock repeating till we can, swap root, if can't swap just re-do the operation
        } while(!subscriptions.compareAndSet(oldRoot, newRoot));
        //persist the update
        m_sessionsStore.wipeSubscriptions(clientID);
    }

    /**
     * Visit the topics tree to deactivate matching subscriptions with clientID.
     * It's a mutating structure operation so create a new subscription tree (partial or total).
     */
    public void deactivate(String clientID) {
        LOG.debug("Disactivating subscriptions for clientID <{}>", clientID);
        TreeNode oldRoot;
        TreeNode newRoot;
        do {
            oldRoot = subscriptions.get();
            newRoot = oldRoot.deactivate(clientID);
            //spin lock repeating till we can, swap root, if can't swap just re-do the operation
        } while(!subscriptions.compareAndSet(oldRoot, newRoot));

        //persist the update
        Set<Subscription> subs = newRoot.findAllByClientID(clientID);
        m_sessionsStore.updateSubscriptions(clientID, subs);
    }

    /**
     * Visit the topics tree to activate matching subscriptions with clientID.
     * It's a mutating structure operation so create a new subscription tree (partial or total).
     */
    public void activate(String clientID) {
        LOG.debug("Activating subscriptions for clientID <{}>", clientID);
        TreeNode oldRoot;
        TreeNode newRoot;
        do {
            oldRoot = subscriptions.get();
            newRoot = oldRoot.activate(clientID);
            //spin lock repeating till we can, swap root, if can't swap just re-do the operation
        } while(!subscriptions.compareAndSet(oldRoot, newRoot));

        //persist the update
        Set<Subscription> subs = newRoot.findAllByClientID(clientID);
        m_sessionsStore.updateSubscriptions(clientID, subs);
    }

    /**
     * Given a topic string return the clients subscriptions that matches it.
     * Topic string can't contain character # and + because they are reserved to
     * listeners subscriptions, and not topic publishing.
     */
    public List<Subscription> matches(String topic) {
        List<Token> tokens;
        try {
            tokens = parseTopic(topic);
        } catch (ParseException ex) {
            //TODO handle the parse exception
            LOG.error(null, ex);
            return Collections.emptyList();
        }

        Queue<Token> tokenQueue = new LinkedBlockingDeque<>(tokens);
        List<Subscription> matchingSubs = new ArrayList<>();
        subscriptions.get().matches(tokenQueue, matchingSubs);

        //remove the overlapping subscriptions, selecting ones with greatest qos
        Map<String, Subscription> subsForClient = new HashMap<>();
        for (Subscription sub : matchingSubs) {
            Subscription existingSub = subsForClient.get(sub.getClientId());
            //update the selected subscriptions if not present or if has a greater qos
            if (existingSub == null || existingSub.getRequestedQos().byteValue() < sub.getRequestedQos().byteValue()) {
                subsForClient.put(sub.getClientId(), sub);
            }
        }
        return /*matchingSubs*/new ArrayList<>(subsForClient.values());
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
    
    private void bfsVisit(TreeNode node, IVisitor visitor, int deep) {
        if (node == null) {
            return;
        }
        visitor.visit(node, deep);
        for (TreeNode child : node.m_children) {
            bfsVisit(child, visitor, ++deep);
        }
    }
    
    /**
     * Verify if the 2 topics matching respecting the rules of MQTT Appendix A
     */
    //TODO reimplement with iterators or with queues
    public static boolean matchTopics(String msgTopic, String subscriptionTopic) {
        try {
            List<Token> msgTokens = SubscriptionsStore.parseTopic(msgTopic);
            List<Token> subscriptionTokens = SubscriptionsStore.parseTopic(subscriptionTopic);
            int i = 0;
            for (; i< subscriptionTokens.size(); i++) {
                Token subToken = subscriptionTokens.get(i);
                if (subToken != Token.MULTI && subToken != Token.SINGLE) {
                    if (i >= msgTokens.size()) {
                        return false;
                    }
                    Token msgToken = msgTokens.get(i);
                    if (!msgToken.equals(subToken)) {
                        return false;
                    }
                } else {
                    if (subToken == Token.MULTI) {
                        return true;
                    }
                    if (subToken == Token.SINGLE) {
                        //skip a step forward
                    }
                }
            }
            //if last token was a SINGLE then treat it as an empty
//            if (subToken == Token.SINGLE && (i - msgTokens.size() == 1)) {
//               i--;
//            }
            return i == msgTokens.size();
        } catch (ParseException ex) {
            LOG.error(null, ex);
            throw new RuntimeException(ex);
        }
    }
    
    protected static List<Token> parseTopic(String topic) throws ParseException {
        List<Token> res = new ArrayList<>();
        String[] splitted = topic.split("/");

        if (splitted.length == 0) {
            res.add(Token.EMPTY);
        }
        
        if (topic.endsWith("/")) {
            //Add a fictious space 
            String[] newSplitted = new String[splitted.length + 1];
            System.arraycopy(splitted, 0, newSplitted, 0, splitted.length); 
            newSplitted[splitted.length] = "";
            splitted = newSplitted;
        }
        
        for (int i = 0; i < splitted.length; i++) {
            String s = splitted[i];
            if (s.isEmpty()) {
//                if (i != 0) {
//                    throw new ParseException("Bad format of topic, expetec topic name between separators", i);
//                }
                res.add(Token.EMPTY);
            } else if (s.equals("#")) {
                //check that multi is the last symbol
                if (i != splitted.length - 1) {
                    throw new ParseException("Bad format of topic, the multi symbol (#) has to be the last one after a separator", i);
                }
                res.add(Token.MULTI);
            } else if (s.contains("#")) {
                throw new ParseException("Bad format of topic, invalid subtopic name: " + s, i);
            } else if (s.equals("+")) {
                res.add(Token.SINGLE);
            } else if (s.contains("+")) {
                throw new ParseException("Bad format of topic, invalid subtopic name: " + s, i);
            } else {
                res.add(new Token(s));
            }
        }

        return res;
    }
}
