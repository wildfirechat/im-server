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

import io.moquette.persistence.PersistentSession;
import io.moquette.spi.IMessagesStore.StoredMessage;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.ISubscriptionsStore;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;
import org.mapdb.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;

/**
 * ISessionsStore implementation backed by MapDB.
 */
class MapDBSessionsStore implements ISessionsStore, ISubscriptionsStore {

    private static final Logger LOG = LoggerFactory.getLogger(MapDBSessionsStore.class);

    // maps clientID->[MessageId -> msg]
    private ConcurrentMap<String, ConcurrentMap<Integer, StoredMessage>> outboundFlightMessages;
    // map clientID <-> set of currently in flight packet identifiers
    private Map<String, Set<Integer>> m_inFlightIds;
    private ConcurrentMap<String, PersistentSession> m_persistentSessions;
    // maps clientID->[MessageId -> guid]
    private ConcurrentMap<String, ConcurrentMap<Integer, StoredMessage>> m_secondPhaseStore;

    private final DB m_db;
    private ConcurrentNavigableMap<LocalDateTime, Set<String>> sessionsClosingTimes;

    MapDBSessionsStore(DB db) {
        m_db = db;
    }

    @Override
    public void initStore() {
        outboundFlightMessages = m_db.getHashMap("outboundFlight");
        m_inFlightIds = m_db.getHashMap("inflightPacketIDs");
        m_persistentSessions = m_db.getHashMap("sessions");
        m_secondPhaseStore = m_db.getHashMap("secondPhase");
        this.sessionsClosingTimes = m_db.getTreeMap("sessionsCreationTimes");
    }

    @Override
    public ISubscriptionsStore subscriptionStore() {
        return this;
    }

