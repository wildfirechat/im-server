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
 * A class that represents the overall connection status of a MQTT session. Its instances will be
 * used by an external codebase when the broker is configured in embedded mode.
 */
public class MqttSession {

    private boolean connectionEstablished;
    private boolean cleanSession;
    private int inflightMessages;
    private int pendingPublishMessagesNo;
    private int secondPhaseAckPendingMessages;
    private MqttConnectionMetrics connectionMetrics;

    public boolean isConnectionEstablished() {
        return connectionEstablished;
    }

    public void setConnectionEstablished(boolean connectionEstablished) {
        this.connectionEstablished = connectionEstablished;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public int getPendingPublishMessagesNo() {
        return pendingPublishMessagesNo;
    }

    public void setPendingPublishMessagesNo(int pendingPublishMessagesNo) {
        this.pendingPublishMessagesNo = pendingPublishMessagesNo;
    }

    public int getInflightMessages() {
        return inflightMessages;
    }

    public void setInflightMessages(int inflightMessages) {
        this.inflightMessages = inflightMessages;
    }

    public int getSecondPhaseAckPendingMessages() {
        return secondPhaseAckPendingMessages;
    }

    public void setSecondPhaseAckPendingMessages(int secondPhaseAckPendingMessages) {
        this.secondPhaseAckPendingMessages = secondPhaseAckPendingMessages;
    }

    public MqttConnectionMetrics getConnectionMetrics() {
        return connectionMetrics;
    }

    public void setConnectionMetrics(MqttConnectionMetrics connectionMetrics) {
        this.connectionMetrics = connectionMetrics;
    }

}
