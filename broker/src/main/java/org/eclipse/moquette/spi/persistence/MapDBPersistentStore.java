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
import org.eclipse.moquette.proto.messages.AbstractMessage;
import org.eclipse.moquette.spi.ClientSession;
import org.eclipse.moquette.spi.IMatchingCondition;
import org.eclipse.moquette.spi.IMessagesStore;
import org.eclipse.moquette.spi.ISessionsStore;
import org.eclipse.moquette.spi.impl.events.PublishEvent;
import org.eclipse.moquette.spi.impl.storage.StoredPublishEvent;
import org.eclipse.moquette.spi.impl.subscriptions.Subscription;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.eclipse.moquette.spi.impl.Utils.defaultGet;

/**
 * MapDB main persistence implementation
 */
public class MapDBPersistentStore implements IMessagesStore, ISessionsStore {

    /**
     * This is a DTO used to persist minimal status (clean session and activation status) of
     * a session.
     * */
    public static class PersistentSession implements Serializable {
        public final boolean cleanSession;
        public final boolean active;

        public PersistentSession(boolean cleanSession, boolean active) {
            this.cleanSession = cleanSession;
            this.active = active;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(MapDBPersistentStore.class);

    private ConcurrentMap<String, StoredMessage> m_retainedStore;
    //maps guid to message, it's message store
    private ConcurrentMap<String, StoredPublishEvent> m_persistentMessageStore;
    //maps clientID->[MessageId -> guid]
    private ConcurrentMap<String, Map<Integer, String>> m_inflightStore;
    //map clientID <-> set of currently in flight packet identifiers
    Map<String, Set<Integer>> m_inFlightIds;
    //bind clientID+MsgID -> evt message published
    private ConcurrentMap<String, StoredPublishEvent> m_qos2Store;
    //persistent Map of clientID, set of Subscriptions
    private ConcurrentMap<String, Set<Subscription>> m_persistentSubscriptions;
    private ConcurrentMap<String, PersistentSession> m_persistentSessions;

    //maps clientID->[guid*], insertion order cares, it's queue
    private ConcurrentMap<String, List<String>> m_enqueuedStore;
    //maps clientID->[messageID*]
    private ConcurrentMap<String, Set<Integer>> m_secondPhaseStore;

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
        m_persistentSessions = m_db.getHashMap("sessions");
        m_enqueuedStore = m_db.getHashMap("sessionQueue");
        m_secondPhaseStore = m_db.getHashMap("secondPhase");
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
    public String storePublishForFuture(PublishEvent evt) {
        LOG.debug("storePublishForFuture store evt {}", evt);
        String guid = UUID.randomUUID().toString();
        evt.setGuid(guid);
        m_persistentMessageStore.put(guid, convertToStored(evt));
        return guid;
    }

    @Override
    public List<PublishEvent> listMessagesInSession(Collection<String> guids) {
        List<PublishEvent> ret = new ArrayList<>();
        for (String guid : guids) {
            ret.add(convertFromStored(m_persistentMessageStore.get(guid))) ;
        }
        return ret;
    }

    public void dropMessagesInSession(String clientID) {
        m_persistentMessageStore.remove(clientID);
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
        if (LOG.isTraceEnabled()) {
            LOG.trace("persisted subscriptions {}", m_persistentSubscriptions);
        }
    }

    @Override
    public void addNewSubscription(Subscription newSubscription) {
        LOG.debug("addNewSubscription invoked with subscription {}", newSubscription);
        final String clientID = newSubscription.getClientId();
        if (!m_persistentSubscriptions.containsKey(clientID)) {
            LOG.debug("subscriptions for clientID <{}> not present, empty subscriptions set", clientID);
            m_persistentSubscriptions.put(clientID, new HashSet<Subscription>());
        }

        Set<Subscription> subs = m_persistentSubscriptions.get(clientID);
        if (!subs.contains(newSubscription)) {
            LOG.debug("updating <{}> subscriptions set with new subscription", clientID);
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
    public ClientSession createNewSession(String clientID, boolean cleanSession) {
        LOG.debug("createNewSession for client <{}> with clean flag <{}>", clientID, cleanSession);
        if (m_persistentSessions.containsKey(clientID)) {
            LOG.error("already exists a session for client <{}>, bad condition", clientID);
            throw new IllegalArgumentException("Can't create a session with the ID of an already existing" + clientID);
        }
        LOG.debug("clientID {} is a newcome, creating it's empty subscriptions set", clientID);
        m_persistentSubscriptions.put(clientID, new HashSet<Subscription>());
        m_persistentSessions.putIfAbsent(clientID, new PersistentSession(cleanSession, false));
        return new ClientSession(clientID, this, this, cleanSession);
    }

    @Override
    public ClientSession sessionForClient(String clientID) {
        if (!m_persistentSessions.containsKey(clientID)) {
            return null;
        }

        PersistentSession storedSession = m_persistentSessions.get(clientID);
        ClientSession clientSession = new ClientSession(clientID, this, this, storedSession.cleanSession);
        if (storedSession.active) {
            clientSession.activate();
        }
        return clientSession;
    }

    @Override
    public void activate(String clientID) {
        activationHelper(clientID, true);
    }

    @Override
    public void deactivate(String clientID) {
        activationHelper(clientID, false);
    }

    @Override
    public void inFlightAck(String clientID, int messageID) {
        Map<Integer, String> m = this.m_inflightStore.get(clientID);
        if (m == null) {
            LOG.error("Can't find the inFlight record for client <{}>", clientID);
            return;
        }
        m.remove(messageID);

        //remove from the ids store
        Set<Integer> inFlightForClient = this.m_inFlightIds.get(clientID);
        if (inFlightForClient != null) {
            inFlightForClient.remove(messageID);
        }
    }

    @Override
    public void inFlight(String clientID, int messageID, String guid) {
        Map<Integer, String> m = this.m_inflightStore.get(clientID);
        if (m == null) {
            m = new HashMap<>();
        }
        m.put(messageID, guid);
        this.m_inflightStore.put(clientID, m);
    }

    @Override
    public void bindToDeliver(String guid, String clientID) {
        List<String> guids = defaultGet(m_enqueuedStore, clientID, new ArrayList<String>());
        guids.add(guid);
        m_enqueuedStore.put(clientID, guids);
    }

    @Override
    public Collection<String> enqueued(String clientID) {
        return defaultGet(m_enqueuedStore, clientID, new ArrayList<String>());
    }

    @Override
    public void removeEnqueued(String clientID, String guid) {
        List<String> guids = defaultGet(m_enqueuedStore, clientID, new ArrayList<String>());
        guids.remove(guid);
        m_enqueuedStore.put(clientID, guids);
    }

    @Override
    public void secondPhaseAcknowledged(String clientID, int messageID) {
        Set<Integer> messageIDs = defaultGet(m_secondPhaseStore, clientID, new HashSet<Integer>());
        messageIDs.remove(messageID);
        m_secondPhaseStore.put(clientID, messageIDs);
    }

    @Override
    public void secondPhaseAckWaiting(String clientID, int messageID) {
        Set<Integer> messageIDs = defaultGet(m_secondPhaseStore, clientID, new HashSet<Integer>());
        messageIDs.add(messageID);
        m_secondPhaseStore.put(clientID, messageIDs);
    }

    private void activationHelper(String clientID, boolean activation) {
        PersistentSession storedSession = m_persistentSessions.get(clientID);
        if (storedSession == null) {
            throw new IllegalStateException((activation ? "activating" : "deactivating") + " a session never stored/created, clientID <"+ clientID + ">", null);
        }
        PersistentSession newStoredSession = new PersistentSession(storedSession.cleanSession, activation);
        m_persistentSessions.put(clientID, newStoredSession);
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
        PublishEvent pubEvt = new PublishEvent(evt.getTopic(), evt.getQos(),
                bbmessage, evt.isRetain(), evt.getClientID(), evt.getMessageID());
        pubEvt.setGuid(evt.getGuid());
        return pubEvt;
    }
}
