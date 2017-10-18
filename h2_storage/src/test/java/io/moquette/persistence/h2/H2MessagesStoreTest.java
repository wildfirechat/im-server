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
package io.moquette.persistence.h2;

import io.moquette.BrokerConstants;
import io.moquette.persistence.MessageStoreTCK;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.MemoryConfig;
import io.moquette.spi.impl.SessionsRepository;
import org.junit.After;
import org.junit.Before;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.assertFalse;

public class H2MessagesStoreTest extends MessageStoreTCK {

    H2PersistentStore storageService;

    private ScheduledExecutorService scheduler;

    @Before
    public void setUp() throws Exception {
        scheduler = Executors.newScheduledThreadPool(1);

        cleanPersistenceFile(BrokerConstants.DEFAULT_PERSISTENT_PATH);
        Properties props = new Properties();
        props.setProperty(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME, BrokerConstants.DEFAULT_PERSISTENT_PATH);
        IConfig conf = new MemoryConfig(props);
        storageService = new H2PersistentStore(conf, scheduler);
        storageService.initStore();
        messagesStore = storageService.messagesStore();
        sessionsStore = storageService.sessionsStore();
        this.sessionsRepository = new SessionsRepository(sessionsStore, null);
    }

    @After
    public void tearDown() {
        if (storageService != null) {
            storageService.close();
        }

        scheduler.shutdown();
        cleanPersistenceFile(BrokerConstants.DEFAULT_PERSISTENT_PATH);
    }

    public static void cleanPersistenceFile(String fileName) {
        File dbFile = new File(fileName);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        assertFalse(dbFile.exists());
    }
}
