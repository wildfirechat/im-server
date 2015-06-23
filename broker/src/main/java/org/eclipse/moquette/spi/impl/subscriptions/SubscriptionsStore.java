/*
 * Copyright (c) 2012-2014 The original author or authors
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

import org.eclipse.moquette.spi.ISessionsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a tree of topics subscriptions.
 *
 * @author andrea
 */
public class SubscriptionsStore {

    /**
     * Check if the topic filter of the subscription is well formed
     * */
    public static boolean validate(Subscription newSubscription) {
        try {
            parseTopic(newSubscription.topicFilter);
            return true;
        } catch (ParseException pex) {
            LOG.info("Bad matching topic filter <{}>", newSubscription.topicFilter);
            return false;
        }
    }

    public static interface IVisitor<T> {
        void visit(TreeNode node, int deep);
        
        T getResult();
    }
    
    private class DumpTreeVisitor implements IVisitor<String> {
        
        String s = "";

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
        
        public String getResult() {
            return s;
        }
    }
    
    private class SubscriptionTreeCollector implements IVisitor<List<Subscription>> {
        
        private List<Subscription> m_allSubscriptions = new ArrayList<Subscription>();

        public void visit(TreeNode node, int deep) {
            m_allSubscriptions.addAll(node.subscriptions());
        }
        
        public List<Subscription> getResult() {
            return m_allSubscriptions;
        }
    }

    private TreeNode subscriptions = new TreeNode(null);
    private ISessionsStore m_sessionsStore;
    private static final Logger LOG = LoggerFactory.getLogger(SubscriptionsStore.class);

    /**
     * Initialize the subscription tree with the list of subscriptions.
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
        TreeNode current = findMatchingNode(newSubscription.topicFilter);
        current.addSubscription(newSubscription);
    }
    
    private TreeNode findMatchingNode(String topic) {
        List<Token> tokens = new ArrayList<>();
        try {
            tokens = parseTopic(topic);
        } catch (ParseException ex) {
            //TODO handle the parse exception
            LOG.error(null, ex);
//            return;
        }

        TreeNode current = subscriptions;
        for (Token token : tokens) {
            TreeNode matchingChildren;

            //check if a children with the same token already exists
            if ((matchingChildren = current.childWithToken(token)) != null) {
                current = matchingChildren;
            } else {
                //create a new node for the newly inserted token
                matchingChildren = new TreeNode(current);
                matchingChildren.setToken(token);
                current.addChild(matchingChildren);
                current = matchingChildren;
            }
        }
        return current;
    }

    public void add(Subscription newSubscription) {
        addDirect(newSubscription);

        //log the subscription
//        String clientID = newSubscription.getClientId();
//        m_storageService.addNewSubscription(newSubscription, clientID);
    }


    public void removeSubscription(String topic, String clientID) {
        TreeNode matchNode = findMatchingNode(topic);
        
        //search for the subscription to remove
        Subscription toBeRemoved = null;
        for (Subscription sub : matchNode.subscriptions()) {
            if (sub.topicFilter.equals(topic) && sub.getClientId().equals(clientID)) {
                toBeRemoved = sub;
                break;
            }
        }
        
        if (toBeRemoved != null) {
            matchNode.subscriptions().remove(toBeRemoved);
        }
    }
    
    /**
     * TODO implement testing
     */
    public void clearAllSubscriptions() {
        SubscriptionTreeCollector subsCollector = new SubscriptionTreeCollector();
        bfsVisit(subscriptions, subsCollector, 0);
        
        List<Subscription> allSubscriptions = subsCollector.getResult();
        for (Subscription subscription : allSubscriptions) {
            removeSubscription(subscription.getTopicFilter(), subscription.getClientId());
        }
    }

    /**
     * Visit the topics tree to remove matching subscriptions with clientID
     */
    public void removeForClient(String clientID) {
        subscriptions.removeClientSubscriptions(clientID);
        //persist the update
        m_sessionsStore.wipeSubscriptions(clientID);
    }

    public void deactivate(String clientID) {
        subscriptions.deactivate(clientID);
        //persist the update
        Set<Subscription> subs = subscriptions.findAllByClientID(clientID);
        m_sessionsStore.updateSubscriptions(clientID, subs);
    }

    public void activate(String clientID) {
        LOG.debug("Activating subscriptions for clientID <{}>", clientID);
        subscriptions.activate(clientID);
        //persist the update
        Set<Subscription> subs = subscriptions.findAllByClientID(clientID);
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
        subscriptions.matches(tokenQueue, matchingSubs);

        //remove the overlapping subscriptions, selecting ones with greatest qos
        Map<String, Subscription> subsForClient = new HashMap<>();
        for (Subscription sub : matchingSubs) {
            Subscription existingSub = subsForClient.get(sub.getClientId());
            //update the selected subscriptions if not present or if has a greater qos
            if (existingSub == null || existingSub.getRequestedQos().ordinal() < sub.getRequestedQos().ordinal()) {
                subsForClient.put(sub.getClientId(), sub);
            }
        }
        return /*matchingSubs*/new ArrayList<>(subsForClient.values());
    }

    public boolean contains(Subscription sub) {
        return !matches(sub.topicFilter).isEmpty();
    }

    public int size() {
        return subscriptions.size();
    }
    
    public String dumpTree() {
        DumpTreeVisitor visitor = new DumpTreeVisitor();
        bfsVisit(subscriptions, visitor, 0);
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
            Token subToken = null;
            for (; i< subscriptionTokens.size(); i++) {
                subToken = subscriptionTokens.get(i);
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