    @Override
    public void addNewSubscription(Subscription newSubscription) {
        LOG.info("Adding new subscription. ClientId={}, topics={}", newSubscription.getClientId(),
            newSubscription.getTopicFilter());
        final String clientID = newSubscription.getClientId();
        m_db.getHashMap("subscriptions_" + clientID).put(newSubscription.getTopicFilter(), newSubscription);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Subscription has been added. ClientId={}, topics={}, clientSubscriptions={}",
                newSubscription.getClientId(), newSubscription.getTopicFilter(),
                m_db.getHashMap("subscriptions_" + clientID));
        }
    }

    @Override
    public void removeSubscription(Topic topicFilter, String clientID) {
        LOG.info("Removing subscription. ClientId={}, topics={}", clientID, topicFilter);
        if (!m_db.exists("subscriptions_" + clientID)) {
            return;
        }
        m_db.getHashMap("subscriptions_" + clientID).remove(topicFilter);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Subscription has been removed. ClientId={}, topics={}, clientSubscriptions={}", clientID,
                topicFilter, m_db.getHashMap("subscriptions_" + clientID));
        }
    }

    @Override
    public void wipeSubscriptions(String clientID) {
        LOG.info("Wiping subscriptions. CId={}", clientID);
        m_db.delete("subscriptions_" + clientID);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Subscriptions have been removed. ClientId={}, clientSubscriptions={}", clientID,
            m_db.getHashMap("subscriptions_" + clientID));
        }
    }

    @Override
    public List<Subscription> listAllSubscriptions() {
        LOG.debug("Retrieving existing subscriptions...");
        List<Subscription> subscriptions = new ArrayList<>();
        for (String clientID : m_persistentSessions.keySet()) {
            ConcurrentMap<Topic, Subscription> clientSubscriptions = m_db.getHashMap("subscriptions_" + clientID);
            subscriptions.addAll(clientSubscriptions.values());
        }
        LOG.debug("Existing subscriptions has been retrieved Result={}", subscriptions);
        return subscriptions;
    }

    @Override
    public Collection<Subscription> listClientSubscriptions(String clientID) {
        ConcurrentMap<Topic, Subscription> clientSubscriptions = m_db.getHashMap("subscriptions_" + clientID);
        if (clientSubscriptions == null) {
            throw new IllegalStateException("Asking for subscriptions of not persisted client: " + clientID);
        }
        return clientSubscriptions.values();
    }

    @Override
    public Subscription reload(Subscription subcription) {
        ConcurrentMap<Topic, Subscription> clientSubscriptions = m_db.getHashMap("subscriptions_" + subcription.getClientId());
        LOG.debug("Retrieving subscriptions. CId={}, subscriptions={}", subcription.getClientId(), clientSubscriptions);
        return clientSubscriptions.get(subcription.getTopicFilter());
    }

    @Override
    public boolean contains(String clientID) {
        return m_persistentSessions.containsKey(clientID);
    }

    @Override
    public void createNewDurableSession(String clientID) {
        m_persistentSessions.putIfAbsent(clientID, new PersistentSession(clientID, false));
    }

    @Override
    public void removeDurableSession(String clientId) {
        this.m_persistentSessions.remove(clientId);
        this.wipeSubscriptions(clientId);
    }

    @Override
    public void updateCleanStatus(String clientId, boolean newCleanStatus) {
        PersistentSession updatedSession = new PersistentSession(clientId, newCleanStatus);
        m_persistentSessions.put(clientId, updatedSession);
    }

    @Override
    public PersistentSession loadSessionByKey(String clientID) {
        return this.m_persistentSessions.get(clientID);
    }

    @Override
    public Collection<PersistentSession> listAllSessions() {
        return this.m_persistentSessions.values();
    }

    /**
     * Return the next valid packetIdentifier for the given client session.
     */
    @Override
    public int nextPacketID(String clientID) {
        LOG.debug("Generating next packet ID CId={}", clientID);
        Set<Integer> inFlightForClient = this.m_inFlightIds.get(clientID);
        if (inFlightForClient == null) {
            int nextPacketId = 1;
            inFlightForClient = Collections.newSetFromMap(new ConcurrentHashMap<>());
            inFlightForClient.add(nextPacketId);
            this.m_inFlightIds.put(clientID, inFlightForClient);
            return nextPacketId;

        }

        int maxId = inFlightForClient.isEmpty() ? 0 : Collections.max(inFlightForClient);
        int nextPacketId = (maxId % 0xFFFF) + 1;
        inFlightForClient.add(nextPacketId);
        LOG.debug("Next packet ID has been generated CId={}, result={}", clientID, nextPacketId);
        return nextPacketId;
    }

    @Override
    public StoredMessage inFlightAck(String clientID, int messageID) {
        LOG.debug("Acknowledging inflight message CId={}, messageId={}", clientID, messageID);
        ConcurrentMap<Integer, StoredMessage> m = this.outboundFlightMessages.get(clientID);
        if (m == null) {
            LOG.error("Can't find the inFlight record for client <{}>", clientID);
            throw new RuntimeException("Can't find the inFlight record for client <" + clientID + ">");
        }
        StoredMessage msg = m.remove(messageID);
        this.outboundFlightMessages.put(clientID, m);

        // remove from the ids store
        Set<Integer> inFlightForClient = this.m_inFlightIds.get(clientID);
        if (inFlightForClient != null) {
            inFlightForClient.remove(messageID);
        }
        return msg;
    }

    @Override
    public void inFlight(String clientID, int messageID, StoredMessage msg) {
        ConcurrentMap<Integer, StoredMessage> messages = outboundFlightMessages.get(clientID);
        if (messages == null) {
            messages = new ConcurrentHashMap<>();
        }
        messages.put(messageID, msg);
        outboundFlightMessages.put(clientID, messages);
    }

    @Override
    public Queue<StoredMessage> queue(String clientID) {
        LOG.info("Queuing pending message. CId={}, guid={}", clientID);
        return this.m_db.getQueue(clientID);
    }

    @Override
    public void dropQueue(String clientID) {
        LOG.info("Removing pending messages. CId={}", clientID);
        this.m_db.delete(clientID);
        outboundFlightMessages.remove(clientID);
        m_inFlightIds.remove(clientID);
    }

    @Override
    public void moveInFlightToSecondPhaseAckWaiting(String clientID, int messageID, StoredMessage msg) {
        LOG.debug("Moving inflight message to 2nd phase ack state. CId={}, messageID={}", clientID, messageID);
        ConcurrentMap<Integer, StoredMessage> m = this.m_secondPhaseStore.get(clientID);
        if (m == null) {
            String error = String.format("Can't find the inFlight record for client <%s> during the second phase of " +
                "QoS2 pub", clientID);
            LOG.error(error);
            throw new RuntimeException(error);
        }
        m.put(messageID, msg);
        this.outboundFlightMessages.put(clientID, m);
    }

    @Override
    public StoredMessage completeReleasedPublish(String clientID, int messageID) {
        LOG.debug("Processing second phase ACK CId={}, messageId={}", clientID, messageID);
        final ConcurrentMap<Integer, StoredMessage> m = this.m_secondPhaseStore.get(clientID);
        if (m == null) {
            String error = String.format("Can't find the inFlight record for client <%s> during the second phase " +
                "acking of QoS2 pub", clientID);
            LOG.error(error);
            throw new RuntimeException(error);
        }

        StoredMessage msg = m.remove(messageID);
        m_secondPhaseStore.put(clientID, m);
        return msg;
    }

    @Override
    public int getInflightMessagesNo(String clientID) {
        int totalInflight = 0;
        ConcurrentMap<Integer, StoredMessage> inflightPerClient = m_db.getHashMap(inboundMessageId2MessagesMapName(clientID));
        if (inflightPerClient != null) {
            totalInflight += inflightPerClient.size();
        }

//        totalInflight += countPubReleaseWaitingPubComplete(clientID);

        Map<Integer, StoredMessage> outboundPerClient = outboundFlightMessages.get(clientID);
        if (outboundPerClient != null) {
            totalInflight += outboundPerClient.size();
        }

        return totalInflight;
    }

    @Override
    public int countPubReleaseWaitingPubComplete(String clientID) {
        if (!m_secondPhaseStore.containsKey(clientID))
            return 0;
        return m_secondPhaseStore.get(clientID).size();
    }

    @Override
    public void removeTemporaryQoS2(String clientID) {
        LOG.info("Removing stored messages with QoS 2. ClientId={}", clientID);
        m_secondPhaseStore.remove(clientID);
    }

    private static String inboundMessageId2MessagesMapName(String clientID) {
        return "inboundInflight_" + clientID;
    }

    @Override
    public synchronized void trackSessionClose(LocalDateTime when, String clientID) {
        this.sessionsClosingTimes.putIfAbsent(when, new HashSet<>());
        this.sessionsClosingTimes.computeIfPresent(when, (key, oldSet) -> {
            oldSet.add(clientID);
            return oldSet;
        });
    }

    @Override
    public Set<String> sessionOlderThan(LocalDateTime queryPin) {
        final Set<String> results = new HashSet<>();
        LocalDateTime keyBefore = this.sessionsClosingTimes.lowerKey(queryPin);
        while (keyBefore != null) {
            results.addAll(this.sessionsClosingTimes.get(keyBefore));
            keyBefore = this.sessionsClosingTimes.lowerKey(keyBefore);
        }
        return results;
    }

}
