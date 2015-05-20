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
package org.eclipse.moquette.spi.impl.security;

import org.eclipse.moquette.spi.impl.subscriptions.SubscriptionsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.eclipse.moquette.spi.impl.security.Authorization.Permission.READ;
import static org.eclipse.moquette.spi.impl.security.Authorization.Permission.READWRITE;
import static org.eclipse.moquette.spi.impl.security.Authorization.Permission.WRITE;

/**
 * Used by the ACLFileParser to push all authorizations it finds.
 * ACLAuthorizator uses it in read mode to check it topics matches the ACLs.
 *
 * Not thread safe.
 *
 * @author andrea
 */
class AuthorizationsCollector implements IAuthorizator {

    private static final Logger LOG = LoggerFactory.getLogger(AuthorizationsCollector.class);

    private List<Authorization> m_authorizations = new ArrayList();

    static final AuthorizationsCollector emptyImmutableCollector() {
        AuthorizationsCollector coll = new AuthorizationsCollector();
        coll.m_authorizations = Collections.emptyList();
        return coll;
    }

    void parse(String line) throws ParseException {
        m_authorizations.add(parseAuthLine(line));
    }

    protected static Authorization parseAuthLine(String line) throws ParseException {
        String[] tokens = line.split("\\s+");
        String keyword = tokens[0];
        if ("topic".equalsIgnoreCase(keyword)) {
            if (tokens.length > 2) {
                //if the tokenized lines has 3 token the second must be the permission
                try {
                    Authorization.Permission permission = Authorization.Permission.valueOf(tokens[1].toUpperCase());
                    //bring topic with all original spacing
                    String topic = line.substring(line.indexOf(tokens[2]));

                    return new Authorization(topic, permission);
                } catch (IllegalArgumentException iaex) {
                    throw new ParseException("invalid permission token", 1);
                }
            }
            String topic = tokens[1];
            return new Authorization(topic);
        }
        return null;
    }

    @Override
    public boolean canWrite(String topic) {
        return canDoOperation(topic, WRITE);
    }

    @Override
    public boolean canRead(String topic) {
        return canDoOperation(topic, READ);
    }

    private boolean canDoOperation(String topic, Authorization.Permission permission) {
        for (Authorization auth : m_authorizations) {
            if (auth.permission == permission || auth.permission == READWRITE) {
                if (SubscriptionsStore.matchTopics(topic, auth.topic)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return m_authorizations.isEmpty();
    }
}
