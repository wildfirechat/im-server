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
package io.moquette.spi;

import java.util.Collection;
import java.util.List;

import io.moquette.spi.impl.subscriptions.Subscription;

/**
 * Store used to handle the persistence of the subscriptions tree.
 *
 * @author andrea
 */
public interface ISessionsStore {

    void updateCleanStatus(String clientID, boolean cleanSession);

    class ClientTopicCouple {
        public final String topicFilter;
        public final String clientID;

        public ClientTopicCouple(String clientID, String topicFilter) {
            this.clientID = clientID;
            this.topicFilter = topicFilter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ClientTopicCouple that = (ClientTopicCouple) o;

            if (topicFilter != null ? !topicFilter.equals(that.topicFilter) : that.topicFilter != null) return false;
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
            return "ClientTopicCouple{" +
                    "topicFilter='" + topicFilter + '\'' +
                    ", clientID='" + clientID + '\'' +
                    '}';
        }

    }

    void initStore();

    /**
     * Add a new subscription to the session
     */
    void addNewSubscription(Subscription newSubscription);

    /**
     * Removed a specific subscription
     * */
    void removeSubscription(String topic, String clientID);

    /**
     * Remove all the subscriptions of the session
     */
    void wipeSubscriptions(String sessionID);

    /**
     * Return all topic filters to recreate the subscription tree.
     * */
    List<ClientTopicCouple> listAllSubscriptions();

    /**
     * @return the subscription stored by clientID and topicFilter, if any else null;
     * */
    Subscription getSubscription(ClientTopicCouple couple);

    /**
     * @return true iff there are subscriptions persisted with clientID
     */
    boolean contains(String clientID);

    ClientSession createNewSession(String clientID, boolean cleanSession);

    /**
     * @param clientID the client owning the session.
     * @return the session for the given clientID, null if not found.
     * */
    ClientSession sessionForClient(String clientID);

    void inFlightAck(String clientID, int messageID);

    /**
     * Save the binding messageID, clientID <-> guid
     * */
    void inFlight(String clientID, int messageID, String guid);

    /**
     * Return the next valid packetIdentifier for the given client session.
     * */
    int nextPacketID(String clientID);

    /**
     * Store the guid to be later published.
     * */
    void bindToDeliver(String guid, String clientID);

    /**
     * List the guids for retained messages for the session
     * */
    Collection<String> enqueued(String clientID);

    /**
     * Remove form the queue of stored messages for session.
     * */
    void removeEnqueued(String clientID, String guid);

    void secondPhaseAcknowledged(String clientID, int messageID);

    void secondPhaseAckWaiting(String clientID, int messageID);

    String mapToGuid(String clientID, int messageID);
}
