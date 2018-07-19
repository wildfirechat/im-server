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

package io.moquette.connections;

import io.moquette.server.ConnectionDescriptor;

import java.util.Collection;
import java.util.Optional;

/**
 * Repository for active connections
 */
public interface IConnectionsManager {

    /**
     * Returns the number of physical connections
     *
     * @return
     */
    int countActiveConnections();

    Optional<ConnectionDescriptor> lookupDescriptor(String clientID);

    ConnectionDescriptor addConnection(ConnectionDescriptor descriptor);

    boolean removeConnection(ConnectionDescriptor descriptor);

    /**
     * Determines weather a MQTT client is connected to the broker.
     *
     * @param clientID
     * @return
     */
    boolean isConnected(String clientID);

    /**
     * Returns the identifiers of the MQTT clients that are connected to the broker.
     *
     * @return
     */
    Collection<String> getConnectedClientIds();
}
