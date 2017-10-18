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

package io.moquette.spi;

import io.moquette.persistence.PersistentSession;
import io.moquette.spi.IMessagesStore.StoredMessage;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Queue;
import java.util.Set;

/**
 * Store used to handle the persistence of the subscriptions tree.
 */
public interface ISessionsStore {

    void initStore();

    ISubscriptionsStore subscriptionStore();

    /**
     * @param clientID
     *            the session client ID.
     * @return true iff there is a session stored for the client
     */
    boolean contains(String clientID);

    void createNewDurableSession(String clientID);

    void removeDurableSession(String clientId);

    void updateCleanStatus(String clientID, boolean cleanSession);

    PersistentSession loadSessionByKey(String clientID);

    /**
     * Returns all the sessions
     *
     * @return the collection of all stored client sessions.
     */
    Collection<PersistentSession> listAllSessions();

    StoredMessage inFlightAck(String clientID, int messageID);

    /**
     * Save the message msg with  messageID, clientID as in flight
     *
     * @param clientID
     *            the client ID
     * @param messageID
     *            the message ID
     * @param msg
     *            the message to put in flight zone
     */
    void inFlight(String clientID, int messageID, StoredMessage msg);

    /**
     * Return the next valid packetIdentifier for the given client session.
     *
     * @param clientID
     *            the clientID requesting next packet id.
     * @return the next valid id.
     */
    int nextPacketID(String clientID);

    /**
     * List the published retained messages for the session
     *
     * @param clientID
     *            the client ID owning the queue.
     * @return the queue of messages.
     */
    Queue<StoredMessage> queue(String clientID);

    void dropQueue(String clientID);

    void moveInFlightToSecondPhaseAckWaiting(String clientID, int messageID, StoredMessage msg);

    /**
     * @param clientID
     *            the client ID accessing the second phase.
     * @param messageID
     *            the message ID that reached the second phase.
     * @return the guid of message just acked.
     */
    StoredMessage completeReleasedPublish(String clientID, int messageID);

    /**
     * Returns the number of inflight messages for the given client ID
     *
     * @param clientID target client.
     * @return count of pending in flight publish messages.
     */
    int getInflightMessagesNo(String clientID);

    /**
     * Returns the number of second-phase ACK pending messages for the given client ID
     *
     * @param clientID target client.
     * @return count of pending in flight publish messages.
     */
    int countPubReleaseWaitingPubComplete(String clientID);

    void removeTemporaryQoS2(String clientID);

    /**
     * Tracks the closing time of a persistent session
     * */
    void trackSessionClose(LocalDateTime when, String clientID);

    /**
     * List the sessions ids closed before the pin date.
     * */
    Set<String> sessionOlderThan(LocalDateTime queryPin);
}
