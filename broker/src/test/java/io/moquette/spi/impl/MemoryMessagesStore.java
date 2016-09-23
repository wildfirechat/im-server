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
package io.moquette.spi.impl;

import io.moquette.spi.IMessagesStore;
import io.moquette.spi.IMatchingCondition;
import io.moquette.spi.MessageGUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static io.moquette.spi.impl.Utils.defaultGet;

/**
 * @author andrea
 */
public class MemoryMessagesStore implements IMessagesStore {

    private static final Logger LOG = LoggerFactory.getLogger(MemoryMessagesStore.class);

    private Map<String, MessageGUID> m_retainedStore = new HashMap<>();
    private Map<MessageGUID, StoredMessage> m_persistentMessageStore = new HashMap<>();
    private Map<String, Map<Integer, MessageGUID>> m_messageToGuids;

    MemoryMessagesStore(Map<String, Map<Integer, MessageGUID>> messageToGuids) {
        m_messageToGuids = messageToGuids;
    }

    @Override
    public void initStore() {
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
        MessageGUID guid = new MessageGUID(UUID.randomUUID().toString());
        evt.setGuid(guid);
        m_persistentMessageStore.put(guid, evt);
        HashMap<Integer, MessageGUID> guids = (HashMap<Integer, MessageGUID>) defaultGet(m_messageToGuids,
                evt.getClientID(), new HashMap<Integer, MessageGUID>());
        guids.put(evt.getMessageID(), guid);
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
        Map<Integer, MessageGUID> messageGUIDMap = m_messageToGuids.get(clientID);
        if (messageGUIDMap == null || messageGUIDMap.isEmpty()) {
            return;
        }
        for (MessageGUID guid : messageGUIDMap.values()) {
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
