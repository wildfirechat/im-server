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

import static org.eclipse.moquette.spi.impl.security.Authorization.Permission.*;
import static org.eclipse.moquette.spi.impl.security.Authorization.Permission;
import org.eclipse.moquette.spi.impl.subscriptions.SubscriptionsStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;

/**
 * @author andrea
 */
public class ACLAuthorizator implements IAuthorizator {

    private static final Logger LOG = LoggerFactory.getLogger(ACLAuthorizator.class);

    List<Authorization> m_authorisations = Collections.emptyList();

    public ACLAuthorizator(String aclFilePath) {
        File aclFile = new File(aclFilePath);
        try {
            m_authorisations = ACLFileParser.parse(aclFile);
        } catch (ParseException pex) {
            LOG.warn(String.format("Format error in parsing acl file %s", aclFile), pex);
        }
    }

    public ACLAuthorizator(List<Authorization> authorisations) {
        m_authorisations = authorisations;
    }

    @Override
    public boolean canWrite(String topic) {
        return canDoOperation(topic, WRITE);
    }

    public boolean canRead(String topic) {
        return canDoOperation(topic, READ);
    }

    private boolean canDoOperation(String topic, Permission permission) {
        for (Authorization auth : m_authorisations) {
            if (auth.permission == permission) {
                if (SubscriptionsStore.matchTopics(topic, auth.topic)) {
                    return true;
                }
            }
        }
        return false;
    }
}
