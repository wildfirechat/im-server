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

package org.eclipse.moquette.spi.persistence;

import org.eclipse.moquette.proto.MQTTException;
import org.eclipse.moquette.spi.IMatchingCondition;
import org.eclipse.moquette.spi.IMessagesStore;
import org.eclipse.moquette.spi.ISessionsStore;
import org.eclipse.moquette.spi.impl.events.PublishEvent;
import org.eclipse.moquette.spi.impl.storage.StoredPublishEvent;
import org.eclipse.moquette.spi.impl.subscriptions.Subscription;
import org.eclipse.moquette.proto.messages.AbstractMessage;

import static org.eclipse.moquette.spi.impl.Utils.defaultGet;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

/**
 * MapDB main persistence implementation
 */
public class MapDBPersistentStore implements IMessagesStore, ISessionsStore {

    private static final Logger LOG = LoggerFactory.getLogger(MapDBPersistentStore.class);

    private ConcurrentMap<String, StoredMessage> m_retainedStore;
    //maps clientID to the list of pending messages stored
    private ConcurrentMap<String, List<StoredPublishEvent>> m_persistentMessageStore;
    //bind clientID+MsgID -> evt message published
    private ConcurrentMap<String, StoredPublishEvent> m_inflightStore;
    //map clientID <-> set of currently in flight packet identifiers
    Map<String, Set<Integer>> m_inFlightIds;
    //bind clientID+MsgID -> evt message published
    private ConcurrentMap<String, StoredPublishEvent> m_qos2Store;
    //persistent Map of clientID, set of Subscriptions
    private ConcurrentMap<String, Set<Subscription>> m_persistentSubscriptions;
    private DB m_db;
    private String m_storePath;

    protected final ScheduledExecutorService m_scheduler = Executors.newScheduledThreadPool(1);

    /*
     * The default constructor will create an in memory store as no file path was specified
     */
    
    public MapDBPersistentStore() {
    	this.m_storePath = "";
    }
    
    public MapDBPersistentStore(String storePath) {
        this.m_storePath = storePath;
    }
    
