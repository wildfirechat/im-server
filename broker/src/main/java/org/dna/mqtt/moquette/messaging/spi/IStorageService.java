package org.dna.mqtt.moquette.messaging.spi;

import java.nio.ByteBuffer;
import org.dna.mqtt.moquette.messaging.spi.impl.events.PublishEvent;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import static org.dna.mqtt.moquette.messaging.spi.impl.HawtDBStorageService.StoredMessage;

import java.util.Collection;
import java.util.List;

/**
 * Defines the SPI to be implemented by a StorageService that handle persistence of messages and subscriptions.
 */
public interface IStorageService extends IPersistentSubscriptionStore {

    /**
     * Used to initialize all persistent store structures
     * */
    void initStore();

    void storeRetained(String topic, ByteBuffer message, AbstractMessage.QOSType qos);

    Collection<StoredMessage> searchMatching(IMatchingCondition condition);

    void storePublishForFuture(PublishEvent evt);

    List<PublishEvent> retrivePersistedPublishes(String clientID);
    
    void cleanPersistedPublishMessage(String clientID, int messageID);

    void cleanPersistedPublishes(String clientID);

    void cleanInFlight(String msgID);

    void addInFlight(PublishEvent evt, String publishKey);

    void close();

    void persistQoS2Message(String publishKey, PublishEvent evt);

    void removeQoS2Message(String publishKey);

    PublishEvent retrieveQoS2Message(String publishKey);
}
