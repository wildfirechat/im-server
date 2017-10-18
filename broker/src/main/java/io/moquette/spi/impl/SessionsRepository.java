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
package io.moquette.spi.impl;

import io.moquette.persistence.PersistentSession;
import io.moquette.spi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SessionsRepository {

    /**
     * Task to be scheduled to execute the cleaning of persisted sessions (clean flag=false) older than a defined period.
     */
    private class SessionCleanerTask implements Runnable {

        @Override
        public void run() {
            wipeExpiredSessions();
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(SessionsRepository.class);
    private final ISessionsStore sessions;
    private ISubscriptionsStore subscriptionsStore;
    private ScheduledExecutorService scheduler;
    private final Map<String, ClientSession> sessionsCache = new ConcurrentHashMap<>();

    public SessionsRepository(ISessionsStore sessionsStore, ScheduledExecutorService scheduler) {
        this.sessions = sessionsStore;
        this.subscriptionsStore = sessionsStore.subscriptionStore();
        this.scheduler = scheduler;
    }

    public void init() {
        SessionCleanerTask cleanerTask = new SessionCleanerTask();
        this.scheduler.schedule(cleanerTask, 1, TimeUnit.HOURS);
    }

    public ClientSession sessionForClient(String clientID) {
        LOG.debug("Retrieving session. CId={}", clientID);
        if (this.sessionsCache.containsKey(clientID)) {
            return this.sessionsCache.get(clientID);
        }

        if (!this.sessions.contains(clientID)) {
            LOG.warn("Session does not exist. CId={}", clientID);
            return null;
        }
        PersistentSession storedSession = this.sessions.loadSessionByKey(clientID);
        return newClientSessionAndCacheIt(storedSession.clientID, storedSession.cleanSession);
    }

    private ClientSession newClientSessionAndCacheIt(String clientID, boolean cleanSession) {
        ClientSession session;
        if (cleanSession) {
            session = new TransientSession(clientID);
        } else {
            DurableSession durableSession = new DurableSession(clientID, this.sessions, this.subscriptionsStore);
            durableSession.reloadAllSubscriptionsFromStore();
            session = durableSession;
        }
        this.sessionsCache.put(clientID, session);
        return session;
    }

    public ClientSession createNewSession(String clientID, boolean cleanSession) {
        if (sessions.contains(clientID)) {
            LOG.error("Unable to create a new session: the client ID is already in use. CId={}, cleanSession={}",
                clientID, cleanSession);
            throw new IllegalArgumentException("Can't create a session with the ID of an already existing" + clientID);
        }
        LOG.debug("Creating new session. CId={}, cleanSession={}", clientID, cleanSession);
        if (!cleanSession) {
            sessions.createNewDurableSession(clientID);
        }
        return newClientSessionAndCacheIt(clientID, cleanSession);
    }

    public Collection<ClientSession> getAllSessions() {
        Collection<ClientSession> result = new ArrayList<>();
        for (PersistentSession persistentSession : this.sessions.listAllSessions()) {
            result.add(sessionForClient(persistentSession.clientID));
        }
        return result;
    }

    private void updateCleanStatus(String clientID, boolean cleanSession) {
        LOG.info("Updating cleanSession flag. CId={}, cleanSession={}", clientID, cleanSession);
        this.sessions.updateCleanStatus(clientID, cleanSession);
    }

    ClientSession createOrLoadClientSession(String clientId, boolean cleanSession) {
        ClientSession clientSession = this.sessionForClient(clientId);
        if (clientSession == null) {
            clientSession = this.createNewSession(clientId, cleanSession);
        } else {
            //session was already present
            if (!clientSession.isCleanSession() && cleanSession) {
                //remove from storage & create new transient session
                //TODO existing subscription from durable session has to be copied into the new transient?
                sessions.removeDurableSession(clientId);
                clientSession = this.createNewSession(clientId, true);
            } /*else {
                clientSession.cleanSession(cleanSession);
                this.updateCleanStatus(clientId, cleanSession);
            }*/
        }
        if (cleanSession) {
            LOG.info("Cleaning session. CId={}", clientId);
            clientSession.cleanSession();
        }
        return clientSession;
    }

    public void disconnect(String clientId) {
        LOG.debug("Removing session from repository's cache");
        ClientSession clientSession = this.sessionForClient(clientId);
        if (clientSession == null) {
            return;
        }

        sessionsCache.remove(clientId);
        this.sessions.trackSessionClose(LocalDateTime.now(), clientId);
    }

    private void wipeExpiredSessions() {
        final LocalDateTime pin = LocalDateTime.now().minus(6, ChronoUnit.DAYS);
        final Set<String> expiredSessionsIds = this.sessions.sessionOlderThan(pin);
        for (String expiredSession : expiredSessionsIds) {
            this.sessions.removeDurableSession(expiredSession);
            this.subscriptionsStore.wipeSubscriptions(expiredSession);
        }
    }
}
