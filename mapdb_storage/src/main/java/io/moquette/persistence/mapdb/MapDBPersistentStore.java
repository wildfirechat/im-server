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

package io.moquette.persistence.mapdb;

import io.moquette.BrokerConstants;
import io.moquette.server.config.IConfig;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.IStore;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * MapDB main persistence implementation
 */
public class MapDBPersistentStore implements IStore {

    private static final Logger LOG = LoggerFactory.getLogger(MapDBPersistentStore.class);

    private DB m_db;
    private final String m_storePath;
    private final int m_autosaveInterval; // in seconds

    protected final ScheduledExecutorService m_scheduler;
    private IMessagesStore m_messageStore;
    private ISessionsStore m_sessionsStore;

    public MapDBPersistentStore(IConfig props, ScheduledExecutorService scheduler) {
        this.m_storePath = props.getProperty(BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME, "");
        this.m_autosaveInterval = Integer
                .parseInt(props.getProperty(BrokerConstants.AUTOSAVE_INTERVAL_PROPERTY_NAME, "30"));
        this.m_scheduler = scheduler;
    }

    @Override
    public IMessagesStore messagesStore() {
        return m_messageStore;
    }

    @Override
    public ISessionsStore sessionsStore() {
        return m_sessionsStore;
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    @Override
    public void initStore() {
        LOG.info("Initializing MapDB store");
        if (m_storePath == null || m_storePath.isEmpty()) {
            LOG.warn("MapDB store file path is empty, using in-memory store");
            m_db = DBMaker.newMemoryDB().make();
        } else {
            File tmpFile;
            try {
                LOG.info("Using user-defined MapDB store file. Path={}", m_storePath);
                tmpFile = new File(m_storePath);
                boolean fileNewlyCreated = tmpFile.createNewFile();
                LOG.warn("Using {} MapDB store file. Path={}", fileNewlyCreated ? "fresh" : "existing", m_storePath);
            } catch (IOException ex) {
                LOG.error("Unable to open MapDB store file. Path={}, cause={}, errorMessage={}", m_storePath,
                    ex.getCause(), ex.getMessage());
                throw new RuntimeException("Can't create temp subscriptions file storage [" + m_storePath + "]", ex);
            }
            m_db = DBMaker.newFileDB(tmpFile).make();
        }
        LOG.info("Scheduling MapDB commit task");
        m_scheduler.scheduleWithFixedDelay(() -> {
                LOG.debug("Committing to MapDB");
                m_db.commit();
        }, this.m_autosaveInterval, this.m_autosaveInterval, TimeUnit.SECONDS);

        // TODO check m_db is valid and
        m_messageStore = new MapDBMessagesStore(m_db);
        m_messageStore.initStore();

        m_sessionsStore = new MapDBSessionsStore(m_db);
        m_sessionsStore.initStore();
    }

    @Override
    public void close() {
        if (this.m_db.isClosed()) {
            LOG.warn("MapDB store is already closed. Nothing will be done");
            return;
        }
        LOG.info("Performing last commit to MapDB");
        this.m_db.commit();
        LOG.info("Closing MapDB store");
        this.m_db.close();
        LOG.info("Stopping MapDB commit tasks");

        //TODO the scheduler must be stopped by the owning (the instance of Server)
        //invalidate the added task
        this.m_scheduler.shutdown();
        try {
            m_scheduler.awaitTermination(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
        }
        if (!m_scheduler.isTerminated()) {
            LOG.warn("Forcing shutdown of MapDB commit tasks");
            m_scheduler.shutdown();
        }
        LOG.info("MapDB store has been closed successfully");
    }
}
