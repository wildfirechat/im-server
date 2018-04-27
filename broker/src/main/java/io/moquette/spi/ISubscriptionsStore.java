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

import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;

import java.util.Collection;
import java.util.List;

public interface ISubscriptionsStore {

    /**
     * Add a new subscription to the session
     *
     * @param newSubscription
     *            the subscription to add.
     */
    void addNewSubscription(Subscription newSubscription);

    /**
     * Removed a specific subscription
     *
     * @param topic
     *            the topic of the subscription.
     * @param clientID
     *            the session client.
     */
    void removeSubscription(Topic topic, String clientID);

    /**
     * Remove all the subscriptions of the session
     *
     * @param clientID
     *            the client ID
     */
    void wipeSubscriptions(String clientID);

    /**
     * Return all topic filters to recreate the subscription tree.
     */
    List<Subscription> listAllSubscriptions();

    /**
     * Load from storage all the subscriptions of the specified client.
     * */
    Collection<Subscription> listClientSubscriptions(String clientID);

    /**
     * @param subcription
     *            the subscription to reaload from storage.
     * @return the subscription stored by clientID and topicFilter, if any else null;
     */
    Subscription reload(Subscription subcription);
}
