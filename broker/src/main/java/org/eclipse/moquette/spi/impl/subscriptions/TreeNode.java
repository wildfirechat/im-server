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

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

class TreeNode {

    private class ClientIDComparator implements Comparator<Subscription> {

        public int compare(Subscription o1, Subscription o2) {
            return o1.getClientId().compareTo(o2.getClientId());
        }
        
    }

    TreeNode m_parent;
    Token m_token;
    List<TreeNode> m_children = new ArrayList<>();
    List<Subscription> m_subscriptions = new ArrayList<>();

    TreeNode(TreeNode parent) {
        this.m_parent = parent;
    }

    Token getToken() {
        return m_token;
    }

    void setToken(Token topic) {
        this.m_token = topic;
    }

    void addSubscription(Subscription s) {
        //avoid double registering for same clientID, topic and QoS
        if (m_subscriptions.contains(s)) {
            return;
        }
        //remove existing subscription for same client and topic but different QoS
        Comparator<Subscription> comparator = new ClientIDComparator();
        Collections.sort(m_subscriptions, comparator);
        int existingSubIdx = Collections.binarySearch(m_subscriptions, s, comparator);
        if (existingSubIdx >= 0) {
            m_subscriptions.remove(existingSubIdx);
        }
        
        m_subscriptions.add(s);
    }

    void addChild(TreeNode child) {
        m_children.add(child);
    }

    boolean isLeaf() {
        return m_children.isEmpty();
    }

    /**
     * Search for children that has the specified token, if not found return
     * null;
     */
    TreeNode childWithToken(Token token) {
        for (TreeNode child : m_children) {
            if (child.getToken().equals(token)) {
                return child;
            }
        }

        return null;
    }

    List<Subscription> subscriptions() {
        return m_subscriptions;
    }

    void matches(Queue<Token> tokens, List<Subscription> matchingSubs) {
        Token t = tokens.poll();

        //check if t is null <=> tokens finished
        if (t == null) {
            matchingSubs.addAll(m_subscriptions);
            //check if it has got a MULTI child and add its subscriptions
            for (TreeNode n : m_children) {
                if (n.getToken() == Token.MULTI || n.getToken() == Token.SINGLE) {
                    matchingSubs.addAll(n.subscriptions());
                }
            }

            return;
        }

        //we are on MULTI, than add subscriptions and return
        if (m_token == Token.MULTI) {
            matchingSubs.addAll(m_subscriptions);
            return;
        }

        for (TreeNode n : m_children) {
            if (n.getToken().match(t)) {
                //Create a copy of token, else if navigate 2 sibling it
                //consumes 2 elements on the queue instead of one
                n.matches(new LinkedBlockingQueue<>(tokens), matchingSubs);
                //TODO don't create a copy n.matches(tokens, matchingSubs);
            }
        }
    }

    /**
     * Return the number of registered subscriptions
     */
    int size() {
        int res = m_subscriptions.size();
        for (TreeNode child : m_children) {
            res += child.size();
        }
        return res;
    }

    void removeClientSubscriptions(String clientID) {
        //collect what to delete and then delete to avoid ConcurrentModification
        List<Subscription> subsToRemove = new ArrayList<>();
        for (Subscription s : m_subscriptions) {
            if (s.clientId.equals(clientID)) {
                subsToRemove.add(s);
            }
        }

        for (Subscription s : subsToRemove) {
            m_subscriptions.remove(s);
        }

        //go deep
        for (TreeNode child : m_children) {
            child.removeClientSubscriptions(clientID);
        }
    }

    /**
     * Deactivate all topic subscriptions for the given clientID.
     * */
    void deactivate(String clientID) {
        for (Subscription s : m_subscriptions) {
            if (s.clientId.equals(clientID)) {
                s.setActive(false);
            }
        }

        //go deep
        for (TreeNode child : m_children) {
            child.deactivate(clientID);
        }
    }

    /**
     * Activate all topic subscriptions for the given clientID.
     * */
    public void activate(String clientID) {
        for (Subscription s : m_subscriptions) {
            if (s.clientId.equals(clientID)) {
                s.setActive(true);
            }
        }

        //go deep
        for (TreeNode child : m_children) {
            child.activate(clientID);
        }
    }

    /**
     * @return the set of subscriptions for the given client.
     * */
    Set<Subscription> findAllByClientID(String clientID) {
        Set<Subscription> subs = new HashSet<>();
        for (Subscription s : m_subscriptions) {
            if (s.clientId.equals(clientID)) {
                subs.add(s);
            }
        }
        //go deep
        for (TreeNode child : m_children) {
            subs.addAll(child.findAllByClientID(clientID));
        }
        return subs;
    }
}
