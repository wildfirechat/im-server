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

import java.util.concurrent.atomic.AtomicInteger;

class SubscriptionCounterVisitor implements CTrie.IVisitor<Integer> {

    private AtomicInteger accumulator = new AtomicInteger(0);

    @Override
    public void visit(CNode node, int deep) {
        accumulator.addAndGet(node.subscriptions.size());
    }

    @Override
    public Integer getResult() {
        return accumulator.get();
    }
}
