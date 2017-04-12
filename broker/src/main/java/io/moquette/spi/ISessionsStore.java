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

import io.moquette.spi.IMessagesStore.StoredMessage;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;

/**
 * Store used to handle the persistence of the subscriptions tree.
 */
public interface ISessionsStore {

    class ClientTopicCouple {

        public final Topic topicFilter;
        public final String clientID;

        public ClientTopicCouple(String clientID, Topic topicFilter) {
            this.clientID = clientID;
            this.topicFilter = topicFilter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            ClientTopicCouple that = (ClientTopicCouple) o;

            if (topicFilter != null ? !topicFilter.equals(that.topicFilter) : that.topicFilter != null)
                return false;
            return !(clientID != null ? !clientID.equals(that.clientID) : that.clientID != null);
        }

        @Override
        public int hashCode() {
            int result = topicFilter != null ? topicFilter.hashCode() : 0;
            result = 31 * result + (clientID != null ? clientID.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            return "ClientTopicCouple{" + "topicFilter='" + topicFilter + '\'' + ", clientID='" + clientID + '\'' + '}';
        }
    }

    void initStore();

    void updateCleanStatus(String clientID, boolean cleanSession);

    /**
     * Add a new subscription to the session
     *
     * @param newSubscription
     *            the subscription to add.
     */
    void addNewSubscription(Subscription newSubscription);

    /**
     * Removed a specific subscription
     *
     * @param topic
     *            the topic of the subscription.
     * @param clientID
     *            the session client.
     */
    void removeSubscription(Topic topic, String clientID);

    /**
     * Remove all the subscriptions of the session
     *
     * @param sessionID
     *            the client ID
     */
    void wipeSubscriptions(String sessionID);

    /**
     * Return all topic filters to recreate the subscription tree.
     */
    List<ClientTopicCouple> listAllSubscriptions();

    /**
     * @param couple
     *            the subscription descriptor.
     * @return the subscription stored by clientID and topicFilter, if any else null;
     */
    Subscription getSubscription(ClientTopicCouple couple);

    /*
     * @return all subscriptions stored.
     */
    List<Subscription> getSubscriptions();

    /**
     * @param clientID
     *            the session client ID.
     * @return true iff there are subscriptions persisted with clientID
     */
    boolean contains(String clientID);

    ClientSession createNewSession(String clientID, boolean cleanSession);

    /**
     * @param clientID
     *            the client owning the session.
     * @return the session for the given clientID, null if not found.
     */
    ClientSession sessionForClient(String clientID);

    /**
     * Returns all the sessions
     *
     * @return
     */
    Collection<ClientSession> getAllSessions();

    void inFlightAck(String clientID, int messageID);

    /**
     * Save the binding messageID, clientID - guid
     *
     * @param clientID
     *            the client ID
     * @param messageID
     *            the message ID
     * @param guid
     *            the uuid of the message to mark as inflight.
     */
    void inFlight(String clientID, int messageID, MessageGUID guid);

    /**
     * Return the next valid packetIdentifier for the given client session.
     *
     * @param clientID
     *            the clientID requesting next packet id.
     * @return the next valid id.
     */
    int nextPacketID(String clientID);

    /**
     * List the guids for retained messages for the session
     *
     * @param clientID
     *            the client ID owning the queue.
     * @return the list of queue message UUIDs.
     */
    BlockingQueue<StoredMessage> queue(String clientID);

    void dropQueue(String clientID);

    void moveInFlightToSecondPhaseAckWaiting(String clientID, int messageID);

    /**
     * @param clientID
     *            the client ID accessing the second phase.
     * @param messageID
     *            the message ID that reached the second phase.
     * @return the guid of message just acked.
     */
    MessageGUID secondPhaseAcknowledged(String clientID, int messageID);

    StoredMessage getInflightMessage(String clientID, int messageID);

    /**
     * Returns the number of inflight messages for the given client ID
     *
     * @param clientID target client.
     * @return count of pending in flight publish messages.
     */
    int getInflightMessagesNo(String clientID);

    /**
     * @return the inflight inbound (PUBREL for Qos2) message.
     * */
    IMessagesStore.StoredMessage inboundInflight(String clientID, int messageID);

    void markAsInboundInflight(String clientID, int messageID, MessageGUID guid);

    /**
     * Returns the size of the session queue for the given client ID
     *
     * @param clientID target client.
     * @return count of enqueued publish messages.
     */
    int getPendingPublishMessagesNo(String clientID);

    /**
     * Returns the number of second-phase ACK pending messages for the given client ID
     *
     * @param clientID target client.
     * @return count of pending in flight publish messages.
     */
    int getSecondPhaseAckPendingMessages(String clientID);

    Collection<MessageGUID> pendingAck(String clientID);
}
