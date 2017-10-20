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

import java.util.concurrent.atomic.AtomicReference;

class INode {
    private AtomicReference<CNode> mainNode = new AtomicReference<>();

    INode(CNode mainNode) {
        this.mainNode.set(mainNode);
        if (mainNode instanceof TNode) { // this should never happen
            throw new IllegalStateException("TNode should not be set on mainNnode");
        }
    }

    boolean compareAndSet(CNode old, CNode newNode) {
        return mainNode.compareAndSet(old, newNode);
    }

    boolean compareAndSet(CNode old, TNode newNode) {
        return mainNode.compareAndSet(old, newNode);
    }

    CNode mainNode() {
        return this.mainNode.get();
    }

    boolean isTombed() {
        return this.mainNode() instanceof TNode;
    }
}
