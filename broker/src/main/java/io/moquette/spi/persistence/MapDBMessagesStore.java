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

import io.moquette.spi.IMatchingCondition;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.MessageGUID;
import org.mapdb.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

/**
 * IMessagesStore implementation backed by MapDB.
 *
 * @author andrea
 */
class MapDBMessagesStore implements IMessagesStore {

    private static final Logger LOG = LoggerFactory.getLogger(MapDBMessagesStore.class);

    private DB m_db;

    //maps clientID -> guid
    private ConcurrentMap<String, MessageGUID> m_retainedStore;
    //maps guid to message, it's message store
    private ConcurrentMap<MessageGUID, IMessagesStore.StoredMessage> m_persistentMessageStore;


    MapDBMessagesStore(DB db) {
        m_db = db;
    }

    @Override
    public void initStore() {
        m_retainedStore = m_db.getHashMap("retained");
        m_persistentMessageStore = m_db.getHashMap("persistedMessages");
    }

    @Override
    public void storeRetained(String topic, MessageGUID guid) {
        m_retainedStore.put(topic, guid);
    }

    @Override
    public Collection<StoredMessage> searchMatching(IMatchingCondition condition) {
        LOG.debug("searchMatching scanning all retained messages, presents are {}", m_retainedStore.size());

        List<StoredMessage> results = new ArrayList<>();
        for (Map.Entry<String, MessageGUID> entry : m_retainedStore.entrySet()) {
            final MessageGUID guid = entry.getValue();
            StoredMessage storedMsg = m_persistentMessageStore.get(guid);
            if (condition.match(entry.getKey())) {
                results.add(storedMsg);
            }
        }

        return results;
    }

    @Override
    public MessageGUID storePublishForFuture(StoredMessage evt) {
        LOG.debug("storePublishForFuture store evt {}", evt);
        if (evt.getClientID() == null) {
            LOG.error("persisting a message without a clientID, bad programming error msg: {}", evt);
            throw new IllegalArgumentException("\"persisting a message without a clientID, bad programming error");
        }
        MessageGUID guid = new MessageGUID(UUID.randomUUID().toString());
        evt.setGuid(guid);
        LOG.debug("storePublishForFuture guid <{}>", guid);
        m_persistentMessageStore.put(guid, evt);
        ConcurrentMap<Integer, MessageGUID> messageIdToGuid = m_db.getHashMap(MapDBSessionsStore.messageId2GuidsMapName(evt.getClientID()));
        messageIdToGuid.put(evt.getMessageID(), guid);
        return guid;
    }

    @Override
    public List<StoredMessage> listMessagesInSession(Collection<MessageGUID> guids) {
        List<StoredMessage> ret = new ArrayList<>();
        for (MessageGUID guid : guids) {
            ret.add(m_persistentMessageStore.get(guid));
        }
        return ret;
    }

    @Override
    public void dropMessagesInSession(String clientID) {
        ConcurrentMap<Integer, MessageGUID> messageIdToGuid = m_db.getHashMap(MapDBSessionsStore.messageId2GuidsMapName(clientID));
        for (MessageGUID guid : messageIdToGuid.values()) {
            removeStoredMessage(guid);
        }
        messageIdToGuid.clear();
    }

    void removeStoredMessage(MessageGUID guid) {
        //remove only the not retained and no more referenced
        StoredMessage storedMessage = m_persistentMessageStore.get(guid);
        if (!storedMessage.isRetained() && storedMessage.getReferenceCounter() == 0) {
            LOG.debug("Cleaning not retained message guid {}", guid);
            m_persistentMessageStore.remove(guid);
        }
    }

    @Override
    public StoredMessage getMessageByGuid(MessageGUID guid) {
        return m_persistentMessageStore.get(guid);
    }

    @Override
    public void cleanRetained(String topic) {
        m_retainedStore.remove(topic);
    }

    @Override
    public void incUsageCounter(MessageGUID guid) {
        IMessagesStore.StoredMessage storedMessage = m_persistentMessageStore.get(guid);
        storedMessage.incReferenceCounter();
        m_persistentMessageStore.put(guid, storedMessage);
    }

    @Override
    public void decUsageCounter(MessageGUID guid) {
        IMessagesStore.StoredMessage storedMessage = m_persistentMessageStore.get(guid);
        storedMessage.decReferenceCounter();
        m_persistentMessageStore.put(guid, storedMessage);
    }
}
