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
package org.dna.mqtt.moquette.messaging.spi.impl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import org.dna.mqtt.moquette.messaging.spi.IMatchingCondition;
import org.dna.mqtt.moquette.messaging.spi.IStorageService;
import org.dna.mqtt.moquette.messaging.spi.impl.events.PublishEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.Subscription;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class MemoryStorageService implements IStorageService {
    
    private Map<String, Set<Subscription>> m_persistentSubscriptions = new HashMap<String, Set<Subscription>>();
    private Map<String, HawtDBStorageService.StoredMessage> m_retainedStore = new HashMap<String, HawtDBStorageService.StoredMessage>();
    //TODO move in a multimap because only Qos1 and QoS2 are stored here and they have messageID(key of secondary map)
    private Map<String, List<PublishEvent>> m_persistentMessageStore = new HashMap<String, List<PublishEvent>>();
    private Map<String, PublishEvent> m_inflightStore = new HashMap<String, PublishEvent>();
    private Map<String, PublishEvent> m_qos2Store = new HashMap<String, PublishEvent>();
    
    private static final Logger LOG = LoggerFactory.getLogger(MemoryStorageService.class);
    
    public void initStore() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void storeRetained(String topic, ByteBuffer message, AbstractMessage.QOSType qos) {
        if (!message.hasRemaining()) {
            //clean the message from topic
            m_retainedStore.remove(topic);
        } else {
            //store the message to the topic
            byte[] raw = new byte[message.remaining()];
            message.get(raw);
            m_retainedStore.put(topic, new HawtDBStorageService.StoredMessage(raw, qos, topic));
        }
    }

    public Collection<HawtDBStorageService.StoredMessage> searchMatching(IMatchingCondition condition) {
        LOG.debug("searchMatching scanning all retained messages, presents are {}", m_retainedStore.size());

        List<HawtDBStorageService.StoredMessage> results = new ArrayList<HawtDBStorageService.StoredMessage>();

        for (Map.Entry<String, HawtDBStorageService.StoredMessage> entry : m_retainedStore.entrySet()) {
            HawtDBStorageService.StoredMessage storedMsg = entry.getValue();
            if (condition.match(entry.getKey())) {
                results.add(storedMsg);
            }
        }

        return results;
    }

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

    public void cleanPersistedPublishes(String clientID) {
        m_persistentMessageStore.remove(clientID);
    }

    public void cleanInFlight(String msgID) {
        m_inflightStore.remove(msgID);
    }

    public void addInFlight(PublishEvent evt, String publishKey) {
        m_inflightStore.put(publishKey, evt);
    }

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

    public void removeAllSubscriptions(String clientID) {
        m_persistentSubscriptions.remove(clientID);
    }

    public List<Subscription> retrieveAllSubscriptions() {
        List<Subscription> allSubscriptions = new ArrayList<Subscription>();
        for (Map.Entry<String, Set<Subscription>> entry : m_persistentSubscriptions.entrySet()) {
            allSubscriptions.addAll(entry.getValue());
        }
        return allSubscriptions;
    }

    public void close() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void persistQoS2Message(String publishKey, PublishEvent evt) {
        LOG.debug("persistQoS2Message store pubKey {}, evt {}", publishKey, evt);
        m_qos2Store.put(publishKey, evt);
    }

    public void removeQoS2Message(String publishKey) {
        m_qos2Store.remove(publishKey);
    }

    public PublishEvent retrieveQoS2Message(String publishKey) {
        return m_qos2Store.get(publishKey);
    }
}
