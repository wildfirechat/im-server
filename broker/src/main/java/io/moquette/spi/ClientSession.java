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

import io.moquette.spi.ISubscriptionsStore.ClientTopicCouple;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

/**
 * Model a Session like describe on page 25 of MQTT 3.1.1 specification:
 * The Session state in the Server consists of:
 * <ul>
 *     <li>The existence of a Session, even if the rest of the Session state is empty.</li>
 *     <li>The Client’s subscriptions.</li>
 *     <li>QoS 1 and QoS 2 messages which have been sent to the Client, but have not been
 *     completely acknowledged.</li>
 *     <li>QoS 1 and QoS 2 messages pending transmission to the Client.</li>
 *     <li>QoS 2 messages which have been received from the Client, but have not been
 *     completely acknowledged.</li>
 *     <li>Optionally, QoS 0 messages pending transmission to the Client.</li>
 * </ul>
 */
public class ClientSession {

    class OutboundFlightZone {

        /**
         * Save the binding messageID, clientID - message
         *
         * @param messageID the packet ID used in transmission
         * @param msg the message to put in flight zone
         */
        void waitingAck(int messageID, IMessagesStore.StoredMessage msg) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Adding to inflight {}, guid <{}>", messageID, msg.getGuid());
            }
            m_sessionsStore.inFlight(ClientSession.this.clientID, messageID, msg);
        }

        IMessagesStore.StoredMessage acknowledged(int messageID) {
            if (LOG.isTraceEnabled())
                LOG.trace("Acknowledging inflight, clientID <{}> messageID {}", ClientSession.this.clientID, messageID);
            return m_sessionsStore.inFlightAck(ClientSession.this.clientID, messageID);
        }
    }

    class InboundFlightZone {

        public IMessagesStore.StoredMessage lookup(int messageID) {
            return m_sessionsStore.inboundInflight(clientID, messageID);
        }

        public void waitingRel(int messageID, IMessagesStore.StoredMessage msg) {
            m_sessionsStore.markAsInboundInflight(clientID, messageID, msg);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(ClientSession.class);

    public final String clientID;

    private final ISessionsStore m_sessionsStore;

    private Set<Subscription> subscriptions = new HashSet<>();

    private final ISubscriptionsStore subscriptionsStore;
    private volatile boolean cleanSession;

    // private BlockingQueue<AbstractMessage> m_queueToPublish = new
    // ArrayBlockingQueue<>(Constants.MAX_MESSAGE_QUEUE);
    private final OutboundFlightZone outboundFlightZone;
    private final InboundFlightZone inboundFlightZone;

    public ClientSession(String clientID, ISessionsStore sessionsStore, ISubscriptionsStore subscriptionsStore,
                         boolean cleanSession) {
        this.clientID = clientID;
        this.m_sessionsStore = sessionsStore;
        this.subscriptionsStore = subscriptionsStore;
        this.cleanSession = cleanSession;
        this.outboundFlightZone = new OutboundFlightZone();
        this.inboundFlightZone = new InboundFlightZone();
    }

    /**
     * Return the list of persisted publishes for the given clientID. For QoS1 and QoS2 with clean
     * session flag, this method return the list of missed publish events while the client was
     * disconnected.
     *
     * @return the list of messages to be delivered for client related to the session.
     */
    public BlockingQueue<IMessagesStore.StoredMessage> queue() {
        LOG.info("Retrieving enqueued messages. ClientId={}", clientID);
        return this.m_sessionsStore.queue(clientID);
    }

    @Override
    public String toString() {
        return "ClientSession{clientID='" + clientID + '\'' + "}";
    }

    public boolean subscribe(Subscription newSubscription) {
        LOG.info("Adding new subscription. ClientId={}, topics={}, qos={}", newSubscription.getClientId(),
            newSubscription.getTopicFilter(), newSubscription.getRequestedQos());
        boolean validTopic = newSubscription.getTopicFilter().isValid();
        if (!validTopic) {
            LOG.warn("The topic filter is not valid. ClientId={}, topics={}", newSubscription.getClientId(),
                newSubscription.getTopicFilter());
            // send SUBACK with 0x80 for this topic filter
            return false;
        }
        ClientTopicCouple matchingCouple = new ClientTopicCouple(this.clientID, newSubscription.getTopicFilter());
        Subscription existingSub = subscriptionsStore.getSubscription(matchingCouple);
        // update the selected subscriptions if not present or if has a greater qos
        if (existingSub == null || existingSub.getRequestedQos().value() < newSubscription.getRequestedQos().value()) {
            if (existingSub != null) {
                LOG.info("Subscription already existed with a lower QoS value. It will be updated. ClientId={}, " +
                    "topics={}, existingQos={}, newQos={}", newSubscription.getClientId(),
                    newSubscription.getTopicFilter(), existingSub.getRequestedQos(), newSubscription.getRequestedQos());
                subscriptions.remove(newSubscription);
            }
            subscriptions.add(newSubscription);
            subscriptionsStore.addNewSubscription(newSubscription);
        }
        return true;
    }

    public void unsubscribeFrom(Topic topicFilter) {
        LOG.info("Removing subscription. ClientID={}, topics={}", clientID, topicFilter);
        subscriptionsStore.removeSubscription(topicFilter, clientID);
        Set<Subscription> subscriptionsToRemove = new HashSet<>();
        for (Subscription sub : this.subscriptions) {
            if (sub.getTopicFilter().equals(topicFilter)) {
                subscriptionsToRemove.add(sub);
            }
        }
        subscriptions.removeAll(subscriptionsToRemove);
    }

    public void disconnect() {
        if (this.cleanSession) {
            LOG.info("Client disconnected. Removing its subscriptions. ClientId={}", clientID);
            // cleanup topic subscriptions
            cleanSession();
        }
    }

    public void cleanSession() {
        m_sessionsStore.cleanSession(this.clientID);
    }

    public boolean isCleanSession() {
        return this.cleanSession;
    }

    public void cleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
        this.m_sessionsStore.updateCleanStatus(this.clientID, cleanSession);
    }

    public int nextPacketId() {
        return this.m_sessionsStore.nextPacketID(this.clientID);
    }

    public IMessagesStore.StoredMessage inFlightAcknowledged(int messageID) {
        return outboundFlightZone.acknowledged(messageID);
    }

    /**
     * Mark the message identified by guid as publish in flight.
     *
     * @return the packetID for the message in flight.
     * */
    public int inFlightAckWaiting(IMessagesStore.StoredMessage msg) {
        LOG.debug("Adding message ot inflight zone. ClientId={}", clientID);
        int messageId = ClientSession.this.nextPacketId();
        outboundFlightZone.waitingAck(messageId, msg);
        return messageId;
    }

    public IMessagesStore.StoredMessage secondPhaseAcknowledged(int messageID) {
        return m_sessionsStore.secondPhaseAcknowledged(clientID, messageID);
    }

    /**
     * Enqueue a message to be sent to the client.
     *
     * @param message
     *            the message to enqueue.
     */
    public void enqueue(IMessagesStore.StoredMessage message) {
        this.m_sessionsStore.queue(this.clientID).add(message);
    }

    public IMessagesStore.StoredMessage inboundInflight(int messageID) {
        return inboundFlightZone.lookup(messageID);
    }

    public void markAsInboundInflight(int messageID, IMessagesStore.StoredMessage msg) {
        inboundFlightZone.waitingRel(messageID, msg);
    }

    public void moveInFlightToSecondPhaseAckWaiting(int messageID, IMessagesStore.StoredMessage msg) {
        m_sessionsStore.moveInFlightToSecondPhaseAckWaiting(this.clientID, messageID, msg);
    }

    public Set<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public int getPendingPublishMessagesNo() {
        return m_sessionsStore.getPendingPublishMessagesNo(clientID);
    }

    public int getSecondPhaseAckPendingMessages() {
        return m_sessionsStore.getSecondPhaseAckPendingMessages(clientID);
    }

    public int getInflightMessagesNo() {
        return m_sessionsStore.getInflightMessagesNo(clientID);
    }

}
