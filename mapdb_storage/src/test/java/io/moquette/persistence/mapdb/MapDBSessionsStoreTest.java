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
package io.moquette.persistence.mapdb;

import io.moquette.BrokerConstants;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.MemoryConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.*;

import static io.moquette.persistence.mapdb.MapDBPersistentStoreTest.cleanPersistenceFile;
import static java.time.LocalDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

public class MapDBSessionsStoreTest {

    private static final Logger LOG = LoggerFactory.getLogger(MapDBSessionsStoreTest.class);

    MapDBPersistentStore storageService;

    private ScheduledExecutorService scheduler;

    @Before
    public void setUp() throws Exception {
        scheduler = Executors.newScheduledThreadPool(1);

        cleanPersistenceFile(BrokerConstants.DEFAULT_PERSISTENT_PATH);
        Properties props = new Properties();
        props.setProperty(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME, BrokerConstants.DEFAULT_PERSISTENT_PATH);
        IConfig conf = new MemoryConfig(props);
        storageService = new MapDBPersistentStore(conf, scheduler);
        storageService.initStore();
    }

    @After
    public void tearDown() {
        if (storageService != null) {
            storageService.close();
        }

        scheduler.shutdown();
        cleanPersistenceFile(BrokerConstants.DEFAULT_PERSISTENT_PATH);
    }

    @Test
    public void testQueryByExpirationDate() {
        MapDBSessionsStore sessionsStore = (MapDBSessionsStore) storageService.sessionsStore();
        LocalDateTime sessionCreationTime = LocalDateTime.of(2017, 10, 1, 10, 0, 0);
        sessionsStore.trackSessionClose(sessionCreationTime, "Sensor1");

        LocalDateTime session2CreationTime = LocalDateTime.of(2017, 11, 1, 10, 0, 0);
        sessionsStore.trackSessionClose(session2CreationTime, "Sensor2");

        //Exercise
        LocalDateTime queryPin = LocalDateTime.of(2017, 10, 20, 10, 0, 0);
        Set<String> sessions = sessionsStore.sessionOlderThan(queryPin);

        //Verify
        assertThat(sessions).contains("Sensor1");
    }

    @Test
    public void testListAllSessionsOlderThan() {
        MapDBSessionsStore sessionsStore = (MapDBSessionsStore) storageService.sessionsStore();
        LocalDateTime sessionCreationTime = LocalDateTime.of(2017, 10, 1, 10, 0, 0);
        sessionsStore.trackSessionClose(sessionCreationTime, "Sensor1");

        LocalDateTime session2CreationTime = LocalDateTime.of(2017, 10, 2, 10, 0, 0);
        sessionsStore.trackSessionClose(session2CreationTime, "Sensor2");

        //Exercise
        LocalDateTime queryPin = LocalDateTime.of(2017, 10, 20, 10, 0, 0);
        Set<String> sessions = sessionsStore.sessionOlderThan(queryPin);

        //Verify
        assertThat(sessions).contains("Sensor1", "Sensor2");
    }

    @Test
    public void testRetrieve1000ExpiredSessionPerformance() throws InterruptedException, ExecutionException, TimeoutException {
        MapDBSessionsStore sessionsStore = (MapDBSessionsStore) storageService.sessionsStore();

        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        final Future<?> future1 = executorService.submit(inserterForOffset(sessionsStore, 0));
        final Future<?> future2 = executorService.submit(inserterForOffset(sessionsStore, 5000));
        future1.get(10, TimeUnit.SECONDS);
        future2.get(10, TimeUnit.SECONDS);
        //prepare the fixture
//        for (int i = 0; i < 100000; i++) {
//            final LocalDateTime timePin = now().minusSeconds(1);
//            sessionsStore.trackSessionClose(timePin, "Sensor" + i);
//        }

        //Exercise
        LOG.warn("Start search");
        Set<String> sessions = sessionsStore.sessionOlderThan(now());
        LOG.warn("Finish search");

        //Verify
        assertThat(sessions.size()).isEqualTo(10000);
    }

    private Runnable inserterForOffset(MapDBSessionsStore sessionsStore, int start) {
        return () -> {
            //prepare the fixture
            for (int i = start; i < start + 5000; i++) {
                final LocalDateTime timePin = now().minusSeconds(1);
                sessionsStore.trackSessionClose(timePin, "Sensor" + i);
            }
        };
    }

}
