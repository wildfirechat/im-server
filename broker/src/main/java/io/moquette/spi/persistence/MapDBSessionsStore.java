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

import io.moquette.spi.ClientSession;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.Utils;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.persistence.MapDBPersistentStore.PersistentSession;
import org.mapdb.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * ISessionsStore implementation backed by MapDB.
 *
 * @author andrea
 */
class MapDBSessionsStore implements ISessionsStore {

    private static final Logger LOG = LoggerFactory.getLogger(MapDBSessionsStore.class);

    //maps clientID->[MessageId -> guid]
    private ConcurrentMap<String, Map<Integer, String>> m_inflightStore;
    //map clientID <-> set of currently in flight packet identifiers
    private Map<String, Set<Integer>> m_inFlightIds;
    //persistent Map of clientID, set of Subscriptions
    private ConcurrentMap<String, Set<Subscription>> m_persistentSubscriptions;
    private ConcurrentMap<String, PersistentSession> m_persistentSessions;
    //maps clientID->[guid*], insertion order cares, it's queue
    private ConcurrentMap<String, List<String>> m_enqueuedStore;
    //maps clientID->[messageID*]
    private ConcurrentMap<String, Set<Integer>> m_secondPhaseStore;

    private final DB m_db;
    private final IMessagesStore m_messagesStore;

    MapDBSessionsStore(DB db, IMessagesStore messagesStore) {
        m_db = db;
        m_messagesStore = messagesStore;
    }

    @Override
    public void initStore() {
        m_inflightStore = m_db.getHashMap("inflight");
        m_inFlightIds = m_db.getHashMap("inflightPacketIDs");
        m_persistentSubscriptions = m_db.getHashMap("subscriptions");
        m_persistentSessions = m_db.getHashMap("sessions");
        m_enqueuedStore = m_db.getHashMap("sessionQueue");
        m_secondPhaseStore = m_db.getHashMap("secondPhase");
    }

    @Override
    public void addNewSubscription(Subscription newSubscription) {
        LOG.debug("addNewSubscription invoked with subscription {}", newSubscription);
        final String clientID = newSubscription.getClientId();
        if (!m_persistentSubscriptions.containsKey(clientID)) {
            LOG.debug("subscriptions for clientID <{}> not present, empty subscriptions set", clientID);
            m_persistentSubscriptions.put(clientID, new HashSet<Subscription>());
        }

        Set<Subscription> subs = m_persistentSubscriptions.get(clientID);
        if (!subs.contains(newSubscription)) {
            LOG.debug("updating <{}> subscriptions set with new subscription", clientID);
            //TODO check the subs doesn't contain another subscription to the same topic with different
            Subscription existingSubscription = null;
            for (Subscription scanSub : subs) {
                if (newSubscription.getTopicFilter().equals(scanSub.getTopicFilter())) {
                    existingSubscription = scanSub;
                    break;
                }
            }
            if (existingSubscription != null) {
                subs.remove(existingSubscription);
            }
            subs.add(newSubscription);
            m_persistentSubscriptions.put(clientID, subs);
            LOG.debug("clientID {} subscriptions set now is {}", clientID, subs);
        }
    }

    @Override
    public void removeSubscription(String topic, String clientID) {
        LOG.debug("removeSubscription topic filter: {} for clientID: {}", topic, clientID);
        if (!m_persistentSubscriptions.containsKey(clientID)) {
            return;
        }
        Set<Subscription> clientSubscriptions = m_persistentSubscriptions.get(clientID);
        //search for the subscription to remove
        Subscription toBeRemoved = null;
        for (Subscription sub : clientSubscriptions) {
            if (sub.getTopicFilter().equals(topic)) {
                toBeRemoved = sub;
                break;
            }
        }

        if (toBeRemoved != null) {
            clientSubscriptions.remove(toBeRemoved);
        }
        m_persistentSubscriptions.put(clientID, clientSubscriptions);
        if (LOG.isTraceEnabled()) {
            LOG.trace("persisted subscriptions {}", m_persistentSubscriptions);
        }
    }

    @Override
    public void wipeSubscriptions(String clientID) {
        m_persistentSubscriptions.remove(clientID);
    }

    @Override
    public void updateSubscriptions(String clientID, Set<Subscription> subscriptions) {
        m_persistentSubscriptions.put(clientID, subscriptions);
    }

