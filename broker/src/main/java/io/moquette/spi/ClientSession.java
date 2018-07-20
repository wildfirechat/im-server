/*
 * Copyright (c) 2012-2018 The original author or authors
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

import io.moquette.spi.impl.WillMessage;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;
import io.netty.handler.codec.mqtt.MqttConnectMessage;

import java.util.HashSet;
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
 */
public abstract class ClientSession {

    public final String clientID;

    protected Set<Subscription> subscriptions = new HashSet<>();

    public ClientSession(String clientID) {
        this.clientID = clientID;
    }

    public Subscription findSubscriptionByTopicFilter(Subscription matchingSub) {
        for (Subscription sub : this.subscriptions) {
            if (matchingSub.equals(sub)) {
                return sub;
            }
        }
        return null;
    }

    public Set<Subscription> getSubscriptions() {
        return subscriptions;
    }

    public abstract boolean isCleanSession();

    public abstract boolean subscribe(Subscription newSubscription);

    public abstract void unsubscribeFrom(Topic topicFilter);

    public abstract void disconnect();

    public abstract void cleanSession();

    protected abstract int nextPacketId();

    public abstract IMessagesStore.StoredMessage inFlightAcknowledged(int messageID);

    /**
     * Mark the message as publish in flight.
     *
     * @return the packetID for the message in flight.
     * */
    public abstract int inFlightAckWaiting(IMessagesStore.StoredMessage msg);

    public abstract IMessagesStore.StoredMessage completeReleasedPublish(int messageID);

    /**
     * Enqueue a message to be sent to the client.
     *
     * @param message
     *            the message to enqueue.
     */
    public abstract void enqueue(IMessagesStore.StoredMessage message);

    /**
     * @return the next message queued to this client or null if empty.
     * */
    public abstract EnqueuedMessage poll();

    public abstract boolean isEmptyQueue();

    public abstract void dropQueue();

    public abstract IMessagesStore.StoredMessage inboundInflight(int messageID);

    public abstract void markAsInboundInflight(int messageID, IMessagesStore.StoredMessage msg);

    public abstract void moveInFlightToSecondPhaseAckWaiting(int messageID, IMessagesStore.StoredMessage msg);

    public abstract int getPendingPublishMessagesNo();

    public abstract int countPubReleaseWaitingPubComplete();

    public abstract int getInflightMessagesNo();

    public abstract void wipeSubscriptions();

    public abstract void storeWillMessage(MqttConnectMessage msg, String clientId);

    public abstract void removeWill();

    public abstract WillMessage willMessage();

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{clientID='" + clientID + "'}";
    }
}
