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
package io.moquette.persistence;

import io.moquette.broker.ISubscriptionsRepository;
import io.moquette.broker.subscriptions.Subscription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MemorySubscriptionsRepository implements ISubscriptionsRepository {

    private final List<Subscription> subscriptions = new ArrayList<>();

    @Override
    public List<Subscription> listAllSubscriptions() {
        return Collections.unmodifiableList(subscriptions);
    }

    @Override
    public void addNewSubscription(Subscription subscription) {
        subscriptions.add(subscription);
    }

    @Override
    public void removeSubscription(String topic, String clientID) {
        subscriptions.stream()
            .filter(s -> s.getTopicFilter().toString().equals(topic) && s.getClientId().equals(clientID))
            .findFirst()
            .ifPresent(subscriptions::remove);
    }
}
