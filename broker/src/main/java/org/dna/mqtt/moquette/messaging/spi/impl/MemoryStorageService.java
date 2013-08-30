package org.dna.mqtt.moquette.messaging.spi.impl;

import java.util.ArrayList;
import org.dna.mqtt.moquette.messaging.spi.IMatchingCondition;
import org.dna.mqtt.moquette.messaging.spi.IStorageService;
import org.dna.mqtt.moquette.messaging.spi.impl.events.PublishEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.Subscription;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.dna.mqtt.moquette.messaging.spi.impl.HawtDBStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class MemoryStorageService implements IStorageService {
    
    private Map<String, Set<Subscription>> m_persistentSubscriptions = new HashMap<String, Set<Subscription>>();
    private Map<String, HawtDBStorageService.StoredMessage> m_retainedStore = new HashMap<String, HawtDBStorageService.StoredMessage>();
    private Map<String, List<PublishEvent>> m_persistentMessageStore = new HashMap<String, List<PublishEvent>>();
    private Map<String, PublishEvent> m_inflightStore = new HashMap<String, PublishEvent>();
    private Map<String, PublishEvent> m_qos2Store = new HashMap<String, PublishEvent>();
    
    private static final Logger LOG = LoggerFactory.getLogger(MemoryStorageService.class);
    
    public void initStore() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void storeRetained(String topic, byte[] message, AbstractMessage.QOSType qos) {
        if (message.length == 0) {
            //clean the message from topic
            m_retainedStore.remove(topic);
        } else {
            //store the message to the topic
            m_retainedStore.put(topic, new HawtDBStorageService.StoredMessage(message, qos, topic));
        }
    }

    public Collection<HawtDBStorageService.StoredMessage> searchMatching(IMatchingCondition condition) {
        LOG.debug("searchMatching scanning all retained messages, presents are " + m_retainedStore.size());

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
        LOG.debug("storePublishForFuture store evt " + evt);
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

    public List<PublishEvent> retrivePersistedPublishes(String clientID) {
        return m_persistentMessageStore.get(clientID);
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
        LOG.debug(String.format("persistQoS2Message store pubKey %s, evt %s", publishKey, evt));
        m_qos2Store.put(publishKey, evt);
    }

    public void removeQoS2Message(String publishKey) {
        m_qos2Store.remove(publishKey);
    }

    public PublishEvent retrieveQoS2Message(String publishKey) {
        return m_qos2Store.get(publishKey);
    }
}
