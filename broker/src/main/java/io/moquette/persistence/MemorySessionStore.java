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

import cn.wildfirechat.proto.WFCMessage;
import io.moquette.server.Constants;
import io.moquette.server.Server;
import io.moquette.spi.ClientSession;
import io.moquette.spi.IMessagesStore.StoredMessage;
import io.moquette.spi.ISessionsStore;

import io.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.liyufan.im.ErrorCode;
import win.liyufan.im.UserSettingScope;
import win.liyufan.im.Utility;

import java.util.*;
import java.util.concurrent.*;

public class MemorySessionStore implements ISessionsStore {
    private static int dumy = 1;
    private static final Logger LOG = LoggerFactory.getLogger(MemorySessionStore.class);

    public static class Session implements Comparable<Session>{
        final String clientID;
        String username;
        private String appName;
        private String deviceToken;
        private String voipDeviceToken;
        private String secret;
        private String dbSecret;

        private long lastActiveTime;

        private long lastChatroomActiveTime;

        private volatile int unReceivedMsgs;

        public long getLastActiveTime() {
            return lastActiveTime;
        }

        public long getLastChatroomActiveTime() {
            return lastChatroomActiveTime;
        }

        public void refreshLastChatroomActiveTime() {
            this.lastChatroomActiveTime = System.currentTimeMillis();
        }

        public void refreshLastActiveTime() {
            this.lastActiveTime = System.currentTimeMillis();
        }

        public int getUnReceivedMsgs() {
            return unReceivedMsgs;
        }

        public void setUnReceivedMsgs(int unReceivedMsgs) {
            this.unReceivedMsgs = unReceivedMsgs;
        }

        public int getPushType() {
            return pushType;
        }

        public void setPushType(int pushType) {
            this.pushType = pushType;
        }

        public String getDeviceName() {
            return deviceName;
        }

        public void setDeviceName(String deviceName) {
            this.deviceName = deviceName;
        }

        public String getDeviceVersion() {
            return deviceVersion;
        }

        public void setDeviceVersion(String deviceVersion) {
            this.deviceVersion = deviceVersion;
        }

        public String getPhoneName() {
            return phoneName;
        }

