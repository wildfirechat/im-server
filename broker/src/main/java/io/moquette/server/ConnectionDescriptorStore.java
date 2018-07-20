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

package io.moquette.server;

import io.moquette.connections.IConnectionsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ConnectionDescriptorStore implements IConnectionsManager {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectionDescriptorStore.class);

    private final ConcurrentMap<String, ConnectionDescriptor> connectionDescriptors;

    public ConnectionDescriptorStore() {
        this.connectionDescriptors = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<ConnectionDescriptor> lookupDescriptor(String clientID) {
        if (clientID == null) {
            return Optional.empty();
        }

        ConnectionDescriptor descriptor = connectionDescriptors.get(clientID);
        if (descriptor == null) {
            /*
             * If the client has just disconnected, its connection descriptor will be null. We
             * don't have to make the broker crash: we'll just discard the PUBACK message.
             */
            return Optional.empty();
        }
        return Optional.of(descriptor);
    }

    @Override
    public ConnectionDescriptor addConnection(ConnectionDescriptor descriptor) {
        return connectionDescriptors.putIfAbsent(descriptor.clientID, descriptor);
    }

    @Override
    public boolean removeConnection(ConnectionDescriptor descriptor) {
        return connectionDescriptors.remove(descriptor.clientID, descriptor);
    }

    @Override
    public boolean isConnected(String clientID) {
        return connectionDescriptors.containsKey(clientID);
    }

    @Override
    public int countActiveConnections() {
        return connectionDescriptors.size();
    }

    @Override
    public Collection<String> getConnectedClientIds() {
        return connectionDescriptors.keySet();
    }
}
