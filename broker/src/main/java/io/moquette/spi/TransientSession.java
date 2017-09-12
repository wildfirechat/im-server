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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TransientSession extends ClientSession {

    private static final Logger LOG = LoggerFactory.getLogger(TransientSession.class);

    private Queue<StoredMessage> messagesQueue = new LinkedList<>();

    private final AtomicInteger packetGenerator = new AtomicInteger(1);

    private final ConcurrentMap<Integer, StoredMessage> inboundInflightMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, StoredMessage> outboundInflightMap = new ConcurrentHashMap<>();
    private final Map<Integer, StoredMessage> secondPhaseStore = new ConcurrentHashMap<>();

    public TransientSession(String clientID) {
        super(clientID);
    }

    @Override
    public void disconnect() {}

    @Override
    public boolean isCleanSession() {
        return true;
    }

    @Override
    public boolean subscribe(Subscription newSubscription) {
        LOG.info("Adding new subscription. CId={}, topics={}, qos={}", newSubscription.getClientId(),
            newSubscription.getTopicFilter(), newSubscription.getRequestedQos());
        boolean validTopic = newSubscription.getTopicFilter().isValid();
        if (!validTopic) {
            LOG.warn("The topic filter is not valid. CId={}, topics={}", newSubscription.getClientId(),
                newSubscription.getTopicFilter());
            // send SUBACK with 0x80 for this topic filter
            return false;
        }
        final Optional<Subscription> mathingExisting = this.subscriptions.stream()
            .filter(s -> s.equals(newSubscription))
            .findFirst();
        if (mathingExisting.isPresent()) {
            Subscription existingSub = mathingExisting.get();
            if (existingSub.qosLessThan(newSubscription)) {
                LOG.info("Subscription already existed with a lower QoS value. It will be updated. CId={}, " +
                        "topics={}, existingQos={}, newQos={}", newSubscription.getClientId(),
                    newSubscription.getTopicFilter(), existingSub.getRequestedQos(), newSubscription.getRequestedQos());
                subscriptions.remove(newSubscription);
            }
        }

        subscriptions.add(newSubscription);
        return true;
    }

    @Override
    public void unsubscribeFrom(Topic topicFilter) {
        LOG.info("Removing subscription. CId={}, topics={}", clientID, topicFilter);
        Set<Subscription> subscriptionsToRemove = new HashSet<>();
        for (Subscription sub : this.subscriptions) {
            if (sub.getTopicFilter().equals(topicFilter)) {
                subscriptionsToRemove.add(sub);
            }
        }
        subscriptions.removeAll(subscriptionsToRemove);
    }

    @Override
    public boolean isEmptyQueue() {
        return this.messagesQueue.isEmpty();
    }

    @Override
    public void enqueue(StoredMessage message) {
        this.messagesQueue.offer(message);
    }

    @Override
    public EnqueuedMessage poll() {
        IMessagesStore.StoredMessage msg = this.messagesQueue.poll();
        if (msg == null) {
            return null;
        }
        int messageId = this.inFlightAckWaiting(msg);
        return new EnqueuedMessage(msg, messageId);
    }

    @Override
    public void dropQueue() {
        LOG.debug("Removing messages of session. CId={}", this.clientID);
        this.messagesQueue = null;
        LOG.debug("Messages of the session have been removed. CId={}", this.clientID);
    }

    @Override
    public void cleanSession() {
        LOG.info("Transient sessiom, wiping existing subscriptions. ClientId={}", clientID);
        this.subscriptions.clear();

        //TODO
//        LOG.info("Removing queues. ClientId={}", clientID);
//        m_sessionsStore.dropQueue(clientID);
    }

    @Override
    protected int nextPacketId() {
        return this.packetGenerator.getAndIncrement();
    }

    @Override
    public int inFlightAckWaiting(StoredMessage msg) {
        LOG.debug("Adding message to inflight zone. CId={}", clientID);
        int messageId = this.nextPacketId();
        this.outboundInflightMap.put(messageId, msg);
        return messageId;
    }

    @Override
    public IMessagesStore.StoredMessage inFlightAcknowledged(int messageID) {
        LOG.debug("Removing message to inflight zone. CId={}, messageID={}", clientID, messageID);
        return this.outboundInflightMap.remove(messageID);
    }

    @Override
    public void markAsInboundInflight(int messageID, IMessagesStore.StoredMessage msg) {
        this.inboundInflightMap.put(messageID, msg);
    }

    @Override
    public IMessagesStore.StoredMessage inboundInflight(int messageID) {
        return this.inboundInflightMap.remove(messageID);
    }

    @Override
    public void moveInFlightToSecondPhaseAckWaiting(int messageID, IMessagesStore.StoredMessage msg) {
        this.secondPhaseStore.put(messageID, msg);
        this.outboundInflightMap.put(messageID, msg);
    }

    @Override
    public IMessagesStore.StoredMessage completeReleasedPublish(int messageID) {
        LOG.info("Acknowledged message in second phase, clientID <{}> messageID {}", clientID, messageID);
        return this.secondPhaseStore.remove(messageID);
    }

    @Override
    public int getPendingPublishMessagesNo() {
        return this.messagesQueue.size();
    }

    @Override
    public int countPubReleaseWaitingPubComplete() {
        return this.outboundInflightMap.size();
    }

    @Override
    public int getInflightMessagesNo() {
        return this.inboundInflightMap.size();
    }
}
