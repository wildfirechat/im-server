/*
 * Copyright (c) 2012-2017 The original author or authorsgetRockQuestions()
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

import io.moquette.parser.proto.messages.AbstractMessage;
import io.moquette.spi.ISessionsStore.ClientTopicCouple;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.SubscriptionsStore;
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
 *
 * @author andrea
 */
public class ClientSession {

    private final static Logger LOG = LoggerFactory.getLogger(ClientSession.class);

    public final String clientID;

    private final IMessagesStore messagesStore;

    private final ISessionsStore m_sessionsStore;

    private Set<Subscription> subscriptions = new HashSet<>();

    private volatile boolean cleanSession;

//    private BlockingQueue<AbstractMessage> m_queueToPublish = new ArrayBlockingQueue<>(Constants.MAX_MESSAGE_QUEUE);

    public ClientSession(String clientID, IMessagesStore messagesStore, ISessionsStore sessionsStore,
                         boolean cleanSession) {
        this.clientID = clientID;
        this.messagesStore = messagesStore;
        this.m_sessionsStore = sessionsStore;
        this.cleanSession = cleanSession;
    }

    /**
     * Return the list of persisted publishes for the given clientID.
     * For QoS1 and QoS2 with clean session flag, this method return the list of
     * missed publish events while the client was disconnected.
     *
     * @return the list of messages to be delivered for client related to the session.
     * */
    public BlockingQueue<IMessagesStore.StoredMessage> queue() {
        return this.m_sessionsStore.queue(clientID);
    }

    @Override
    public String toString() {
        return "ClientSession{clientID='" + clientID + '\'' +"}";
    }

    public boolean subscribe(Subscription newSubscription) {
        LOG.info("<{}> subscribed to the topic filter <{}> with QoS {}",
                newSubscription.getClientId(), newSubscription.getTopicFilter(),
                AbstractMessage.QOSType.formatQoS(newSubscription.getRequestedQos()));
        boolean validTopic = SubscriptionsStore.validate(newSubscription.getTopicFilter());
        if (!validTopic) {
            //send SUBACK with 0x80 for this topic filter
            return false;
        }
        ClientTopicCouple matchingCouple = new ClientTopicCouple(this.clientID, newSubscription.getTopicFilter());
        Subscription existingSub = m_sessionsStore.getSubscription(matchingCouple);
        //update the selected subscriptions if not present or if has a greater qos
        if (existingSub == null || existingSub.getRequestedQos().byteValue() < newSubscription.getRequestedQos().byteValue()) {
            if (existingSub != null) {
                subscriptions.remove(newSubscription);
            }
            subscriptions.add(newSubscription);
            m_sessionsStore.addNewSubscription(newSubscription);
        }
        return true;
    }

    public void unsubscribeFrom(String topicFilter) {
        m_sessionsStore.removeSubscription(topicFilter, clientID);
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
            //cleanup topic subscriptions
            cleanSession();
        }

    }

    public void cleanSession() {
        LOG.info("cleaning old saved subscriptions for client <{}>", this.clientID);
        m_sessionsStore.wipeSubscriptions(this.clientID);
        LOG.debug("Wiped subscriptions for client <{}>", this.clientID);

        //remove also the messages stored of type QoS1/2
        messagesStore.dropMessagesInSession(this.clientID);
        LOG.debug("Removed messages in session for client <{}>", this.clientID);
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

    public void inFlightAcknowledged(int messageID) {
        LOG.trace("Acknowledging inflight, clientID <{}> messageID {}", this.clientID, messageID);
        m_sessionsStore.inFlightAck(this.clientID, messageID);
    }

    public void inFlightAckWaiting(MessageGUID guid, int messageID) {
        LOG.trace("Adding to inflight {}, guid <{}>", messageID, guid);
        m_sessionsStore.inFlight(this.clientID, messageID, guid);
    }

    public IMessagesStore.StoredMessage secondPhaseAcknowledged(int messageID) {
        MessageGUID guid = m_sessionsStore.secondPhaseAcknowledged(clientID, messageID);
        return messagesStore.getMessageByGuid(guid);
    }

    /**
     * Enqueue a message to be sent to the client.
     * @param message the message to enqueue.
     * */
    public void enqueue(IMessagesStore.StoredMessage message) {
        this.m_sessionsStore.queue(this.clientID).add(message);
    }

    public IMessagesStore.StoredMessage storedMessage(int messageID) {
        final MessageGUID guid = m_sessionsStore.mapToGuid(clientID, messageID);
        return messagesStore.getMessageByGuid(guid);
    }

    public void moveInFlightToSecondPhaseAckWaiting(int messageID) {
        m_sessionsStore.moveInFlightToSecondPhaseAckWaiting(this.clientID, messageID);
    }

    public IMessagesStore.StoredMessage getInflightMessage(int messageID) {
        return m_sessionsStore.getInflightMessage(clientID, messageID);
    }
}