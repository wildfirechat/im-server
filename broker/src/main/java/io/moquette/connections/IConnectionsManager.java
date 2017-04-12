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

package io.moquette.connections;

import java.util.Collection;

/**
 * This interface will be used by an external codebase to retrieve and close physical connections.
 */
public interface IConnectionsManager {

    /**
     * Returns the number of physical connections
     *
     * @return
     */
    int getActiveConnectionsNo();

    /**
     * Determines wether a MQTT client is connected to the broker.
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

    /**
     * Closes a physical connection.
     *
     * @param clientID
     * @param closeImmediately
     *            If false, the connection will be flushed before it is closed.
     * @return
     */
    boolean closeConnection(String clientID, boolean closeImmediately);

    /**
     * Returns the state of the session of a given client.
     *
     * @param clientID
     * @return
     */
    MqttSession getSessionStatus(String clientID);

    /**
     * Returns the state of all the sessions
     *
     * @return
     */
    Collection<MqttSession> getSessions();
}
