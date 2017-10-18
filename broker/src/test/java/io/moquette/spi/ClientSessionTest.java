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

import io.moquette.persistence.MemoryStorageService;
import io.moquette.spi.impl.SessionsRepository;
import io.moquette.spi.impl.subscriptions.CTrieSubscriptionDirectory;
import io.moquette.spi.impl.subscriptions.ISubscriptionsDirectory;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;
import static io.netty.handler.codec.mqtt.MqttQoS.EXACTLY_ONCE;
import static org.junit.Assert.assertEquals;

public class ClientSessionTest {

    private ISubscriptionsDirectory subscriptionsDirectory;
    private SessionsRepository sessionsRepository;

    @Before
    public void setUp() {
        subscriptionsDirectory = new CTrieSubscriptionDirectory();
        MemoryStorageService storageService = new MemoryStorageService(null, null);
        ISessionsStore sessionsStore = storageService.sessionsStore();
        sessionsRepository = new SessionsRepository(sessionsStore, null);
        subscriptionsDirectory.init(sessionsRepository);
    }

    @Test
    public void overridingSubscriptionsForTransientSession() {
        ClientSession sessionClean = sessionsRepository.createNewSession("SESSION_ID_1", true);
        checkOverridingSubcription(sessionClean);
    }

    @Test
    public void overridingSubscriptionsForPersistentSession() {
        ClientSession sessionClean = sessionsRepository.createNewSession("SESSION_ID_1", false);
        checkOverridingSubcription(sessionClean);
    }

    private void checkOverridingSubcription(ClientSession sessionClean) {
        // Subscribe on /topic with QOSType.MOST_ONE
        Subscription oldSubscription = new Subscription(sessionClean.clientID, new Topic("/topic"), AT_MOST_ONCE);
        sessionClean.subscribe(oldSubscription);
        subscriptionsDirectory.add(oldSubscription);

        // Subscribe on /topic again that overrides the previous subscription.
        Subscription overrindingSubscription = new Subscription(sessionClean.clientID, new Topic("/topic"), EXACTLY_ONCE);
        sessionClean.subscribe(overrindingSubscription);
        subscriptionsDirectory.add(overrindingSubscription);

        // Verify
        List<Subscription> subscriptions = subscriptionsDirectory.matches(new Topic("/topic"));
        assertEquals("Only one subscription must match /topic", 1, subscriptions.size());
        Subscription sub = subscriptions.get(0);
        assertEquals("Matching subscription MUST have higher QoS",
            overrindingSubscription.getRequestedQos(), sub.getRequestedQos());
    }
}
