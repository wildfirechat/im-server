package org.dna.mqtt.moquette.messaging.spi.impl;

import java.io.File;
import java.util.List;
import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.Subscription;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.server.Server;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author andrea
 */
public class HawtDBStorageServiceTest {

    HawtDBStorageService m_storageService;
        
    @Before
    public void setUp() throws Exception {
        File dbFile = new File(Server.STORAGE_FILE_PATH);
        assertFalse(String.format("The DB storagefile %s already exists", Server.STORAGE_FILE_PATH), dbFile.exists());
        
        m_storageService = new HawtDBStorageService();
        m_storageService.initStore();
    }

    @After
    public void tearDown() {
        if (m_storageService != null) {
            m_storageService.close();
        }
        
        File dbFile = new File(Server.STORAGE_FILE_PATH);
        if (dbFile.exists()) {
            dbFile.delete();
        }
        assertFalse(dbFile.exists());
    }

    @Test
    public void overridingSubscriptions() {
        Subscription oldSubscription = new Subscription("FAKE_CLI_ID_1", "/topic", AbstractMessage.QOSType.MOST_ONE, false);
        m_storageService.addNewSubscription(oldSubscription, oldSubscription.getClientId());
        Subscription overrindingSubscription = new Subscription("FAKE_CLI_ID_1", "/topic", AbstractMessage.QOSType.EXACTLY_ONCE, false);
        m_storageService.addNewSubscription(overrindingSubscription, overrindingSubscription.getClientId());
        
        //Verify
        List<Subscription> subscriptions = m_storageService.retrieveAllSubscriptions();
        assertEquals(1, subscriptions.size());
        Subscription sub = subscriptions.get(0);
        assertEquals(overrindingSubscription.getRequestedQos(), sub.getRequestedQos());
    }
}