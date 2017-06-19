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
import io.moquette.spi.ClientSession;
import io.moquette.spi.IMessagesStore.StoredMessage;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.ISubscriptionsStore;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;
import org.mapdb.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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

    MapDBSessionsStore(DB db) {
        m_db = db;
    }

    @Override
    public void initStore() {
        outboundFlightMessages = m_db.getHashMap("outboundFlight");
        m_inFlightIds = m_db.getHashMap("inflightPacketIDs");
        m_persistentSessions = m_db.getHashMap("sessions");
        m_secondPhaseStore = m_db.getHashMap("secondPhase");
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
    public List<ClientTopicCouple> listAllSubscriptions() {
        LOG.debug("Retrieving existing subscriptions");
        final List<ClientTopicCouple> allSubscriptions = new ArrayList<>();
        for (String clientID : m_persistentSessions.keySet()) {
            ConcurrentMap<Topic, Subscription> clientSubscriptions = m_db.getHashMap("subscriptions_" + clientID);
            for (Topic topicFilter : clientSubscriptions.keySet()) {
                allSubscriptions.add(new ClientTopicCouple(clientID, topicFilter));
            }
        }
        if (LOG.isTraceEnabled()) {
            LOG.trace("The existing subscriptions have been retrieved. Result={}", allSubscriptions);
        }
        return allSubscriptions;
    }

    @Override
    public Subscription getSubscription(ClientTopicCouple couple) {
        ConcurrentMap<Topic, Subscription> clientSubscriptions = m_db.getHashMap("subscriptions_" + couple.clientID);
        LOG.debug("Retrieving subscriptions. CId={}, subscriptions={}", couple.clientID, clientSubscriptions);
        return clientSubscriptions.get(couple.topicFilter);
    }

    @Override
    public List<Subscription> getSubscriptions() {
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
    public boolean contains(String clientID) {
        return m_db.exists("subscriptions_" + clientID);
    }

    @Override
    public ClientSession createNewSession(String clientID, boolean cleanSession) {
        if (m_persistentSessions.containsKey(clientID)) {
            LOG.error("Unable to create a new session: the client ID is already in use. ClientId={}, cleanSession={}",
                clientID, cleanSession);
            throw new IllegalArgumentException("Can't create a session with the ID of an already existing" + clientID);
        }
        LOG.debug("Creating new session. CId={}, cleanSession={}", clientID, cleanSession);
        m_persistentSessions.putIfAbsent(clientID, new PersistentSession(cleanSession));
        return new ClientSession(clientID, this, this, cleanSession);
    }

    @Override
    public ClientSession sessionForClient(String clientID) {
        LOG.debug("Retrieving session CId={}", clientID);
        if (!m_persistentSessions.containsKey(clientID)) {
            LOG.warn("Session does not exist CId={}", clientID);
            return null;
        }

        PersistentSession storedSession = m_persistentSessions.get(clientID);
        return new ClientSession(clientID, this, this, storedSession.cleanSession);
    }

    @Override
    public Collection<ClientSession> getAllSessions() {
        Collection<ClientSession> result = new ArrayList<>();
        for (Map.Entry<String, PersistentSession> entry : m_persistentSessions.entrySet()) {
            result.add(new ClientSession(entry.getKey(), this, this, entry.getValue().cleanSession));
        }
        return result;
    }

    @Override
    public void updateCleanStatus(String clientID, boolean cleanSession) {
        LOG.info("Updating cleanSession flag. CId={}, cleanSession={}", clientID, cleanSession);
        m_persistentSessions.put(clientID, new PersistentSession(cleanSession));
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
    public StoredMessage secondPhaseAcknowledged(String clientID, int messageID) {
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

        Map<Integer, StoredMessage> secondPhaseInFlight = this.m_secondPhaseStore.get(clientID);
        if (secondPhaseInFlight != null) {
            totalInflight += secondPhaseInFlight.size();
        }

        Map<Integer, StoredMessage> outboundPerClient = outboundFlightMessages.get(clientID);
        if (outboundPerClient != null) {
            totalInflight += outboundPerClient.size();
        }

        return totalInflight;
    }

    @Override
    public StoredMessage inboundInflight(String clientID, int messageID) {
        LOG.debug("Mapping inbound message ID to GUID CId={}, messageId={}", clientID, messageID);
        ConcurrentMap<Integer, StoredMessage> messageIdToGuid = m_db.getHashMap(inboundMessageId2MessagesMapName(clientID));
        return messageIdToGuid.get(messageID);
    }

    @Override
    public void markAsInboundInflight(String clientID, int messageID, StoredMessage msg) {
        ConcurrentMap<Integer, StoredMessage> messageIdToGuid = m_db.getHashMap(inboundMessageId2MessagesMapName(clientID));
        messageIdToGuid.put(messageID, msg);
    }

    @Override
    public int getPendingPublishMessagesNo(String clientID) {
        return queue(clientID).size();
    }

    @Override
    public int getSecondPhaseAckPendingMessages(String clientID) {
        if (!m_secondPhaseStore.containsKey(clientID))
            return 0;
        return m_secondPhaseStore.get(clientID).size();
    }

    @Override
    public void cleanSession(String clientID) {
        // remove also the messages stored of type QoS1/2
        LOG.info("Removing stored messages with QoS 1 and 2. ClientId={}", clientID);
        m_secondPhaseStore.remove(clientID);
        outboundFlightMessages.remove(clientID);
        m_inFlightIds.remove(clientID);

        LOG.info("Wiping existing subscriptions. ClientId={}", clientID);
        wipeSubscriptions(clientID);

        //remove also the enqueued messages
        dropQueue(clientID);
    }

    static String inboundMessageId2MessagesMapName(String clientID) {
        return "inboundInflight_" + clientID;
    }
}
