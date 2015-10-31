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
package org.eclipse.moquette.spi;

import org.eclipse.moquette.proto.messages.AbstractMessage;
import org.eclipse.moquette.spi.impl.events.PublishEvent;
import org.eclipse.moquette.spi.impl.subscriptions.Subscription;
import org.eclipse.moquette.spi.impl.subscriptions.SubscriptionsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author andrea
 */
public class ClientSession {

    private final static Logger LOG = LoggerFactory.getLogger(ClientSession.class);

    public final String clientID;

    private final IMessagesStore messagesStore;

    private final ISessionsStore m_sessionsStore;

    private Set<Subscription> subscriptions = new HashSet<>();

    public ClientSession(String clientID, IMessagesStore messagesStore, ISessionsStore sessionsStore) {
        this.clientID = clientID;
        this.messagesStore = messagesStore;
        this.m_sessionsStore = sessionsStore;
    }
    //List of client's subscriptions
    //list of messages not acknowledged by client
    //list of in-flight messages

    /**
     * @return the list of messages to be delivered for client related to the session.
     * */
    public List<PublishEvent> storedMessages() {
        return messagesStore.listMessagesInSession(clientID);
    }

    /**
     * Remove a message previously stored for delivery.
     * */
    public void removeDelivered(int messageID) {
        messagesStore.removeMessageInSession(clientID, messageID);
    }

    @Override
    public String toString() {
        return "ClientSession{" +
                "clientID='" + clientID + '\'' +
                '}';
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
        subscriptions.add(newSubscription);
        m_sessionsStore.addNewSubscription(newSubscription);
        return true;
    }
}