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

import java.util.List;
import java.util.Set;

import org.eclipse.moquette.spi.impl.subscriptions.Subscription;

/**
 * Store used to handle the persistence of the subscriptions tree.
 *
 * @author andrea
 */
public interface ISessionsStore {

    /**
     * Add a new subscription to the session
     */
    void addNewSubscription(Subscription newSubscription);

    /**
     * Removed a specific subscription
     * */
    void removeSubscription(String topic, String clientID);

    /**
     * Remove all the subscriptions of the session
     */
    void wipeSubscriptions(String sessionID);

    /**
     * Updates the subscriptions set for the clientID
     * */
    void updateSubscriptions(String clientID, Set<Subscription> subscriptions);

    List<Subscription> listAllSubscriptions();

    /**
     * @return true iff there are subscriptions persisted with clientID
     */
    boolean contains(String clientID);

    void createNewSession(String clientID);
}
