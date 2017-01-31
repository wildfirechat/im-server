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

package io.moquette.spi.persistence;

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
 *
 * @author andrea
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
        LOG.info("Initializing store...");
        m_retainedStore = m_db.getHashMap("retained");
        m_persistentMessageStore = m_db.getHashMap("persistedMessages");
    }

    @Override
    public void storeRetained(Topic topic, MessageGUID guid) {
        LOG.debug("Storing retained messages. Topic = {}, guid = {}.", topic, guid);
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
            LOG.trace("The retained messages have been scanned. MatchingMessages = {}.", results);
        }

        return results;
    }

    @Override
    public MessageGUID storePublishForFuture(StoredMessage storedMessage) {
        assert storedMessage.getClientID() != null : "The message to be persisted must have a valid client ID";
        MessageGUID guid = new MessageGUID(UUID.randomUUID().toString());
        storedMessage.setGuid(guid);
        LOG.debug(
                "Storing publish event. MqttClientId = {}, messageId = {}, guid = {}, topic = {}.",
                storedMessage.getClientID(),
                storedMessage.getMessageID(),
                guid,
                storedMessage.getTopic());
        m_persistentMessageStore.put(guid, storedMessage);
        ConcurrentMap<Integer, MessageGUID> messageIdToGuid = m_db
                .getHashMap(messageId2GuidsMapName(storedMessage.getClientID()));
        messageIdToGuid.put(storedMessage.getMessageID(), guid);
        return guid;
    }

    @Override
    public void dropMessagesInSession(String clientID) {
        LOG.debug("Dropping stored messages. ClientId = {}.", clientID);
        ConcurrentMap<Integer, MessageGUID> messageIdToGuid = m_db.getHashMap(messageId2GuidsMapName(clientID));
        for (MessageGUID guid : messageIdToGuid.values()) {
            removeStoredMessage(guid);
        }
        messageIdToGuid.clear();
    }

    void removeStoredMessage(MessageGUID guid) {
        // remove only the not retained and no more referenced
        StoredMessage storedMessage = m_persistentMessageStore.get(guid);
        if (!storedMessage.isRetained()) {
            LOG.debug(
                    "Dropping stored message. ClientId = {}, messageId = {}, guid = {}, topic = {}.",
                    storedMessage.getClientID(),
                    storedMessage.getMessageID(),
                    guid,
                    storedMessage.getTopic());
            m_persistentMessageStore.remove(guid);
        }
    }

    @Override
    public StoredMessage getMessageByGuid(MessageGUID guid) {
        LOG.debug("Retrieving stored message. Guid = {}.", guid);
        return m_persistentMessageStore.get(guid);
    }

    @Override
    public void cleanRetained(Topic topic) {
        LOG.debug("Cleaning retained messages. Topic = {}.", topic);
        m_retainedStore.remove(topic);
    }

    @Override
    public int getPendingPublishMessages(String clientID) {
        ConcurrentMap<Integer, MessageGUID> messageIdToGuidMap = m_db
                .getHashMap(messageId2GuidsMapName(clientID));
        return messageIdToGuidMap.size();
    }

    @Override
    public MessageGUID mapToGuid(String clientID, int messageID) {
        LOG.debug("Mapping message ID to GUID CId={}, messageId={}", clientID, messageID);
        ConcurrentMap<Integer, MessageGUID> messageIdToGuid = m_db.getHashMap(messageId2GuidsMapName(clientID));
        MessageGUID result = messageIdToGuid.get(messageID);
        LOG.debug("Message ID has been mapped to a GUID CId={}, messageId={}, guid={}", clientID, messageID, result);
        return result;
    }

    static String messageId2GuidsMapName(String clientID) {
        return "guidsMapping_" + clientID;
    }
}
