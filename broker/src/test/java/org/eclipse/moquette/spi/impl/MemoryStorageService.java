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
package org.eclipse.moquette.spi.impl;

import java.util.*;

import org.eclipse.moquette.spi.ClientSession;
import org.eclipse.moquette.spi.IMatchingCondition;
import org.eclipse.moquette.spi.IMessagesStore;
import org.eclipse.moquette.spi.persistence.MemorySessionStore;
import org.eclipse.moquette.spi.impl.subscriptions.Subscription;

import org.eclipse.moquette.spi.ISessionsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.moquette.spi.impl.Utils.defaultGet;

/**
 */
public class MemoryStorageService implements IMessagesStore, ISessionsStore {
    
    private Map<String, String> m_retainedStore = new HashMap<>();
    //TODO move in a multimap because only Qos1 and QoS2 are stored here and they have messageID(key of secondary map)
    private Map<String, StoredMessage> m_persistentMessageStore = new HashMap<>();
    //maps clientID->[MessageId -> guid]
    private Map<String, Map<Integer, String>> m_inflightStore = new HashMap<>();
    //maps clientID->[guid*]
    private Map<String, Set<String>> m_enqueuedStore = new HashMap<>();
    //maps clientID->[messageID*]
    private Map<String, Set<Integer>> m_secondPhaseStore = new HashMap<>();
    private Map<String, Set<Integer>> m_inflightIDs = new HashMap<>();
    private Map<String, Map<Integer, String>> m_messageToGuids = new HashMap<>();
    private MemorySessionStore m_sessionsStore;
    
    private static final Logger LOG = LoggerFactory.getLogger(MemoryStorageService.class);

    public void initStore() {
        m_sessionsStore = new MemorySessionStore(this);
    }
    
    @Override
    public void cleanRetained(String topic) {
        m_retainedStore.remove(topic);
    }
    
    @Override
    public void storeRetained(String topic, String guid) {
        m_retainedStore.put(topic, guid);
    }

    @Override
    public Collection<StoredMessage> searchMatching(IMatchingCondition condition) {
        LOG.debug("searchMatching scanning all retained messages, presents are {}", m_retainedStore.size());

        List<StoredMessage> results = new ArrayList<>();

        for (Map.Entry<String, String> entry : m_retainedStore.entrySet()) {
            final String guid = entry.getValue();
            StoredMessage storedMsg = m_persistentMessageStore.get(guid);
            if (condition.match(entry.getKey())) {
                results.add(storedMsg);
            }
        }

        return results;
    }

    @Override
    public String storePublishForFuture(StoredMessage evt) {
        LOG.debug("storePublishForFuture store evt {}", evt);
        String guid = UUID.randomUUID().toString();
        evt.setGuid(guid);
        m_persistentMessageStore.put(guid, evt);
        HashMap<Integer, String> guids = (HashMap<Integer, String>) defaultGet(m_messageToGuids,
                evt.getClientID(), new HashMap<Integer, String>());
        guids.put(evt.getMessageID(), guid);
        return guid;
    }

    @Override
    public List<StoredMessage> listMessagesInSession(Collection<String> guids) {
        List<StoredMessage> ret = new ArrayList<>();
        for (String guid : guids) {
            ret.add(m_persistentMessageStore.get(guid));
        }
        return ret;
    }

    @Override
    public void dropMessagesInSession(String clientID) {
        m_persistentMessageStore.remove(clientID);
    }

    @Override
    public StoredMessage getMessageByGuid(String guid) {
        return m_persistentMessageStore.get(guid);
    }

    @Override
    public String mapToGuid(String clientID, int messageID) {
        HashMap<Integer, String> guids = (HashMap<Integer, String>) defaultGet(m_messageToGuids,
                clientID, new HashMap<Integer, String>());
        return guids.get(messageID);
    }

    /**
     * Return the next valid packetIdentifier for the given client session.
     * */
    @Override
    public int nextPacketID(String clientID) {
        Set<Integer> inFlightForClient = m_inflightIDs.get(clientID);
        if (inFlightForClient == null) {
            int nextPacketId = 1;
            inFlightForClient = new HashSet<>();
            inFlightForClient.add(nextPacketId);
            m_inflightIDs.put(clientID, inFlightForClient);
            return nextPacketId;
        }
        int maxId = Collections.max(inFlightForClient);
        int nextPacketId = (maxId + 1) % 0xFFFF;
        inFlightForClient.add(nextPacketId);
        return nextPacketId;
    }

    @Override
    public void addNewSubscription(Subscription newSubscription) {
        m_sessionsStore.addNewSubscription(newSubscription);
    }

    @Override
    public void removeSubscription(String topic, String clientID) {
        m_sessionsStore.removeSubscription(topic, clientID);
    }

    @Override
    public void wipeSubscriptions(String sessionID) {
        m_sessionsStore.wipeSubscriptions(sessionID);
    }

    @Override
    public void updateSubscriptions(String clientID, Set<Subscription> subscriptions) {
        m_sessionsStore.updateSubscriptions(clientID, subscriptions);
    }

    @Override
    public List<Subscription> listAllSubscriptions() {
        return m_sessionsStore.listAllSubscriptions();
    }

    @Override
    public boolean contains(String clientID) {
        return m_sessionsStore.contains(clientID);
    }

    @Override
    public ClientSession createNewSession(String clientID, boolean cleanSession) {
        return m_sessionsStore.createNewSession(clientID, cleanSession);
    }

    @Override
    public ClientSession sessionForClient(String clientID) {
        return m_sessionsStore.sessionForClient(clientID);
    }

    @Override
    public void activate(String clientID) {
        m_sessionsStore.activate(clientID);
    }

    @Override
    public void deactivate(String clientID) {
        m_sessionsStore.deactivate(clientID);
    }

    @Override
    public void inFlightAck(String clientID, int messageID) {
        Map<Integer, String> m = this.m_inflightStore.get(clientID);
        if (m == null) {
            LOG.error("Can't find the inFlight record for client <{}>", clientID);
            return;
        }
        m.remove(messageID);
    }

    @Override
    public void inFlight(String clientID, int messageID, String guid) {
        Map<Integer, String> m = this.m_inflightStore.get(clientID);
        if (m == null) {
            m = new HashMap<>();
        }
        m.put(messageID, guid);
        this.m_inflightStore.put(clientID, m);
    }

    @Override
    public void bindToDeliver(String guid, String clientID) {
        Set<String> guids = defaultGet(m_enqueuedStore, clientID, new HashSet<String>());
        guids.add(guid);
        m_enqueuedStore.put(clientID, guids);
    }

    @Override
    public Collection<String> enqueued(String clientID) {
        return defaultGet(m_enqueuedStore, clientID, new HashSet<String>());
    }

    @Override
    public void removeEnqueued(String clientID, String guid) {
        Set<String> guids = defaultGet(m_enqueuedStore, clientID, new HashSet<String>());
        guids.remove(guid);
        m_enqueuedStore.put(clientID, guids);
    }

    @Override
    public void secondPhaseAcknowledged(String clientID, int messageID) {
        Set<Integer> messageIDs = defaultGet(m_secondPhaseStore, clientID, new HashSet<Integer>());
        messageIDs.remove(messageID);
        m_secondPhaseStore.put(clientID, messageIDs);
    }

    @Override
    public void secondPhaseAckWaiting(String clientID, int messageID) {
        Set<Integer> messageIDs = defaultGet(m_secondPhaseStore, clientID, new HashSet<Integer>());
        messageIDs.add(messageID);
        m_secondPhaseStore.put(clientID, messageIDs);
    }
}
