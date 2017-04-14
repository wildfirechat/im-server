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

package io.moquette.interception.messages;

public class InterceptUnsubscribeMessage implements InterceptMessage {

    private final String topicFilter;
    private final String clientID;
    private final String username;

    public InterceptUnsubscribeMessage(String topicFilter, String clientID, String username) {
        this.topicFilter = topicFilter;
        this.clientID = clientID;
        this.username = username;
    }

    public String getTopicFilter() {
        return topicFilter;
    }

    public String getClientID() {
        return clientID;
    }

    public String getUsername() {
        return username;
    }
}
