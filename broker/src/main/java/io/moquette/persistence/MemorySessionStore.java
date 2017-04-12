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

package io.moquette.persistence;

import io.moquette.server.Constants;
import io.moquette.spi.ClientSession;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.IMessagesStore.StoredMessage;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.MessageGUID;
import io.moquette.spi.impl.Utils;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static io.moquette.spi.impl.Utils.defaultGet;

public class MemorySessionStore implements ISessionsStore {

    private static final Logger LOG = LoggerFactory.getLogger(MemorySessionStore.class);

    private Map<String, Map<Topic, Subscription>> m_persistentSubscriptions = new HashMap<>();

    private Map<String, PersistentSession> m_persistentSessions = new HashMap<>();

    // maps clientID->[MessageId -> guid]
    private Map<String, Map<Integer, MessageGUID>> m_inflightStore = new HashMap<>();
    // maps clientID->BlockingQueue
    private Map<String, BlockingQueue<StoredMessage>> queues = new HashMap<>();
    // maps clientID->[MessageId -> guid]
    private Map<String, Map<Integer, MessageGUID>> m_secondPhaseStore = new HashMap<>();

    private Map<String, Map<Integer, MessageGUID>> outboundFlightMessageToGuid = new HashMap<>();

    private Map<String, Map<Integer, MessageGUID>> inboundFlightMessageToGuid = new HashMap<>();

    private final IMessagesStore m_messagesStore;

    public MemorySessionStore(IMessagesStore messagesStore) {
        this.m_messagesStore = messagesStore;
    }

    @Override
    public void removeSubscription(Topic topic, String clientID) {
        LOG.debug("removeSubscription topic filter: {} for clientID: {}", topic, clientID);
        if (!m_persistentSubscriptions.containsKey(clientID)) {
            return;
        }
        Map<Topic, Subscription> clientSubscriptions = m_persistentSubscriptions.get(clientID);
        clientSubscriptions.remove(topic);
    }

    @Override
    public void initStore() {
    }

    @Override
    public void addNewSubscription(Subscription newSubscription) {
        final String clientID = newSubscription.getClientId();
        if (!m_persistentSubscriptions.containsKey(clientID)) {
            m_persistentSubscriptions.put(clientID, new HashMap<Topic, Subscription>());
        }

        m_persistentSubscriptions.get(clientID).put(newSubscription.getTopicFilter(), newSubscription);
    }

    @Override
    public void wipeSubscriptions(String clientID) {
        m_persistentSubscriptions.remove(clientID);
    }

    @Override
    public boolean contains(String clientID) {
        return m_persistentSubscriptions.containsKey(clientID);
    }

    @Override
    public ClientSession createNewSession(String clientID, boolean cleanSession) {
        LOG.debug("createNewSession for client <{}>", clientID);
        if (m_persistentSessions.containsKey(clientID)) {
            LOG.error("already exists a session for client <{}>, bad condition", clientID);
            throw new IllegalArgumentException("Can't create a session with the ID of an already existing" + clientID);
        }
        LOG.debug("clientID {} is a newcome, creating it's empty subscriptions set", clientID);
        m_persistentSubscriptions.put(clientID, new HashMap<Topic, Subscription>());
        m_persistentSessions.put(clientID, new PersistentSession(cleanSession));
        return new ClientSession(clientID, m_messagesStore, this, cleanSession);
    }

    @Override
    public ClientSession sessionForClient(String clientID) {
        if (!m_persistentSessions.containsKey(clientID)) {
            return null;
        }

        PersistentSession storedSession = m_persistentSessions.get(clientID);
        return new ClientSession(clientID, m_messagesStore, this, storedSession.cleanSession);
    }

    @Override
    public Collection<ClientSession> getAllSessions() {
        Collection<ClientSession> result = new ArrayList<>();
        for (Map.Entry<String, PersistentSession> entry : m_persistentSessions.entrySet()) {
            result.add(new ClientSession(entry.getKey(), m_messagesStore, this, entry.getValue().cleanSession));
        }
        return result;
    }

    @Override
    public void updateCleanStatus(String clientID, boolean cleanSession) {
        m_persistentSessions.put(clientID, new PersistentSession(cleanSession));
    }

    @Override
    public List<ClientTopicCouple> listAllSubscriptions() {
        List<ClientTopicCouple> allSubscriptions = new ArrayList<>();
        for (Map.Entry<String, Map<Topic, Subscription>> entry : m_persistentSubscriptions.entrySet()) {
            for (Subscription sub : entry.getValue().values()) {
                allSubscriptions.add(sub.asClientTopicCouple());
            }
        }
        return allSubscriptions;
    }

    @Override
    public Subscription getSubscription(ClientTopicCouple couple) {
        Map<Topic, Subscription> subscriptions = m_persistentSubscriptions.get(couple.clientID);
        if (subscriptions == null || subscriptions.isEmpty()) {
            return null;
        }
        return subscriptions.get(couple.topicFilter);
    }

    @Override
    public List<Subscription> getSubscriptions() {
        List<Subscription> subscriptions = new ArrayList<>();
        for (Map.Entry<String, Map<Topic, Subscription>> entry : m_persistentSubscriptions.entrySet()) {
            subscriptions.addAll(entry.getValue().values());
        }
        return subscriptions;
    }

