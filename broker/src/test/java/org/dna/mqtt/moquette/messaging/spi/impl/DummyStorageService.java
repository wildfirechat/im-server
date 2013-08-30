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

/**
 */
public class DummyStorageService implements IStorageService {
    
    private Map<String, Set<Subscription>> m_persistentSubscriptions = new HashMap<String, Set<Subscription>>();
    
    public void initStore() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void storeRetained(String topic, byte[] message, AbstractMessage.QOSType qos) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<HawtDBStorageService.StoredMessage> searchMatching(IMatchingCondition condition) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void storePublishForFuture(PublishEvent evt) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<PublishEvent> retrivePersistedPublishes(String clientID) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void cleanPersistedPublishes(String clientID) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void cleanInFlight(String msgID) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addInFlight(PublishEvent evt, String publishKey) {
        //To change body of implemented methods use File | Settings | File Templates.
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
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeQoS2Message(String publishKey) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public PublishEvent retrieveQoS2Message(String publishKey) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
