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

import cn.wildfirechat.proto.WFCMessage;
import io.moquette.persistence.MemorySessionStore.Session;
import io.moquette.spi.IMessagesStore.StoredMessage;
import cn.wildfirechat.common.ErrorCode;

import java.util.Collection;
import java.util.Queue;

/**
 * Store used to handle the persistence of the subscriptions tree.
 */
public interface ISessionsStore {

    void initStore();

    /**
     * @param clientID
     *            the session client ID.
     * @return true iff there are subscriptions persisted with clientID
     */
    boolean contains(String clientID);

    Session updateOrCreateUserSession(String username, String clientID, int platform);

    ErrorCode loadActiveSession(String username, String clientID);

    ClientSession updateExistSession(String username, String clientID, WFCMessage.RouteRequest endpoint, boolean cleanSession);

    Session getSession(String clientID);

    void cleanDuplatedToken(String cid, int pushType, String token, boolean isVoip, String packageName);

    void updateSessionToken(Session session, boolean voip);

    void clearUserSession(String username);

    void kickoffUserClient(String userId, String clientId);
    /**
     * @param clientID
     *            the client owning the session.
     * @return the session for the given clientID, null if not found.
     */

    Session sessionForClientAndUser(String username, String clientID);

    ClientSession sessionForClient(String clientID);

    void loadUserSession(String username, String clientID);
    
    Collection<Session> sessionForUser(String username);

    /**
     * Returns all the sessions
     *
     * @return the collection of all stored client sessions.
     */
    Collection<ClientSession> getAllSessions();

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
    StoredMessage secondPhaseAcknowledged(String clientID, int messageID);

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

    void markAsInboundInflight(String clientID, int messageID, StoredMessage msg);

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

    void disableSession(String userId, String clientId);
    void cleanSession(String userId, String clientID);

    boolean isMultiEndpointSupported();

    ErrorCode kickoffPCClient(String operator, String pcClientId);
}