    @Override
    public void inFlightAck(String clientID, int messageID) {
        Map<Integer, MessageGUID> m = this.m_inflightStore.get(clientID);
        if (m == null) {
            LOG.error("Can't find the inFlight record for client <{}>", clientID);
            return;
        }
        m.remove(messageID);
    }

    @Override
    public void inFlight(String clientID, int messageID, MessageGUID guid) {
        Map<Integer, MessageGUID> m = this.m_inflightStore.get(clientID);
        if (m == null) {
            m = new HashMap<>();
        }
        m.put(messageID, guid);
        this.m_inflightStore.put(clientID, m);

        final HashMap<Integer, MessageGUID> emptyGuids = new HashMap<>();
        Map<Integer, MessageGUID> guids = defaultGet(outboundFlightMessageToGuid, clientID, emptyGuids);
        guids.put(messageID, guid);
        outboundFlightMessageToGuid.put(clientID, guids);
    }

    /**
     * Return the next valid packetIdentifier for the given client session.
     */
    @Override
    public int nextPacketID(String clientID) {
        Map<Integer, MessageGUID> m = this.m_inflightStore.get(clientID);
        if (m == null) {
            m = new HashMap<>();
            int nextPacketId = 1;
            m.put(nextPacketId, null);
            return nextPacketId;
        }
        int maxId = m.keySet().isEmpty() ? 0 :Collections.max(m.keySet());
        int nextPacketId = (maxId + 1) % 0xFFFF;
        m.put(nextPacketId, null);
        return nextPacketId;
    }

    @Override
    public BlockingQueue<StoredMessage> queue(String clientID) {
        final ArrayBlockingQueue<StoredMessage> emptyQueue = new ArrayBlockingQueue<>(Constants.MAX_MESSAGE_QUEUE);
        BlockingQueue<StoredMessage> messagesQueue = Utils.defaultGet(queues, clientID, emptyQueue);
        queues.put(clientID, messagesQueue);
        return messagesQueue;
    }

    @Override
    public void dropQueue(String clientID) {
        queues.remove(clientID);
    }

    @Override
    public void moveInFlightToSecondPhaseAckWaiting(String clientID, int messageID) {
        LOG.info("acknowledging inflight clientID <{}> messageID {}", clientID, messageID);
        Map<Integer, MessageGUID> m = this.m_inflightStore.get(clientID);
        if (m == null) {
            LOG.error("Can't find the inFlight record for client <{}>", clientID);
            return;
        }
        MessageGUID guid = m.remove(messageID);

        LOG.info("Moving to second phase store");
        final HashMap<Integer, MessageGUID> emptyGuids = new HashMap<>();
        Map<Integer, MessageGUID> messageIDs = Utils.defaultGet(m_secondPhaseStore, clientID, emptyGuids);
        messageIDs.put(messageID, guid);
        m_secondPhaseStore.put(clientID, messageIDs);
    }

    @Override
    public MessageGUID secondPhaseAcknowledged(String clientID, int messageID) {
        final HashMap<Integer, MessageGUID> emptyGuids = new HashMap<>();
        Map<Integer, MessageGUID> messageIDs = Utils.defaultGet(m_secondPhaseStore, clientID, emptyGuids);
        MessageGUID guid = messageIDs.remove(messageID);
        m_secondPhaseStore.put(clientID, messageIDs);
        return guid;
    }

    @Override
    public StoredMessage getInflightMessage(String clientID, int messageID) {
        Map<Integer, MessageGUID> inflightMessages = m_inflightStore.get(clientID);
        MessageGUID guid = inflightMessages.get(messageID);
        return m_messagesStore.getMessageByGuid(guid);
    }

    @Override
    public int getInflightMessagesNo(String clientID) {
        Map<Integer, MessageGUID> inflightMessages = m_inflightStore.get(clientID);
        if (inflightMessages == null)
            return 0;
        else
            return inflightMessages.size();
    }

    @Override
    public StoredMessage inboundInflight(String clientID, int messageID) {
        final HashMap<Integer, MessageGUID> emptyGuids = new HashMap<>();
        Map<Integer, MessageGUID> guids = Utils.defaultGet(inboundFlightMessageToGuid, clientID, emptyGuids);
        final MessageGUID guid = guids.get(messageID);
        return m_messagesStore.getMessageByGuid(guid);
    }

    @Override
    public void markAsInboundInflight(String clientID, int messageID, MessageGUID guid) {
        final HashMap<Integer, MessageGUID> emptyGuids = new HashMap<>();
        Map<Integer, MessageGUID> guids = Utils.defaultGet(inboundFlightMessageToGuid, clientID, emptyGuids);
        guids.put(messageID, guid);
        inboundFlightMessageToGuid.put(clientID, guids);
    }

    @Override
    public int getPendingPublishMessagesNo(String clientID) {
        return queues.get(clientID).size();
    }

    @Override
    public int getSecondPhaseAckPendingMessages(String clientID) {
        Map<Integer, MessageGUID> pendingAcks = m_secondPhaseStore.get(clientID);
        if (pendingAcks == null)
            return 0;
        else
            return pendingAcks.size();
    }

    @Override
    public Collection<MessageGUID> pendingAck(String clientID) {
        Map<Integer, MessageGUID> messageGUIDMap = outboundFlightMessageToGuid.get(clientID);
        if (messageGUIDMap == null || messageGUIDMap.isEmpty()) {
            return Collections.emptyList();
        }

        return new ArrayList<>(messageGUIDMap.values());
    }
}
