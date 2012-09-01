package org.dna.mqtt.moquette.messaging.spi;

import org.dna.mqtt.moquette.messaging.spi.impl.SimpleMessaging;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import static org.dna.mqtt.moquette.messaging.spi.impl.SimpleMessaging.StoredMessage;

import java.util.Collection;

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

}
