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

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

import io.moquette.persistence.MemoryStorageService;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.SessionsRepository;
import io.netty.handler.codec.mqtt.MqttQoS;

public final class PerformanceTest {

    // Set CPU governor and max frequency to lowest value for this test
    /* debian:
       cpufreq-set -g performance -u 800MHz -c 0
       cpufreq-set -g performance -u 800MHz -c 1
       cpufreq-set -g performance -u 800MHz -c 2
       cpufreq-set -g performance -u 800MHz -c 3
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        ISubscriptionsDirectory store = new CTrieSubscriptionDirectory();
        MemoryStorageService memStore = new MemoryStorageService(null, null);
        ISessionsStore aSessionsStore = memStore.sessionsStore();
        SessionsRepository sessionsRepository = new SessionsRepository(aSessionsStore, null);
        store.init(sessionsRepository);

        int times = 10000;

        int users = 500;
        int devices = 5;
        int values = 3;
        int subs = 2;
        int n = users * devices * values * subs;

        ThreadMXBean b = ManagementFactory.getThreadMXBean();

        Topic[] topics = new Topic[n];
        for (int i = 0; i < n; i++) {
            topics[i] = new Topic(
                    (i % users) + "/" + ((i / users) % devices) + "/" + ((i / (users * devices)) % values) + "/"
                            + ((i / (users * devices * values)) % subs));

            Subscription sub = new Subscription("CLI_ID_" + (i % users), topics[i], MqttQoS.AT_MOST_ONCE);
            aSessionsStore.subscriptionStore().addNewSubscription(sub);
            store.add(sub);
        }

        long min = Long.MAX_VALUE;
        //AtomicLong min = new AtomicLong(Long.MAX_VALUE);

        System.out.println("Store size: " + store.size() + " " + n);

        // warm up
        for (int i = 0; i < n; i++) {
            store.matches(topics[i]);
        }

        System.out.println("press return for start");
        System.in.read();

//        IntStream.range(0, times)
//            .parallel()
//            .forEach(j -> {

        for (int j = 0; j < times; j++) {

            System.gc();
            Thread.yield();

            long time = b.getCurrentThreadCpuTime();
            for (int i = 0; i < n; i++) {
                store.matches(topics[i]);
            }
            long time2 = b.getCurrentThreadCpuTime() - time;
            min = Math.min(min, time2);
//            min.updateAndGet(c -> Math.min(c, time2));
            System.out.println(".." + j + ": " + time2 + " min:" + min);
        }
//            });

        System.out.println("min:" + min);
    }

    private PerformanceTest() {
    }
}
