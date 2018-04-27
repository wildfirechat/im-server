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

package io.moquette.spi.impl;

class RunningSubscription {

    final String clientID;
    final long packetId;

    RunningSubscription(String clientID, long packetId) {
        this.clientID = clientID;
        this.packetId = packetId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        RunningSubscription that = (RunningSubscription) o;

        return packetId == that.packetId
                && (clientID != null ? clientID.equals(that.clientID) : that.clientID == null);
    }

    @Override
    public int hashCode() {
        int result = clientID != null ? clientID.hashCode() : 0;
        result = 31 * result + (int) (packetId ^ (packetId >>> 32));
        return result;
    }
}
