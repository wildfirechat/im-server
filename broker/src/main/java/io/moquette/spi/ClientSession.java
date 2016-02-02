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

import io.moquette.proto.messages.AbstractMessage;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.SubscriptionsStore;
import io.moquette.spi.ISessionsStore.ClientTopicCouple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private boolean active = false;

    public ClientSession(String clientID, IMessagesStore messagesStore, ISessionsStore sessionsStore,
                         boolean cleanSession) {
        this.clientID = clientID;
        this.messagesStore = messagesStore;
        this.m_sessionsStore = sessionsStore;
        this.cleanSession = cleanSession;
    }
    //List of client's subscriptions
    //list of messages not acknowledged by client
    //list of in-flight messages

    /**
     * @return the list of messages to be delivered for client related to the session.
     * */
    public List<IMessagesStore.StoredMessage> storedMessages() {
        //read all messages from enqueued store
        Collection<String> guids = this.m_sessionsStore.enqueued(clientID);
        return messagesStore.listMessagesInSession(guids);
    }

    /**
     * Remove the messages stored in a cleanSession false.
     */
    public void removeEnqueued(String guid) {
        this.m_sessionsStore.removeEnqueued(this.clientID, guid);
    }

    @Override
    public String toString() {
        return "ClientSession{clientID='" + clientID + '\'' +"}";
    }

    public boolean subscribe(String topicFilter, Subscription newSubscription) {
        LOG.info("<{}> subscribed to topicFilter <{}> with QoS {}",
                newSubscription.getClientId(), topicFilter,
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

        //deactivate the session
        deactivate();
    }

    public void cleanSession() {
        LOG.info("cleaning old saved subscriptions for client <{}>", this.clientID);
        m_sessionsStore.wipeSubscriptions(this.clientID);

        //remove also the messages stored of type QoS1/2
        messagesStore.dropMessagesInSession(this.clientID);
    }

    public boolean isCleanSession() {
        return this.cleanSession;
    }

    public void cleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
        this.m_sessionsStore.updateCleanStatus(this.clientID, cleanSession);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isActive() {
        return this.active;
    }

    public int nextPacketId() {
        return this.m_sessionsStore.nextPacketID(this.clientID);
    }

    public void inFlightAcknowledged(int messageID) {
        m_sessionsStore.inFlightAck(this.clientID, messageID);
    }

    public void inFlightAckWaiting(String guid, int messageID) {
        m_sessionsStore.inFlight(this.clientID, messageID, guid);
    }

    public void secondPhaseAcknowledged(int messageID) {
        m_sessionsStore.secondPhaseAcknowledged(clientID, messageID);
    }

    public void secondPhaseAckWaiting(int messageID) {
        m_sessionsStore.secondPhaseAckWaiting(clientID, messageID);
    }

    public void enqueueToDeliver(String guid) {
        this.m_sessionsStore.bindToDeliver(guid, this.clientID);
    }

    public IMessagesStore.StoredMessage storedMessage(int messageID) {
        final String guid = m_sessionsStore.mapToGuid(clientID, messageID);
        return messagesStore.getMessageByGuid(guid);
    }
}