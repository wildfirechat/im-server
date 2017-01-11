package io.moquette.server;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.moquette.parser.proto.messages.AbstractMessage;

public class ConnectionDescriptorStore {

	private static final Logger LOG = LoggerFactory.getLogger(ConnectionDescriptorStore.class);

	private final ConcurrentMap<String, ConnectionDescriptor> connectionDescriptors;

	public ConnectionDescriptorStore() {
		this.connectionDescriptors = new ConcurrentHashMap<String, ConnectionDescriptor>();
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
	
	public boolean isConnected(String clientID) {
		return connectionDescriptors.containsKey(clientID);
	}

}
