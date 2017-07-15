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

import io.moquette.spi.ISubscriptionsStore.ClientTopicCouple;

import java.util.*;

class CNode {

    Token token;
    private List<INode> children;
    Set<ClientTopicCouple> subscriptions = new HashSet<>();

    //private int subtreeSubscriptions;

    CNode() {
        this.children = new ArrayList<>();
        this.subscriptions = new HashSet<>();
    }

    //Copy constructor
    private CNode(Token token, List<INode> children, Set<ClientTopicCouple> subscriptions) {
        this.token = token;
        this.children = children;
        this.subscriptions = subscriptions;
    }

    boolean anyChildrenMatch(Token token) {
        for (INode iNode : children) {
            final CNode child = iNode.mainNode();
            if (child.equals(token)) {
                return true;
            }
        }
        return false;
    }

    List<INode> allChildren() {
        return this.children;
    }

    INode childOf(Token token) {
        for (INode iNode : children) {
            final CNode child = iNode.mainNode();
            if (child.equals(token)) {
                return iNode;
            }
        }
        throw new IllegalArgumentException("Asked for a token that doesn't exists in any child [" + token + "]");
    }

    private boolean equals(Token token) {
        if (this.token == null) {
            return false;
        }
        return this.token.equals(token);
    }

    CNode copy() {
        return new CNode(this.token, this.children, this.subscriptions);
    }

    public void add(INode newINode) {
        this.children.add(newINode);
    }

    CNode addSubscription(String clientId, Topic topic) {
        this.subscriptions.add(new ClientTopicCouple(clientId, topic));
        return this;
    }

    /**
     * @return true iff the subscriptions contained in this node are owned by clientId
     * */
    boolean containsOnly(String clientId) {
        for (ClientTopicCouple sub : this.subscriptions) {
            if (!sub.clientID.equals(clientId)) {
                return false;
            }
        }
        return true;
    }

    //TODO this is equivalent to negate(containsOnly(clientId))
    public boolean contains(String clientId) {
        for (ClientTopicCouple sub : this.subscriptions) {
            if (sub.clientID.equals(clientId)) {
                return true;
            }
        }
        return false;
    }

    void removeSubscriptionsFor(String clientId) {
        Set<ClientTopicCouple> toRemove = new HashSet<>();
        for (ClientTopicCouple sub : this.subscriptions) {
            if (sub.clientID.equals(clientId)) {
                toRemove.add(sub);
            }
        }
        this.subscriptions.removeAll(toRemove);
    }

//    public List<INode> childrenMatching(Token token) {
//        List<INode> res = new ArrayList<>();
//        for (INode iNode : children) {
//            final CNode child = iNode.mainNode();
//            if (child.equals(token) || (child.token == Token.MULTI || child.token == Token.SINGLE)) {
//                res.add(iNode);
//            }
//        }
//        return res;
//    }
}
