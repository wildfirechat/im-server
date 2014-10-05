/*
 * Copyright (c) 2012-2014 The original author or authors
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

/**
 */
public class MemoryStorageService implements IMessagesStore, ISessionsStore {
    
    private Map<String, Set<Subscription>> m_persistentSubscriptions = new HashMap<String, Set<Subscription>>();
    private Map<String, StoredMessage> m_retainedStore = new HashMap<String, StoredMessage>();
    //TODO move in a multimap because only Qos1 and QoS2 are stored here and they have messageID(key of secondary map)
    private Map<String, List<PublishEvent>> m_persistentMessageStore = new HashMap<String, List<PublishEvent>>();
    private Map<String, PublishEvent> m_inflightStore = new HashMap<String, PublishEvent>();
    private Map<String, PublishEvent> m_qos2Store = new HashMap<String, PublishEvent>();
    
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
        List<PublishEvent> storedEvents;
        String clientID = evt.getClientID();
        if (!m_persistentMessageStore.containsKey(clientID)) {
            storedEvents = new ArrayList<PublishEvent>();
        } else {
            storedEvents = m_persistentMessageStore.get(clientID);
        }
        storedEvents.add(evt);
        m_persistentMessageStore.put(clientID, storedEvents);
    }

    @Override
    public List<PublishEvent> retrievePersistedPublishes(String clientID) {
        return m_persistentMessageStore.get(clientID);
    }
    
    @Override
    public void cleanPersistedPublishMessage(String clientID, int messageID) {
        List<PublishEvent> events = m_persistentMessageStore.get(clientID);
        PublishEvent toRemoveEvt = null;
        for (PublishEvent evt : events) {
            if (evt.getMessageID() == messageID) {
                toRemoveEvt = evt;
            }
        }
        events.remove(toRemoveEvt);
        m_persistentMessageStore.put(clientID, events);
    }

    @Override
    public void cleanPersistedPublishes(String clientID) {
        m_persistentMessageStore.remove(clientID);
    }

    @Override
    public void cleanInFlight(String msgID) {
        m_inflightStore.remove(msgID);
    }

    @Override
    public void addInFlight(PublishEvent evt, String publishKey) {
        m_inflightStore.put(publishKey, evt);
    }

    @Override
    public void addNewSubscription(Subscription newSubscription, String clientID) {
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
