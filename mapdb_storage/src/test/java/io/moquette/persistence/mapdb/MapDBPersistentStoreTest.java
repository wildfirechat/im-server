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
import io.moquette.persistence.MessageStoreTCK;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.MemoryConfig;
import io.moquette.spi.IMessagesStore.StoredMessage;
import io.moquette.spi.impl.SessionsRepository;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 *
 * @author andrea
 */
public class MapDBPersistentStoreTest extends MessageStoreTCK {

    MapDBPersistentStore m_storageService;

    private ScheduledExecutorService scheduler;

    @Before
    public void setUp() throws Exception {
        scheduler = Executors.newScheduledThreadPool(1);

        cleanPersistenceFile(BrokerConstants.DEFAULT_PERSISTENT_PATH);
        Properties props = new Properties();
        props.setProperty(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME, BrokerConstants.DEFAULT_PERSISTENT_PATH);
        IConfig conf = new MemoryConfig(props);
        m_storageService = new MapDBPersistentStore(conf, scheduler);
        m_storageService.initStore();
        messagesStore = m_storageService.messagesStore();
        sessionsStore = m_storageService.sessionsStore();
        this.sessionsRepository = new SessionsRepository(sessionsStore, null);
    }

    @After
    public void tearDown() {
        if (m_storageService != null) {
            m_storageService.close();
        }

        scheduler.shutdown();
        cleanPersistenceFile(BrokerConstants.DEFAULT_PERSISTENT_PATH);
    }

    static void cleanPersistenceFile(String fileName) {
        File dbFile = new File(fileName);
        if (dbFile.exists()) {
            dbFile.delete();
            new File(fileName + ".p").delete();
            new File(fileName + ".t").delete();
        }
        assertFalse(dbFile.exists());
    }

    @Test
    public void testNextPacketID_notExistingClientSession() {
        int packetId = sessionsStore.nextPacketID("NOT_EXISTING_CLI");
        assertEquals(1, packetId);
    }

    @Test
    public void testNextPacketID_existingClientSession() {
        // Force creation of inflight map for the CLIENT session
        int packetId = sessionsStore.nextPacketID("CLIENT");
        assertEquals(1, packetId);

        // request a second packetID
        packetId = sessionsStore.nextPacketID("CLIENT");
        assertEquals(2, packetId);
    }

    @Test
    public void testNextPacketID() {
        StoredMessage msgStored = new StoredMessage("Hello".getBytes(), MqttQoS.AT_LEAST_ONCE, "/topic");
        msgStored.setClientID(TEST_CLIENT);

        // request a first ID
        int packetId = sessionsStore.nextPacketID("CLIENT");
        sessionsStore.inFlight("CLIENT", packetId, msgStored); // simulate an inflight
        assertEquals(1, packetId);

        // release the ID
        sessionsStore.inFlightAck("CLIENT", packetId);

        // request a second packetID, counter restarts from 0
        packetId = sessionsStore.nextPacketID("CLIENT");
        assertEquals(1, packetId);
    }

    @Test
    public void testCloseShutdownCommitTask() throws InterruptedException {
        m_storageService.close();

        // verify the executor is shutdown
        assertTrue("Storage service scheduler can't be stopped in 3 seconds",
                m_storageService.m_scheduler.awaitTermination(3, TimeUnit.SECONDS));
        assertTrue(m_storageService.m_scheduler.isTerminated());
    }
}
