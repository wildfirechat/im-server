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

import org.eclipse.moquette.spi.impl.events.PublishEvent;

import java.util.List;

/**
 * @author andrea
 */
public class ClientSession {

    public final String clientID;

    private final IMessagesStore messagesStore;

    public ClientSession(String clientID, IMessagesStore messagesStore) {
        this.clientID = clientID;
        this.messagesStore = messagesStore;
    }
    //List of client's subscriptions
    //list of messages not acknowledged by client
    //list of in-flight messages

    /**
     * @return the list of messages to be delivered for client related to the session.
     * */
    public List<PublishEvent> storedMessages() {
        return messagesStore.listMessagesInSession(clientID);
    }

    /**
     * Remove a message previously stored for delivery.
     * */
    public void removeDelivered(int messageID) {
        messagesStore.removeMessageInSession(clientID, messageID);
    }

    @Override
    public String toString() {
        return "ClientSession{" +
                "clientID='" + clientID + '\'' +
                '}';
    }
}