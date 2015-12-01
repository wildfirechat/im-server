/*
 * Copyright (c) 2012-2015 The original author or authors
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
package io.moquette.spi.persistence;

import io.moquette.commons.Constants;
import static io.moquette.commons.Constants.*;
import io.moquette.server.IntegrationUtils;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.MemoryConfig;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.proto.messages.AbstractMessage;
import io.moquette.spi.impl.subscriptions.Subscription;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 *
 * @author andrea
 */
public class MapDBPersistentStoreTest {

    MapDBPersistentStore m_storageService;
    ISessionsStore m_sessionsStore;
    IMessagesStore m_messagesStore;

    @Before
    public void setUp() throws Exception {
        IntegrationUtils.cleanPersistenceFile(Constants.DEFAULT_PERSISTENT_PATH);
        Properties props = new Properties();
        props.setProperty(PERSISTENT_STORE_PROPERTY_NAME, DEFAULT_PERSISTENT_PATH);
        IConfig conf = new MemoryConfig(props);
        m_storageService = new MapDBPersistentStore(conf);
        m_storageService.initStore();
        m_messagesStore = m_storageService.messagesStore();
        m_sessionsStore = m_storageService.sessionsStore(m_messagesStore);
    }

    @After
    public void tearDown() {
        if (m_storageService != null) {
            m_storageService.close();
        }

        IntegrationUtils.cleanPersistenceFile(Constants.DEFAULT_PERSISTENT_PATH);
    }

    @Test
    public void overridingSubscriptions() {
        Subscription oldSubscription = new Subscription("FAKE_CLI_ID_1", "/topic", AbstractMessage.QOSType.MOST_ONE, false);
        m_sessionsStore.addNewSubscription(oldSubscription);
        Subscription overrindingSubscription = new Subscription("FAKE_CLI_ID_1", "/topic", AbstractMessage.QOSType.EXACTLY_ONCE, false);
        m_sessionsStore.addNewSubscription(overrindingSubscription);
        
        //Verify
        List<Subscription> subscriptions = m_sessionsStore.listAllSubscriptions();
        assertEquals(1, subscriptions.size());
        Subscription sub = subscriptions.get(0);
        assertEquals(overrindingSubscription.getRequestedQos(), sub.getRequestedQos());
    }

    @Test
    public void testNextPacketID_notExistingClientSession() {
        int packetId = m_messagesStore.nextPacketID("NOT_EXISTING_CLI");
        assertEquals(1, packetId);
    }

    @Test
    public void testNextPacketID_existingClientSession() {
        //Force creation of inflight map for the CLIENT session
        int packetId = m_messagesStore.nextPacketID("CLIENT");
        assertEquals(1, packetId);

        //request a second packetID
        packetId = m_messagesStore.nextPacketID("CLIENT");
        assertEquals(2, packetId);
    }

    @Test
    public void testNextPacketID() {
        //request a first ID

        int packetId = m_messagesStore.nextPacketID("CLIENT");
        m_sessionsStore.inFlight("CLIENT", packetId, "ABCDE"); //simulate an inflight
        assertEquals(1, packetId);

        //release the ID
        m_sessionsStore.inFlightAck("CLIENT", packetId);

        //request a second packetID, counter restarts from 0
        packetId = m_messagesStore.nextPacketID("CLIENT");
        assertEquals(1, packetId);
    }

    @Test
    public void testCloseShutdownCommitTask() throws InterruptedException {
        m_storageService.close();

        //verify the executor is shutdown
        assertTrue("Storage service scheduler can't be stopped in 3 seconds",
                m_storageService.m_scheduler.awaitTermination(3, TimeUnit.SECONDS));
        assertTrue(m_storageService.m_scheduler.isTerminated());
    }
}