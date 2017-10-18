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

import io.moquette.persistence.PersistentSession;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.IMessagesStore.StoredMessage;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.ISubscriptionsStore;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class H2SessionsStore implements ISessionsStore, ISubscriptionsStore {

    private static final Logger LOG = LoggerFactory.getLogger(H2SessionsStore.class);

    private final MVStore mvStore;
    private ConcurrentMap<String, PersistentSession> sessions;
    // maps clientID->[MessageId -> msg]
    private ConcurrentMap<String, ConcurrentMap<Integer, StoredMessage>> outboundFlightMessages;
    // map clientID <-> set of currently in flight packet identifiers
    private Map<String, Set<Integer>> inFlightIds;
    // maps clientID->[MessageId -> guid]
    private ConcurrentMap<String, ConcurrentMap<Integer, StoredMessage>> secondPhaseStore;
    private MVMap<LocalDateTime, Set<String>> sessionsClosingTimes;

    H2SessionsStore(MVStore mvStore) {
        this.mvStore = mvStore;
    }

    @Override
    public void initStore() {
        this.sessions = mvStore.openMap("sessions");
        this.outboundFlightMessages = mvStore.openMap("outboundFlight");
        this.inFlightIds = mvStore.openMap("inflightPacketIDs");
        this.secondPhaseStore = mvStore.openMap("secondPhase");
        this.sessionsClosingTimes = mvStore.openMap("sessionsCreationTimes");
        LOG.info("Initialized sessions H2 store");
    }

    @Override
    public ISubscriptionsStore subscriptionStore() {
        return this;
    }

    //TODO move in the subscription directory
    @Override
    public void addNewSubscription(Subscription newSubscription) {
        LOG.info("Adding new subscription CId={}, topics={}", newSubscription.getClientId(),
            newSubscription.getTopicFilter());
        final String clientID = newSubscription.getClientId();
        final MVMap<Object, Object> sessionSubscriptions = this.mvStore.openMap("subscriptions_" + clientID);
        sessionSubscriptions.put(newSubscription.getTopicFilter(), newSubscription);

        if (LOG.isTraceEnabled()) {
            LOG.trace("Subscription has been added CId={}, topics={}, clientSubscriptions={}",
                newSubscription.getClientId(), newSubscription.getTopicFilter(), sessionSubscriptions);
        }
    }

    //TODO move in the subscription directory
    @Override
    public void removeSubscription(Topic topicFilter, String clientID) {
        LOG.info("Removing subscription. CId={}, topics={}", clientID, topicFilter);
        if (!this.mvStore.hasMap("subscriptions_" + clientID)) {
            return;
        }
        final MVMap<Object, Object> sessionsSubscriptions = this.mvStore.openMap("subscriptions_" + clientID);
        sessionsSubscriptions.remove(topicFilter);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Subscription has been removed. CId={}, topics={}, clientSubscriptions={}", clientID,
                topicFilter, sessionsSubscriptions);
        }
    }

    @Override
    public void wipeSubscriptions(String sessionID) {
        LOG.info("Wiping subscriptions. CId={}", sessionID);
        if (!this.mvStore.hasMap("subscriptions_" + sessionID)) {
            return;
        }
        final MVMap<Object, Object> subscriptions = this.mvStore.openMap("subscriptions_" + sessionID);
        this.mvStore.removeMap(subscriptions);
        if (LOG.isDebugEnabled()) {
            LOG.debug("Subscriptions have been removed. CId={}, clientSubscriptions={}", sessionID, subscriptions);
        }
    }

    @Override
    public List<Subscription> listAllSubscriptions() {
        LOG.debug("Retrieving existing subscriptions...");
        List<Subscription> subscriptions = new ArrayList<>();
        for (String clientID : this.sessions.keySet()) {
            ConcurrentMap<Topic, Subscription> clientSubscriptions = this.mvStore.openMap("subscriptions_" + clientID);
            subscriptions.addAll(clientSubscriptions.values());
        }
        LOG.debug("Existing subscriptions has been retrieved Result={}", subscriptions);
        return subscriptions;
    }

    @Override
    public Collection<Subscription> listClientSubscriptions(String clientID) {
        ConcurrentMap<Topic, Subscription> clientSubscriptions = this.mvStore.openMap("subscriptions_" + clientID);
        if (clientSubscriptions == null) {
            throw new IllegalStateException("Asking for subscriptions of not persisted client: " + clientID);
        }
        return clientSubscriptions.values();
    }

    @Override
    public Subscription reload(Subscription subcription) {
        ConcurrentMap<Topic, Subscription> clientSubscriptions = this.mvStore.openMap("subscriptions_" + subcription.getClientId());
        LOG.debug("Retrieving subscriptions CId={}, subscriptions={}", subcription.getClientId(), clientSubscriptions);
        return clientSubscriptions.get(subcription.getTopicFilter());
    }

    @Override
    public boolean contains(String clientID) {
        return this.sessions.containsKey(clientID);
    }

    @Override
    public void createNewDurableSession(String clientID) {
        this.sessions.putIfAbsent(clientID, new PersistentSession(clientID, false));
    }

    @Override
    public void removeDurableSession(String clientId) {
        this.sessions.remove(clientId);
        this.wipeSubscriptions(clientId);
    }

    @Override
    public void updateCleanStatus(String clientId, boolean newCleanStatus) {
        PersistentSession updatedSession = new PersistentSession(clientId, newCleanStatus);
        this.sessions.put(clientId, updatedSession);
    }

    @Override
    public PersistentSession loadSessionByKey(String clientID) {
        return this.sessions.get(clientID);
    }

    @Override
    public Collection<PersistentSession> listAllSessions() {
        return this.sessions.values();
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
        Set<Integer> inFlightForClient = this.inFlightIds.get(clientID);
        if (inFlightForClient != null) {
            inFlightForClient.remove(messageID);
        }
        return msg;
    }

    @Override
    public void inFlight(String clientID, int messageID, StoredMessage msg) {
        ConcurrentMap<Integer, IMessagesStore.StoredMessage> messages = this.outboundFlightMessages.get(clientID);
        if (messages == null) {
            messages = new ConcurrentHashMap<>();
        }
        messages.put(messageID, msg);
        this.outboundFlightMessages.put(clientID, messages);
    }

    @Override
    public int nextPacketID(String clientID) {
        LOG.debug("Generating next packet ID CId={}", clientID);
        Set<Integer> inFlightForClient = this.inFlightIds.get(clientID);
        if (inFlightForClient == null) {
            int nextPacketId = 1;
            inFlightForClient = Collections.newSetFromMap(new ConcurrentHashMap<>());
            inFlightForClient.add(nextPacketId);
            this.inFlightIds.put(clientID, inFlightForClient);
            return nextPacketId;
        }

        int maxId = inFlightForClient.isEmpty() ? 0 : Collections.max(inFlightForClient);
        int nextPacketId = (maxId % 0xFFFF) + 1;
        inFlightForClient.add(nextPacketId);
        LOG.debug("Next packet ID has been generated CId={}, result={}", clientID, nextPacketId);
        return nextPacketId;
    }

    @Override
    public Queue<StoredMessage> queue(String clientID) {
        return new H2PersistentQueue<>(this.mvStore, clientID);
    }

    @Override
    public void dropQueue(String clientID) {
        H2PersistentQueue.dropQueue(this.mvStore, clientID);
        this.outboundFlightMessages.remove(clientID);
        this.inFlightIds.remove(clientID);
    }

    @Override
    public void moveInFlightToSecondPhaseAckWaiting(String clientID, int messageID, StoredMessage msg) {
        LOG.debug("Moving inflight message to 2nd phase ack state CId={}, messageID={}", clientID, messageID);
        ConcurrentMap<Integer, StoredMessage> m = this.secondPhaseStore.get(clientID);
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
        final ConcurrentMap<Integer, StoredMessage> m = this.secondPhaseStore.get(clientID);
        if (m == null) {
            String error = String.format("Can't find the inFlight record for client <%s> during the second phase " +
                "acking of QoS2 pub", clientID);
            LOG.error(error);
            throw new RuntimeException(error);
        }

        StoredMessage msg = m.remove(messageID);
        this.secondPhaseStore.put(clientID, m);
        return msg;
    }

    @Override
    public int getInflightMessagesNo(String clientID) {
        int totalInflight = 0;
        ConcurrentMap<Integer, StoredMessage> inflightPerClient = this.mvStore.openMap(inboundStoreForClient(clientID));
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

    private static String inboundStoreForClient(String clientID) {
        return "inboundInflight_" + clientID;
    }

    @Override
    public int countPubReleaseWaitingPubComplete(String clientID) {
        if (!this.secondPhaseStore.containsKey(clientID))
            return 0;
        return this.secondPhaseStore.get(clientID).size();
    }

    @Override
    public void removeTemporaryQoS2(String clientID) {
        LOG.info("Removing stored messages with QoS 2. ClientId={}", clientID);
        this.secondPhaseStore.remove(clientID);
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
