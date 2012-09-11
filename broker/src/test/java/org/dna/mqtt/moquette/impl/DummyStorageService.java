package org.dna.mqtt.moquette.messaging.spi.impl;

import org.dna.mqtt.moquette.messaging.spi.IMatchingCondition;
import org.dna.mqtt.moquette.messaging.spi.IStorageService;
import org.dna.mqtt.moquette.messaging.spi.impl.events.PublishEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.Subscription;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 */
public class DummyStorageService implements IStorageService {
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
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeAllSubscriptions(String clientID) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<Subscription> retrieveAllSubscriptions() {
        return Collections.EMPTY_LIST;
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
