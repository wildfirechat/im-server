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
package io.moquette.persistence.h2;

import io.moquette.BrokerConstants;
import io.moquette.server.config.IConfig;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.IStore;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class H2PersistentStore implements IStore {

    private static final Logger LOG = LoggerFactory.getLogger(H2PersistentStore.class);

    private final String storePath;
    private final int autosaveInterval; // in seconds
    protected final ScheduledExecutorService scheduler;
    private MVStore mvStore;

    private IMessagesStore messageStore;
    private ISessionsStore sessionsStore;

    public H2PersistentStore(IConfig props, ScheduledExecutorService scheduler) {
        this.storePath = props.getProperty(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME, "");
        final String autosaveProp = props.getProperty(BrokerConstants.AUTOSAVE_INTERVAL_PROPERTY_NAME, "30");
        this.autosaveInterval = Integer.parseInt(autosaveProp);
        this.scheduler = scheduler;
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    @Override
    public void initStore() {
        LOG.info("Initializing H2 store");
        if (storePath == null || storePath.isEmpty()) {
            LOG.warn("H2 store file path is empty, using in-memory store");
            mvStore = MVStore.open(null);
        } else {
            mvStore = new MVStore.Builder()
                .fileName(storePath)
                .autoCommitDisabled()
                .open();
        }

        LOG.info("Scheduling H2 commit task");
        scheduler.scheduleWithFixedDelay(() -> {
            LOG.debug("Committing to H2");
            mvStore.commit();
        }, this.autosaveInterval, this.autosaveInterval, TimeUnit.SECONDS);

        messageStore = new H2MessagesStore(mvStore);
        messageStore.initStore();

        sessionsStore = new H2SessionsStore(mvStore);
        sessionsStore.initStore();
    }

    @Override
    public void close() {
    }

    @Override
    public IMessagesStore messagesStore() {
        return messageStore;
    }

    @Override
    public ISessionsStore sessionsStore() {
        return sessionsStore;
    }

}
