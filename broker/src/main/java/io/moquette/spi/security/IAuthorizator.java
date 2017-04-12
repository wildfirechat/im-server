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

package io.moquette.spi.security;

import io.moquette.spi.impl.subscriptions.Topic;

/**
 * ACL checker.
 *
 * Create an authorizator that matches topic names with same grammar of subscriptions. The # is
 * always a terminator and its the multilevel matcher. The + sign is the single level matcher.
 */
public interface IAuthorizator {

    /**
     * Ask the implementation of the authorizator if the topic can be used in a publish.
     *
     * @param topic
     *            the topic to write to.
     * @param user
     *            the user
     * @param client
     *            the client
     * @return true if the user from client can publish data on topic.
     */
    boolean canWrite(Topic topic, String user, String client);

    boolean canRead(Topic topic, String user, String client);
}
