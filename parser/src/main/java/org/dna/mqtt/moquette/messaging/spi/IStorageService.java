package org.dna.mqtt.moquette.messaging.spi;

import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.Subscription;
import org.dna.mqtt.moquette.messaging.spi.impl.events.PublishEvent;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import static org.dna.mqtt.moquette.messaging.spi.impl.HawtDBStorageService.StoredMessage;

import java.util.Collection;
import java.util.List;

/**
 * Defines the SPI to be implemented by a StorageService that handle persistence of messages and subscriptions.
 */
public interface IStorageService {

    /**
     * Used to initialize all persistent store structures
     * */
    void initStore();

    void storeRetained(String topic, byte[] message, AbstractMessage.QOSType qos);

    Collection<StoredMessage> searchMatching(IMatchingCondition condition);

    void storePublishForFuture(PublishEvent evt);

    List<PublishEvent> retrivePersistedPublishes(String clientID);

    void cleanPersistedPublishes(String clientID);

    void cleanInFlight(String msgID);

    void addInFlight(PublishEvent evt, String publishKey);

    void addNewSubscription(Subscription newSubscription, String clientID);

    void removeAllSubscriptions(String clientID);

    List<Subscription> retrieveAllSubscriptions();

    void close();

    void persistQoS2Message(String publishKey, PublishEvent evt);

    void removeQoS2Message(String publishKey);

    PublishEvent retrieveQoS2Message(String publishKey);
}
