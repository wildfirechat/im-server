package io.moquette.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.moquette.connections.IConnectionsManager;
import io.moquette.connections.MqttConnectionMetrics;
import io.moquette.connections.MqttSession;
import io.moquette.connections.MqttSubscription;
import io.moquette.parser.proto.messages.AbstractMessage;
import io.moquette.server.netty.metrics.BytesMetrics;
import io.moquette.server.netty.metrics.MessageMetrics;
import io.moquette.spi.ClientSession;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.subscriptions.Subscription;

public class ConnectionDescriptorStore implements IConnectionsManager {

	private static final Logger LOG = LoggerFactory.getLogger(ConnectionDescriptorStore.class);

	private final ConcurrentMap<String, ConnectionDescriptor> connectionDescriptors;
	private final ISessionsStore sessionsStore;

	public ConnectionDescriptorStore(ISessionsStore sessionsStore) {
		this.connectionDescriptors = new ConcurrentHashMap<String, ConnectionDescriptor>();
		this.sessionsStore = sessionsStore;
	}

	public boolean sendMessage(AbstractMessage message, String clientId) {
		return sendMessage(message, null, clientId);
	}

	public boolean sendMessage(AbstractMessage message, Integer messageID, String clientID) {
		try {
			// The connection descriptors map will never be null: it's a final
			// attribute.

			if (messageID != null) {
				LOG.info("Sending {} message. MqttClientId = {}, messageId = {}.", message.getMessageType(), clientID,
						messageID);
			} else {
				LOG.debug("Sending {} message. MqttClientId = {}.", message.getMessageType(), clientID);
			}

			ConnectionDescriptor descriptor = connectionDescriptors.get(clientID);
			if (descriptor == null) {
				if (messageID != null) {
					LOG.error(
							"The client has just disconnected. The {} message could not be sent. MqttClientId = {}, messageId = {}.",
							message.getMessageType(), clientID, messageID);
				} else {
					LOG.error("The client has just disconnected. The {} could not be sent. MqttClientId = {}.",
							message.getMessageType(), clientID);
				}
				/*
				 * If the client hast just disconnected, its connection
				 * descriptor will be null. We don't have to make the broker
				 * crash: we'll just discard the PUBACK message.
				 */
				return false;
			}
			descriptor.writeAndFlush(message);
			return true;
		} catch (Throwable e) {
			if (messageID != null) {
				LOG.error(
						"Unable to send {} message. MqttClientId = {}, messageId = {}, cause = {}, errorMessage = {}.",
						message.getMessageType(), clientID, messageID, e.getCause(), e.getMessage());
			} else {
				LOG.error("Unable to send {} message. MqttClientId = {}, cause = {}, errorMessage = {}.",
						message.getMessageType(), clientID, e.getCause(), e.getMessage());
			}
			return false;
		}
	}

	public ConnectionDescriptor addConnection(ConnectionDescriptor descriptor) {
		return connectionDescriptors.putIfAbsent(descriptor.clientID, descriptor);
	}

	public boolean removeConnection(ConnectionDescriptor descriptor) {
		return connectionDescriptors.remove(descriptor.clientID, descriptor);
	}

	public ConnectionDescriptor getConnection(String clientID) {
		return connectionDescriptors.get(clientID);
	}
	
    @Override
    public boolean isConnected(String clientID) {
        return connectionDescriptors.containsKey(clientID);
    }

    @Override
    public int getActiveConnectionsNo() {
        return connectionDescriptors.size();
    }

    @Override
    public Collection<String> getConnectedClientIds() {
        return connectionDescriptors.keySet();
    }

    @Override
    public boolean closeConnection(String clientID, boolean closeImmediately) {
        ConnectionDescriptor descriptor = connectionDescriptors.get(clientID);
        if (descriptor == null) {
            LOG.error(
                    "The connection descriptor doesn't exist. The MQTT connection cannot be closed. MqttClientId = {}, closeImmediately = {}.",
                    clientID, closeImmediately);
            return false;
        }
        if (closeImmediately) {
            descriptor.abort();
            return true;
        } else {
            return descriptor.close();
        }
    }

    @Override
    public MqttSession getSessionStatus(String clientID) {
        LOG.info("Retrieving status of session. MqttClientId = {}.", clientID);
        ClientSession session = sessionsStore.sessionForClient(clientID);
        if (session == null) {
            LOG.error("The given MQTT client ID doesn't have an associated session. MqttClientId = {}.", clientID);
            return null;
        }
        return buildMqttSession(session);
    }

    @Override
    public Collection<MqttSession> getSessions() {
        LOG.info("Retrieving status of all sessions.");
        Collection<MqttSession> result = new ArrayList<MqttSession>();
        for (ClientSession session : sessionsStore.getAllSessions()) {
            result.add(buildMqttSession(session));
        }
        return result;
    }

    private MqttSession buildMqttSession(ClientSession session) {
        MqttSession result = new MqttSession();
        Collection<MqttSubscription> mqttSubscriptions = new ArrayList<>();
        for (Subscription subscription : session.getSubscriptions()) {
            mqttSubscriptions.add(new MqttSubscription(subscription.getRequestedQos().toString(),
                    subscription.getClientId(), subscription.getTopicFilter(), subscription.isActive()));
        }
        result.setActiveSubscriptions(mqttSubscriptions);
        result.setCleanSession(session.isCleanSession());
        ConnectionDescriptor descriptor = this.getConnection(session.clientID);
        if (descriptor != null) {
            result.setConnectionEstablished(true);
            BytesMetrics bytesMetrics = descriptor.getBytesMetrics();
            MessageMetrics messageMetrics = descriptor.getMessageMetrics();
            result.setConnectionMetrics(new MqttConnectionMetrics(bytesMetrics.readBytes(), bytesMetrics.wroteBytes(),
                    messageMetrics.messagesRead(), messageMetrics.messagesWrote()));
        } else {
            result.setConnectionEstablished(false);
        }
        result.setPendingPublishMessagesNo(session.getPendingPublishMessagesNo());
        result.setSecondPhaseAckPendingMessages(session.getSecondPhaseAckPendingMessages());
        result.setInflightMessages(session.getInflightMessagesNo());
        LOG.info("The status of the session has been retrieved successfully. MqttClientId = {}.", session.clientID);
        return result;
    }

}
