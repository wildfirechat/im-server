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
package io.moquette.persistence;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;

class H2PersistentQueue<T> extends AbstractQueue<T> {

    private final MVMap<Long, T> queueMap;
    private final MVMap<String, Long> metadataMap;
    private final AtomicLong head;
    private final AtomicLong tail;

    H2PersistentQueue(MVStore store, String queueName) {
        if (queueName == null || queueName.isEmpty()) {
            throw new IllegalArgumentException("queueName parameter can't be empty or null");
        }
        this.queueMap = store.openMap("queue_" + queueName);
        this.metadataMap = store.openMap("queue_" + queueName + "_meta");

        //setup head index
        long headIdx = 0L;
        if (this.metadataMap.containsKey("head")) {
            headIdx = this.metadataMap.get("head");
        } else {
            this.metadataMap.put("head", headIdx);
        }
        this.head = new AtomicLong(headIdx);

        //setup tail index
        long tailIdx = 0L;
        if (this.metadataMap.containsKey("tail")) {
            tailIdx = this.metadataMap.get("tail");
        } else {
            this.metadataMap.put("tail", tailIdx);
        }
        this.tail = new AtomicLong(tailIdx);
    }

    static void dropQueue(MVStore store, String queueName) {
        store.removeMap(store.openMap("queue_" + queueName));
        store.removeMap(store.openMap("queue_" + queueName + "_meta"));
    }

    @Override
    public Iterator<T> iterator() {
        return null;
    }

    @Override
    public int size() {
        return this.head.intValue() - this.tail.intValue();
    }

    @Override
    public boolean offer(T t) {
        if (t == null) {
            throw new NullPointerException("Inserted element can't be null");
        }
        final long nextHead = head.getAndIncrement();
        this.queueMap.put(nextHead, t);
        this.metadataMap.put("head", nextHead + 1);
        return true;
    }

    @Override
    public T poll() {
        if (head.equals(tail)) {
            return null;
        }
        final long nextTail = tail.getAndIncrement();
        final T tail = this.queueMap.get(nextTail);
        queueMap.remove(nextTail);
        this.metadataMap.put("tail", nextTail + 1);
        return tail;
    }

    @Override
    public T peek() {
        if (head.equals(tail)) {
            return null;
        }
        return this.queueMap.get(tail.get());
    }

}