    @Override
    public void initStore() {
    	if (m_storePath == null || m_storePath.isEmpty()) {
    		m_db = DBMaker.newMemoryDB().make();
    	} else {
	        File tmpFile;
	        try {
	            tmpFile = new File(m_storePath);
	            boolean fileNewlyCreated = tmpFile.createNewFile();
                LOG.info("Starting with {} [{}] db file", fileNewlyCreated ? "fresh" : "existing", m_storePath);
	        } catch (IOException ex) {
	            LOG.error(null, ex);
	            throw new MQTTException("Can't create temp file for subscriptions storage [" + m_storePath + "]", ex);
	        }
	        m_db = DBMaker.newFileDB(tmpFile).make();
    	}
        m_retainedStore = m_db.getHashMap("retained");
        m_persistentMessageStore = m_db.getHashMap("persistedMessages");
        m_inflightStore = m_db.getHashMap("inflight");
        m_inFlightIds = m_db.getHashMap("inflightPacketIDs");
        m_persistentSubscriptions = m_db.getHashMap("subscriptions");
        m_qos2Store = m_db.getHashMap("qos2Store");
        m_scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                m_db.commit();
            }
        }, 30, 30, TimeUnit.SECONDS);
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

        List<StoredMessage> results = new ArrayList<>();

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
        List<StoredPublishEvent> storedEvents;
        String clientID = evt.getClientID();
        if (!m_persistentMessageStore.containsKey(clientID)) {
            storedEvents = new ArrayList<>();
        } else {
            storedEvents = m_persistentMessageStore.get(clientID);
        }
        storedEvents.add(convertToStored(evt));
        m_persistentMessageStore.put(clientID, storedEvents);
        //NB rewind the evt message content
        LOG.debug("Stored published message for client <{}> on topic <{}>", clientID, evt.getTopic());
    }

    @Override
    public List<PublishEvent> listMessagesInSession(String clientID) {
        List<PublishEvent> liveEvts = new ArrayList<>();
        List<StoredPublishEvent> storedEvts = defaultGet(m_persistentMessageStore, clientID, Collections.<StoredPublishEvent>emptyList());

        for (StoredPublishEvent storedEvt : storedEvts) {
            liveEvts.add(convertFromStored(storedEvt));
        }
        return liveEvts;
    }

    @Override
    public void removeMessageInSession(String clientID, Integer messageID) {
        List<StoredPublishEvent> events = m_persistentMessageStore.get(clientID);
        if (events == null) {
            return;
        }
        StoredPublishEvent toRemoveEvt = null;
        for (StoredPublishEvent evt : events) {
            if (evt.getMessageID() == null && messageID == null) {
                //was a qos0 message (no ID)
                toRemoveEvt = evt;
            }
            if (evt.getMessageID().equals(messageID)) {
                toRemoveEvt = evt;
            }
        }
        events.remove(toRemoveEvt);
        m_persistentMessageStore.put(clientID, events);
    }

    public void dropMessagesInSession(String clientID) {
        m_persistentMessageStore.remove(clientID);
    }

    //----------------- In flight methods -----------------
    @Override
    public void cleanTemporaryPublish(String clientID, int packetID) {
        String publishKey = String.format("%s%d", clientID, packetID);
        m_inflightStore.remove(publishKey);
        Set<Integer> inFlightForClient = this.m_inFlightIds.get(clientID);
        if (inFlightForClient != null) {
            inFlightForClient.remove(packetID);
        }
    }

    @Override
    public void storeTemporaryPublish(PublishEvent evt, String clientID, int packetID) {
        String publishKey = String.format("%s%d", clientID, packetID);
        StoredPublishEvent storedEvt = convertToStored(evt);
        m_inflightStore.put(publishKey, storedEvt);
    }

    /**
     * Return the next valid packetIdentifier for the given client session.
     * */
    @Override
    public int nextPacketID(String clientID) {
        Set<Integer> inFlightForClient = this.m_inFlightIds.get(clientID);
        if (inFlightForClient == null) {
            int nextPacketId = 1;
            inFlightForClient = new HashSet<>();
            inFlightForClient.add(nextPacketId);
            this.m_inFlightIds.put(clientID, inFlightForClient);
            return nextPacketId;
        }
        int maxId = inFlightForClient.isEmpty() ? 0 : Collections.max(inFlightForClient);
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
        m_persistentSubscriptions.put(clientID, clientSubscriptions);
    }

    @Override
    public void addNewSubscription(Subscription newSubscription) {
        LOG.debug("addNewSubscription invoked with subscription {}", newSubscription);
        final String clientID = newSubscription.getClientId();
        if (!m_persistentSubscriptions.containsKey(clientID)) {
            LOG.debug("clientID {} is a newcome, creating it's subscriptions set", clientID);
            m_persistentSubscriptions.put(clientID, new HashSet<Subscription>());
        }

        Set<Subscription> subs = m_persistentSubscriptions.get(clientID);
        if (!subs.contains(newSubscription)) {
            LOG.debug("updating clientID {} subscriptions set with new subscription", clientID);
            //TODO check the subs doesn't contain another subscription to the same topic with different
            Subscription existingSubscription = null;
            for (Subscription scanSub : subs) {
                if (newSubscription.getTopicFilter().equals(scanSub.getTopicFilter())) {
                    existingSubscription = scanSub;
                    break;
                }
            }
            if (existingSubscription != null) {
                subs.remove(existingSubscription);
            }
            subs.add(newSubscription);
            m_persistentSubscriptions.put(clientID, subs);
            LOG.debug("clientID {} subscriptions set now is {}", clientID, subs);
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

    public List<Subscription> listAllSubscriptions() {
        List<Subscription> allSubscriptions = new ArrayList<>();
        for (Map.Entry<String, Set<Subscription>> entry : m_persistentSubscriptions.entrySet()) {
            allSubscriptions.addAll(entry.getValue());
        }
        LOG.debug("retrieveAllSubscriptions returning subs {}", allSubscriptions);
        return allSubscriptions;
    }

    @Override
    public boolean contains(String clientID) {
        return m_persistentSubscriptions.containsKey(clientID);
    }

    @Override
    public void createNewSession(String clientID) {
        LOG.debug("createNewSession for client <{}>", clientID);
        if (m_persistentSubscriptions.containsKey(clientID)) {
            LOG.error("already exists a session for client <{}>", clientID);
            return;
        }
        LOG.debug("clientID {} is a newcome, creating it's empty subscriptions set", clientID);
        m_persistentSubscriptions.put(clientID, new HashSet<Subscription>());
    }

    @Override
    public void close() {
        if (this.m_db.isClosed()) {
            LOG.debug("already closed");
            return;
        }
        this.m_db.commit();
        LOG.debug("persisted subscriptions {}", m_persistentSubscriptions);
        this.m_db.close();
        LOG.debug("closed disk storage");
        this.m_scheduler.shutdown();
        LOG.debug("Persistence commit scheduler is shutdown");
    }

    /*-------- QoS 2  storage management --------------*/
    @Override
    public void persistQoS2Message(String publishKey, PublishEvent evt) {
        LOG.debug("persistQoS2Message store pubKey: {}, evt: {}", publishKey, evt);
        m_qos2Store.put(publishKey, convertToStored(evt));
    }

    @Override
    public void removeQoS2Message(String publishKey) {
        LOG.debug("Removing stored Q0S2 message <{}>", publishKey);
        m_qos2Store.remove(publishKey);
    }

    public PublishEvent retrieveQoS2Message(String publishKey) {
        StoredPublishEvent storedEvt = m_qos2Store.get(publishKey);
        return convertFromStored(storedEvt);
    }

    private StoredPublishEvent convertToStored(PublishEvent evt) {
        return new StoredPublishEvent(evt);
    }

    private PublishEvent convertFromStored(StoredPublishEvent evt) {
        byte[] message = evt.getMessage();
        ByteBuffer bbmessage = ByteBuffer.wrap(message);
        //bbmessage.flip();
        return new PublishEvent(evt.getTopic(), evt.getQos(),
                bbmessage, evt.isRetain(), evt.getClientID(), evt.getMessageID());
    }
}
