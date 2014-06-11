/*
 * Copyright (c) 2012-2014 The original author or authors
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
package org.dna.mqtt.moquette.messaging.spi;

import java.util.List;
import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.Subscription;

/**
 * Store used to handle the persistence of the subscriptions tree.
 *
 * @author andrea
 */
public interface IPersistentSubscriptionStore {

    void addNewSubscription(Subscription newSubscription, String clientID);

    void removeAllSubscriptions(String clientID);

    List<Subscription> retrieveAllSubscriptions();
}