    @Override
    public List<Subscription> listAllSubscriptions() {
        List<Subscription> allSubscriptions = new ArrayList<>();
        for (Map.Entry<String, Set<Subscription>> entry : m_persistentSubscriptions.entrySet()) {
            allSubscriptions.addAll(entry.getValue());
        }
        LOG.debug("retrieveAllSubscriptions returning subs {}", allSubscriptions);
        return allSubscriptions;
    }

    @Override
    public boolean contains(String clientID) {
        return m_persistentSubscriptions.containsKey(clientID);
    }

    @Override
    public ClientSession createNewSession(String clientID, boolean cleanSession) {
        LOG.debug("createNewSession for client <{}> with clean flag <{}>", clientID, cleanSession);
        if (m_persistentSessions.containsKey(clientID)) {
            LOG.error("already exists a session for client <{}>, bad condition", clientID);
            throw new IllegalArgumentException("Can't create a session with the ID of an already existing" + clientID);
        }
        LOG.debug("clientID {} is a newcome, creating it's empty subscriptions set", clientID);
        m_persistentSubscriptions.put(clientID, new HashSet<Subscription>());
        m_persistentSessions.putIfAbsent(clientID, new PersistentSession(cleanSession, false));
        return new ClientSession(clientID, m_messagesStore, this, cleanSession);
    }

    @Override
    public ClientSession sessionForClient(String clientID) {
        if (!m_persistentSessions.containsKey(clientID)) {
            return null;
        }

        PersistentSession storedSession = m_persistentSessions.get(clientID);
        ClientSession clientSession = new ClientSession(clientID, m_messagesStore, this, storedSession.cleanSession);
        if (storedSession.active) {
            clientSession.activate();
        }
        return clientSession;
    }

    @Override
    public void activate(String clientID) {
        activationHelper(clientID, true);
    }

    @Override
    public void deactivate(String clientID) {
        activationHelper(clientID, false);
    }

    private void activationHelper(String clientID, boolean activation) {
        PersistentSession storedSession = m_persistentSessions.get(clientID);
        if (storedSession == null) {
            throw new IllegalStateException((activation ? "activating" : "deactivating") + " a session never stored/created, clientID <"+ clientID + ">", null);
        }
        PersistentSession newStoredSession = new PersistentSession(storedSession.cleanSession, activation);
        m_persistentSessions.put(clientID, newStoredSession);
    }

    @Override
    public void inFlightAck(String clientID, int messageID) {
        Map<Integer, String> m = this.m_inflightStore.get(clientID);
        if (m == null) {
            LOG.error("Can't find the inFlight record for client <{}>", clientID);
            return;
        }
        m.remove(messageID);

        //remove from the ids store
        Set<Integer> inFlightForClient = this.m_inFlightIds.get(clientID);
        if (inFlightForClient != null) {
            inFlightForClient.remove(messageID);
        }
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
        List<String> guids = Utils.defaultGet(m_enqueuedStore, clientID, new ArrayList<String>());
        guids.add(guid);
        m_enqueuedStore.put(clientID, guids);
    }

    @Override
    public Collection<String> enqueued(String clientID) {
        return Utils.defaultGet(m_enqueuedStore, clientID, new ArrayList<String>());
    }

    @Override
    public void removeEnqueued(String clientID, String guid) {
        List<String> guids = Utils.defaultGet(m_enqueuedStore, clientID, new ArrayList<String>());
        guids.remove(guid);
        m_enqueuedStore.put(clientID, guids);
    }

    @Override
    public void secondPhaseAcknowledged(String clientID, int messageID) {
        Set<Integer> messageIDs = Utils.defaultGet(m_secondPhaseStore, clientID, new HashSet<Integer>());
        messageIDs.remove(messageID);
        m_secondPhaseStore.put(clientID, messageIDs);
    }

    @Override
    public void secondPhaseAckWaiting(String clientID, int messageID) {
        Set<Integer> messageIDs = Utils.defaultGet(m_secondPhaseStore, clientID, new HashSet<Integer>());
        messageIDs.add(messageID);
        m_secondPhaseStore.put(clientID, messageIDs);
    }

    @Override
    public String mapToGuid(String clientID, int messageID) {
        ConcurrentMap<Integer, String> messageIdToGuid = m_db.getHashMap(messageId2GuidsMapName(clientID));
        return messageIdToGuid.get(messageID);
    }

    static String messageId2GuidsMapName(String clientID) {
        return "guidsMapping_" + clientID;
    }
}
