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

import io.moquette.spi.IMatchingCondition;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.MessageGUID;
import io.moquette.spi.impl.subscriptions.Topic;
import org.mapdb.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * IMessagesStore implementation backed by MapDB.
 */
class MapDBMessagesStore implements IMessagesStore {

    private static final Logger LOG = LoggerFactory.getLogger(MapDBMessagesStore.class);

    private DB m_db;

    // maps clientID -> guid
    private ConcurrentMap<Topic, MessageGUID> m_retainedStore;
    // maps guid to message, it's message store
    private ConcurrentMap<MessageGUID, IMessagesStore.StoredMessage> m_persistentMessageStore;

    MapDBMessagesStore(DB db) {
        m_db = db;
    }

    @Override
    public void initStore() {
        m_retainedStore = m_db.getHashMap("retained");
        m_persistentMessageStore = m_db.getHashMap("persistedMessages");
        LOG.info("Initialized store");
    }

    @Override
    public void storeRetained(Topic topic, MessageGUID guid) {
        LOG.debug("Storing retained messages. Topic={}, guid={}", topic, guid);
        m_retainedStore.put(topic, guid);
    }

    @Override
    public Collection<StoredMessage> searchMatching(IMatchingCondition condition) {
        LOG.debug("Scanning retained messages...");
        List<StoredMessage> results = new ArrayList<>();
        for (Map.Entry<Topic, MessageGUID> entry : m_retainedStore.entrySet()) {
            final MessageGUID guid = entry.getValue();
            StoredMessage storedMsg = m_persistentMessageStore.get(guid);
            if (condition.match(entry.getKey())) {
                results.add(storedMsg);
            }
        }

        if (LOG.isTraceEnabled()) {
            LOG.trace("The retained messages have been scanned. MatchingMessages={}", results);
        }

        return results;
    }

    @Override
    public MessageGUID storePublishForFuture(StoredMessage storedMessage) {
        assert storedMessage.getClientID() != null : "Message to be persisted must have a valid client ID";
        MessageGUID guid = new MessageGUID(UUID.randomUUID().toString());
        storedMessage.setGuid(guid);
        LOG.debug("Storing publish event. CId={}, guid={}, topic={}", storedMessage.getClientID(), guid,
            storedMessage.getTopic());
        m_persistentMessageStore.put(guid, storedMessage);
        return guid;
    }

    @Override
    public void dropInFlightMessagesInSession(Collection<MessageGUID> pendingAckMessages) {
        //remove all guids from retained
        Collection<MessageGUID> messagesToRemove = new HashSet<>(pendingAckMessages);
        messagesToRemove.removeAll(m_retainedStore.values());

        for (MessageGUID guid : messagesToRemove) {
            m_persistentMessageStore.remove(guid);
        }
    }

    @Override
    public StoredMessage getMessageByGuid(MessageGUID guid) {
        LOG.debug("Retrieving stored message. Guid={}", guid);
        return m_persistentMessageStore.get(guid);
    }

    @Override
    public void cleanRetained(Topic topic) {
        LOG.debug("Cleaning retained messages. Topic={}", topic);
        m_retainedStore.remove(topic);
    }
}
