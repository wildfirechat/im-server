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

import io.moquette.spi.IMessagesStore;
import io.moquette.spi.IMatchingCondition;
import io.moquette.spi.MessageGUID;
import io.moquette.spi.impl.subscriptions.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public class MemoryMessagesStore implements IMessagesStore {

    private static final Logger LOG = LoggerFactory.getLogger(MemoryMessagesStore.class);

    private Map<Topic, MessageGUID> m_retainedStore = new HashMap<>();
    private Map<MessageGUID, StoredMessage> m_persistentMessageStore = new HashMap<>();

    MemoryMessagesStore() {
    }

    @Override
    public void initStore() {
    }

    @Override
    public void storeRetained(Topic topic, MessageGUID guid) {
        m_retainedStore.put(topic, guid);
    }

    @Override
    public Collection<StoredMessage> searchMatching(IMatchingCondition condition) {
        LOG.debug("searchMatching scanning all retained messages, presents are {}", m_retainedStore.size());

        List<StoredMessage> results = new ArrayList<>();

        for (Map.Entry<Topic, MessageGUID> entry : m_retainedStore.entrySet()) {
            final MessageGUID guid = entry.getValue();
            StoredMessage storedMsg = m_persistentMessageStore.get(guid);
            if (condition.match(entry.getKey())) {
                results.add(storedMsg);
            }
        }

        return results;
    }

    @Override
    public MessageGUID storePublishForFuture(StoredMessage storedMessage) {
        LOG.debug("storePublishForFuture store evt {}", storedMessage);
        MessageGUID guid = new MessageGUID(UUID.randomUUID().toString());
        storedMessage.setGuid(guid);
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
        return m_persistentMessageStore.get(guid);
    }

    @Override
    public void cleanRetained(Topic topic) {
        m_retainedStore.remove(topic);
    }
}
