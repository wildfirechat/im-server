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
package io.moquette.broker.subscriptions;

class TNode extends CNode {

    @Override
    INode childOf(Token token) {
        throw new IllegalStateException("Can't be invoked on TNode");
    }

    @Override
    CNode copy() {
        throw new IllegalStateException("Can't be invoked on TNode");
    }

    @Override
    public void add(INode newINode) {
        throw new IllegalStateException("Can't be invoked on TNode");
    }

    @Override
    CNode addSubscription(Subscription newSubscription) {
        throw new IllegalStateException("Can't be invoked on TNode");
    }

    @Override
    boolean containsOnly(String clientId) {
        throw new IllegalStateException("Can't be invoked on TNode");
    }

    @Override
    public boolean contains(String clientId) {
        throw new IllegalStateException("Can't be invoked on TNode");
    }

    @Override
    void removeSubscriptionsFor(String clientId) {
        throw new IllegalStateException("Can't be invoked on TNode");
    }

    @Override
    boolean anyChildrenMatch(Token token) {
        return false;
    }
}