        public void setPhoneName(String phoneName) {
            this.phoneName = phoneName;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        public String getCarrierName() {
            return carrierName;
        }

        public void setCarrierName(String carrierName) {
            this.carrierName = carrierName;
        }

        public long getUpdateDt() {
            return updateDt;
        }

        public void setUpdateDt(long updateDt) {
            this.updateDt = updateDt;
        }

        private int pushType;
        private int platform;

        private String deviceName;
        private String deviceVersion;
        private String phoneName;
        private String language;
        private String carrierName;
        private long updateDt;

        final ClientSession clientSession;
        final BlockingQueue<StoredMessage> queue = new ArrayBlockingQueue<>(Constants.MAX_MESSAGE_QUEUE);
        final Map<Integer, StoredMessage> secondPhaseStore = new ConcurrentHashMap<>();
        final Map<Integer, StoredMessage> outboundFlightMessages =
                Collections.synchronizedMap(new HashMap<Integer, StoredMessage>());
        final Map<Integer, StoredMessage> inboundFlightMessages = new ConcurrentHashMap<>();

        public Session(String username, String clientID, ClientSession clientSession) {
            this.clientID = clientID;
            this.clientSession = clientSession;
            this.username = username;
        }

        public String getAppName() {
            return appName;
        }

        public void setAppName(String appName) {
            this.appName = appName;
        }

        public String getDeviceToken() {
            return deviceToken;
        }

        public void setDeviceToken(String deviceToken) {
            this.deviceToken = deviceToken;
        }

        public int getPlatform() {
            return platform;
        }

        public void setPlatform(int platform) {
            this.platform = platform;
        }

        public String getClientID() {
			return clientID;
		}

		public String getUsername() {
			return username;
		}

        public void setUsername(String username) {
            this.username = username;
        }

        public ClientSession getClientSession() {
			return clientSession;
		}

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getDbSecret() {
            return dbSecret;
        }

        public void setDbSecret(String dbSecret) {
            this.dbSecret = dbSecret;
        }

        public String getVoipDeviceToken() {
            return voipDeviceToken;
        }

        public void setVoipDeviceToken(String voipDeviceToken) {
            this.voipDeviceToken = voipDeviceToken;
        }

        @Override
		public int compareTo(Session o) {
			// TODO Auto-generated method stub
			if (clientID.equals(o.clientID) && username.equals(o.username)) {
				return 0;
			}
			if (clientID.equals(o.clientID)) {
				return username.compareTo(o.username);
			} else {
				return clientID.compareTo(o.clientID);
			}
		}


        
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentSkipListSet<String>> userSessions = new ConcurrentHashMap<>();

    private final Server mServer;
    private final DatabaseStore databaseStore;
    public MemorySessionStore(Server server, DatabaseStore databaseStore) {
    		mServer = server;
    		this.databaseStore = databaseStore;
    }

    @Override
    public Session getSession(String clientID) {
        Session session = sessions.get(clientID);
        if (session == null) {
            LOG.error("Can't find the session for client <{}>", clientID);
        }
        return session;
    }

    @Override
    public void initStore() {
    }

    @Override
    public boolean contains(String clientID) {
        return sessions.containsKey(clientID);
    }

    @Override
    public void updateSessionToken(Session session, boolean voip) {
        databaseStore.updateSessionToken(session.getUsername(), session.getClientID(), voip ? session.getVoipDeviceToken() : session.getDeviceToken(), voip);
    }

    @Override
    public Session createUserSession(String username, String clientID) {
        LOG.debug("createUserSession for client <{}>, user <{}>", clientID, username);

        ClientSession clientSession = new ClientSession(clientID, this);
        Session session = databaseStore.getSession(username, clientID, clientSession);

        if (session == null) {
            session = databaseStore.createSession(username, clientID, clientSession);
        }

        return session;
    }


    @Override
    public ErrorCode createNewSession(String username, String clientID, boolean cleanSession) {
        LOG.debug("createNewSession for client <{}>", clientID);

        Session session = sessions.get(clientID);
        if (session != null) {
            LOG.error("already exists a session for client <{}>, bad condition", clientID);
            throw new IllegalArgumentException("Can't create a session with the ID of an already existing" + clientID);
        }


        ClientSession clientSession = new ClientSession(clientID, this);
        session = databaseStore.getSession(username, clientID, clientSession);

        if (session == null) {
            session = databaseStore.createSession(username, clientID, clientSession);
        }

        sessions.put(clientID, session);
        ConcurrentSkipListSet<String> sessionSet = userSessions.get(username);
        if (sessionSet == null) {
			sessionSet = new ConcurrentSkipListSet<>();
			userSessions.put(username, sessionSet);
		}
        sessionSet = userSessions.get(username);
        sessionSet.add(clientID);

        return ErrorCode.ERROR_CODE_SUCCESS;
    }

    @Override
    public ClientSession updateExistSession(String username, String clientID, WFCMessage.RouteRequest endpoint, boolean cleanSession) {
        LOG.debug("updateExistSession for client <{}>", clientID);
        Session session = sessions.get(clientID);
        if (session == null) {
            LOG.error("already exists a session for client <{}>, bad condition", clientID);
            throw new IllegalArgumentException("Can't create a session with the ID of an already existing" + clientID);
        }
        if (!session.getUsername().equals(username)) {
            ConcurrentSkipListSet<String> sessionSet = userSessions.get(session.getUsername());
            if(sessionSet != null) {
                sessionSet.remove(clientID);
            }
        }

        session.setUsername(username);
        sessions.put(clientID, session);

        ConcurrentSkipListSet<String> sessionSet = userSessions.get(username);
        if (sessionSet == null) {
            sessionSet = new ConcurrentSkipListSet<>();
            userSessions.put(username, sessionSet);
        }
        sessionSet = userSessions.get(username);
        sessionSet.add(clientID);

        if (endpoint != null) {
            databaseStore.updateSession(username, clientID, session, endpoint);
        }

        return session.clientSession;
    }

    @Override
    public Session sessionForClientAndUser(String username, String clientID) {
        Session session = sessions.get(clientID);
        if (session != null) {
            if (session.getUsername().equals(username)) {
                return session;
            } else {
                cleanSession(clientID);
            }
        }
        return null;
    }

    @Override
    public ClientSession sessionForClient(String clientID) {
        if (!sessions.containsKey(clientID)) {
            LOG.error("Can't find the session for client <{}>", clientID);
            return null;
        }

        Session session = sessions.get(clientID);

        return session.getClientSession();
    }

    @Override
    public void loadUserSession(String username, String clientID) {
        if (sessions.containsKey(clientID)) {
            return;
        }
        Session session = databaseStore.getSession(username, clientID, new ClientSession(clientID, this));
        if (session != null) {
            sessions.put(clientID, session);
        }
    }

    @Override
    public Collection<Session> sessionForUser(String username) {
    	ConcurrentSkipListSet<String> sessionSet = userSessions.get(username);
        if (sessionSet == null) {
			sessionSet = new ConcurrentSkipListSet<String>();
			userSessions.put(username, sessionSet);
		}
        sessionSet = userSessions.get(username);
        ArrayList<Session> out = new ArrayList<>();
        for (String clientId : sessionSet
             ) {
            Session session = sessions.get(clientId);
            if (session != null && session.getUsername().equals(username)) {
                out.add(session);
            }
        }
        return out;
    }

    @Override
    public Collection<ClientSession> getAllSessions() {
        Collection<ClientSession> result = new ArrayList<>();
        for (Session entry : sessions.values()) {
            result.add(new ClientSession(entry.clientID, this));
        }
        return result;
    }

    @Override
    public StoredMessage inFlightAck(String clientID, int messageID) {
        return getSession(clientID).outboundFlightMessages.remove(messageID);
    }

    @Override
    public void inFlight(String clientID, int messageID, StoredMessage msg) {
        Session session = sessions.get(clientID);
        if (session == null) {
            LOG.error("Can't find the session for client <{}>", clientID);
            return;
        }

        session.outboundFlightMessages.put(messageID, msg);
    }

    /**
     * Return the next valid packetIdentifier for the given client session.
     */
    @Override
    public int nextPacketID(String clientID) {
        if (!sessions.containsKey(clientID)) {
            LOG.error("Can't find the session for client <{}>", clientID);
            return -1;
        }

        Map<Integer, StoredMessage> m = sessions.get(clientID).outboundFlightMessages;
        int maxId = m.keySet().isEmpty() ? 0 : Collections.max(m.keySet());
        int nextPacketId = (maxId + 1) % 0xFFFF;
        m.put(nextPacketId, null);
        return nextPacketId;
    }

    @Override
    public BlockingQueue<StoredMessage> queue(String clientID) {
        if (!sessions.containsKey(clientID)) {
            LOG.error("Can't find the session for client <{}>", clientID);
            return null;
        }

        return sessions.get(clientID).queue;
    }

    @Override
    public void dropQueue(String clientID) {
        sessions.get(clientID).queue.clear();
    }

    @Override
    public void moveInFlightToSecondPhaseAckWaiting(String clientID, int messageID, StoredMessage msg) {
        LOG.info("Moving msg inflight second phase store, clientID <{}> messageID {}", clientID, messageID);
        Session session = sessions.get(clientID);
        if (session == null) {
            LOG.error("Can't find the session for client <{}>", clientID);
            return;
        }

        session.secondPhaseStore.put(messageID, msg);
        session.outboundFlightMessages.put(messageID, msg);
    }

    @Override
    public StoredMessage secondPhaseAcknowledged(String clientID, int messageID) {
        LOG.info("Acknowledged message in second phase, clientID <{}> messageID {}", clientID, messageID);
        return getSession(clientID).secondPhaseStore.remove(messageID);
    }

    @Override
    public int getInflightMessagesNo(String clientID) {
        Session session = sessions.get(clientID);
        if (session == null) {
            LOG.error("Can't find the session for client <{}>", clientID);
            return 0;
        }

        return session.inboundFlightMessages.size() + session.secondPhaseStore.size()
            + session.outboundFlightMessages.size();
    }

    @Override
    public StoredMessage inboundInflight(String clientID, int messageID) {
        return getSession(clientID).inboundFlightMessages.get(messageID);
    }

    @Override
    public void markAsInboundInflight(String clientID, int messageID, StoredMessage msg) {
        if (!sessions.containsKey(clientID))
            LOG.error("Can't find the session for client <{}>", clientID);

        sessions.get(clientID).inboundFlightMessages.put(messageID, msg);
    }

    @Override
    public int getPendingPublishMessagesNo(String clientID) {
        if (!sessions.containsKey(clientID)) {
            LOG.error("Can't find the session for client <{}>", clientID);
            return 0;
        }

        return sessions.get(clientID).queue.size();
    }

    @Override
    public int getSecondPhaseAckPendingMessages(String clientID) {
        if (!sessions.containsKey(clientID)) {
            LOG.error("Can't find the session for client <{}>", clientID);
            return 0;
        }

        return sessions.get(clientID).secondPhaseStore.size();
    }

    @Override
    public void cleanSession(String clientID) {
        LOG.info("Fooooooooo <{}>", clientID);

        Session session = sessions.get(clientID);
        if (session == null) {
            LOG.error("Can't find the session for client <{}>", clientID);
            return;
        }
        ConcurrentSkipListSet<String> sessionSet = userSessions.get(session.username);
        if (sessionSet == null) {
			sessionSet = new ConcurrentSkipListSet<>();
			userSessions.put(session.username, sessionSet);
		}
        sessionSet = userSessions.get(session.username);
        sessionSet.remove(clientID);

        // remove also the messages stored of type QoS1/2
        LOG.info("Removing stored messages with QoS 1 and 2. ClientId={}", clientID);

        session.secondPhaseStore.clear();
        session.outboundFlightMessages.clear();
        session.inboundFlightMessages.clear();

        LOG.info("Wiping existing subscriptions. ClientId={}", clientID);

        //remove also the enqueued messages
        dropQueue(clientID);

        // TODO this missing last step breaks the junit test
        sessions.remove(clientID);
    }
}
