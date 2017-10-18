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

package io.moquette.persistence;

import io.moquette.server.config.IConfig;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.IStore;

import java.util.concurrent.ScheduledExecutorService;

public class MemoryStorageService implements IStore {

    private MemorySessionStore m_sessionsStore;
    private MemoryMessagesStore m_messagesStore;

    // NB these params must be here "by contract" used in introspection instantiation used in
    // ProtocolProcessorBootstrapper.instantiateConfiguredStore
    public MemoryStorageService(IConfig props, ScheduledExecutorService scheduler) {
        m_messagesStore = new MemoryMessagesStore();
        m_sessionsStore = new MemorySessionStore();
        m_messagesStore.initStore();
        m_sessionsStore.initStore();
    }

    @Override
    public IMessagesStore messagesStore() {
        return m_messagesStore;
    }

    @Override
    public ISessionsStore sessionsStore() {
        return m_sessionsStore;
    }

    @Override
    public void initStore() {
    }

    @Override
    public void close() {
    }
}
