/*
 * Copyright (c) 2012-2014 The original author or authors
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

    /**
     * Persist the message. 
     * If the message is empty then the topic is cleaned, else it's stored.
     */
    void storeRetained(String topic, ByteBuffer message, AbstractMessage.QOSType qos);

    /**
     * Return a list of retained messages that satisfy the condition.
     */
    Collection<StoredMessage> searchMatching(IMatchingCondition condition);

    void storePublishForFuture(PublishEvent evt);

    /**
     * Return the list of persisted publishes for the given clientID.
     * For QoS1 and QoS2 with clean session flag, this method return the list of 
     * missed publish events while the client was disconnected.
     */
    List<PublishEvent> retrievePersistedPublishes(String clientID);
    
    void cleanPersistedPublishMessage(String clientID, int messageID);

    void cleanPersistedPublishes(String clientID);

    void cleanInFlight(String msgID);

    void addInFlight(PublishEvent evt, String publishKey);

    void close();

    void persistQoS2Message(String publishKey, PublishEvent evt);

    void removeQoS2Message(String publishKey);

    PublishEvent retrieveQoS2Message(String publishKey);
}
