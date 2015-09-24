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

import java.nio.ByteBuffer;
import java.util.*;

import org.eclipse.moquette.spi.IMatchingCondition;
import org.eclipse.moquette.spi.IMessagesStore;
import org.eclipse.moquette.spi.impl.events.PublishEvent;
import org.eclipse.moquette.spi.impl.subscriptions.Subscription;
import org.eclipse.moquette.proto.messages.AbstractMessage;

import org.eclipse.moquette.spi.ISessionsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.eclipse.moquette.spi.impl.Utils.defaultGet;

/**
 */
public class MemoryStorageService implements IMessagesStore, ISessionsStore {
    
    private Map<String, Set<Subscription>> m_persistentSubscriptions = new HashMap<>();
    private Map<String, StoredMessage> m_retainedStore = new HashMap<>();
    //TODO move in a multimap because only Qos1 and QoS2 are stored here and they have messageID(key of secondary map)
    private Map<String, List<PublishEvent>> m_persistentMessageStore = new HashMap<>();
    private Map<String, PublishEvent> m_inflightStore = new HashMap<>();
    private Map<String, Set<Integer>> m_inflightIDs = new HashMap<>();
    private Map<String, PublishEvent> m_qos2Store = new HashMap<>();
    
    private static final Logger LOG = LoggerFactory.getLogger(MemoryStorageService.class);
    
    public void initStore() {
    }
    
    @Override
    public void cleanRetained(String topic) {
        m_retainedStore.remove(topic);
    }
    
    @Override
    public void storeRetained(String topic, ByteBuffer message, AbstractMessage.QOSType qos) {
        if (!message.hasRemaining()) {
            //clean the message from topic
            m_retainedStore.remove(topic);
        } else {
            //store the message to the topic
            byte[] raw = new byte[message.remaining()];
            message.get(raw);
            m_retainedStore.put(topic, new StoredMessage(raw, qos, topic));
        }
    }

    @Override
    public Collection<StoredMessage> searchMatching(IMatchingCondition condition) {
        LOG.debug("searchMatching scanning all retained messages, presents are {}", m_retainedStore.size());

        List<StoredMessage> results = new ArrayList<StoredMessage>();

        for (Map.Entry<String, StoredMessage> entry : m_retainedStore.entrySet()) {
            StoredMessage storedMsg = entry.getValue();
            if (condition.match(entry.getKey())) {
                results.add(storedMsg);
            }
        }

        return results;
    }

    @Override
    public void storePublishForFuture(PublishEvent evt) {
        LOG.debug("storePublishForFuture store evt {}", evt);
//        List<PublishEvent> storedEvents;
        String clientID = evt.getClientID();
//        if (!m_persistentMessageStore.containsKey(clientID)) {
//            storedEvents = new ArrayList<PublishEvent>();
//        } else {
//            storedEvents = m_persistentMessageStore.get(clientID);
//        }
        List<PublishEvent> storedEvents = defaultGet(m_persistentMessageStore, clientID, new ArrayList<PublishEvent>());
        storedEvents.add(evt);
        m_persistentMessageStore.put(clientID, storedEvents);
    }

    @Override
    public List<PublishEvent> listMessagesInSession(String clientID) {
        return new ArrayList<>(defaultGet(m_persistentMessageStore, clientID, Collections.<PublishEvent>emptyList()));
    }
    
    @Override
    public void removeMessageInSession(String clientID, Integer messageID) {
        List<PublishEvent> events = m_persistentMessageStore.get(clientID);
        if (events == null) {
            return;
        }
        PublishEvent toRemoveEvt = null;
        for (PublishEvent evt : events) {
            if (evt.getMessageID() == null && messageID == null) {
                //was a qos0 message (no ID)
                toRemoveEvt = evt;
            }
            if (evt.getMessageID() == messageID) {
                toRemoveEvt = evt;
            }
        }
        events.remove(toRemoveEvt);
        m_persistentMessageStore.put(clientID, events);
    }

    @Override
    public void dropMessagesInSession(String clientID) {
        m_persistentMessageStore.remove(clientID);
    }

    @Override
    public void cleanTemporaryPublish(String clientID, int packetID) {
        String publishKey = String.format("%s%d", clientID, packetID);
        m_inflightStore.remove(publishKey);
        Set<Integer> inFlightForClient = m_inflightIDs.get(clientID);
        if (inFlightForClient != null) {
            inFlightForClient.remove(packetID);
        }
    }

    @Override
    public void storeTemporaryPublish(PublishEvent evt, String clientID, int packetID) {
        String publishKey = String.format("%s%d", clientID, packetID);
        m_inflightStore.put(publishKey, evt);
    }

    /**
     * Return the next valid packetIdentifer for the given client session.
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
    }

    @Override
    public void addNewSubscription(Subscription newSubscription) {
        final String clientID = newSubscription.getClientId();
        if (!m_persistentSubscriptions.containsKey(clientID)) {
            m_persistentSubscriptions.put(clientID, new HashSet<Subscription>());
        }

        Set<Subscription> subs = m_persistentSubscriptions.get(clientID);
        if (!subs.contains(newSubscription)) {
            subs.add(newSubscription);
            m_persistentSubscriptions.put(clientID, subs);
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
    public boolean contains(String clientID) {
        return m_persistentSubscriptions.containsKey(clientID);
    }

    @Override
    public void createNewSession(String clientID) {
        if (m_persistentSubscriptions.containsKey(clientID)) {
            LOG.error("already exists a session for client <{}>", clientID);
            return;
        }
        m_persistentSubscriptions.put(clientID, new HashSet<Subscription>());
    }

    @Override
    public List<Subscription> listAllSubscriptions() {
        List<Subscription> allSubscriptions = new ArrayList<Subscription>();
        for (Map.Entry<String, Set<Subscription>> entry : m_persistentSubscriptions.entrySet()) {
            allSubscriptions.addAll(entry.getValue());
        }
        return allSubscriptions;
    }

    @Override
    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void persistQoS2Message(String publishKey, PublishEvent evt) {
        LOG.debug("persistQoS2Message store pubKey {}, evt {}", publishKey, evt);
        m_qos2Store.put(publishKey, evt);
    }

    @Override
    public void removeQoS2Message(String publishKey) {
        m_qos2Store.remove(publishKey);
    }

    @Override
    public PublishEvent retrieveQoS2Message(String publishKey) {
        return m_qos2Store.get(publishKey);
    }
}
