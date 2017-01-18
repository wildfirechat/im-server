package io.moquette.spi.impl;

import io.moquette.parser.proto.messages.PublishMessage;
import io.moquette.server.ConnectionDescriptorStore;
import io.moquette.spi.ClientSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static io.moquette.parser.proto.messages.AbstractMessage.QOSType.MOST_ONE;
import static io.moquette.spi.impl.ProtocolProcessor.asStoredMessage;

class PersistentQueueMessageSender {

    private static final Logger LOG = LoggerFactory.getLogger(PersistentQueueMessageSender.class);
    private final ConnectionDescriptorStore connectionDescriptorStore;

    public PersistentQueueMessageSender(ConnectionDescriptorStore connectionDescriptorStore) {
        this.connectionDescriptorStore = connectionDescriptorStore;
    }

    void sendPublish(ClientSession clientsession, PublishMessage pubMessage) {
        String clientId = clientsession.clientID;
        if (LOG.isDebugEnabled()) {
        	LOG.debug("Sending PUBLISH message. MessageId = {}, mqttClientId = {}, topic = {}, qos = {}, payload = {}.",
        			pubMessage.getMessageID(),
    				clientId, pubMessage.getTopicName(), DebugUtils.payload2Str(pubMessage.getPayload()));
        } else {
			LOG.info("Sending PUBLISH message. MessageId = {}, mqttClientId = {}, topic = {}.", pubMessage.getMessageID(),
					clientId, pubMessage.getTopicName());
        }

		boolean messageDelivered = connectionDescriptorStore.sendMessage(pubMessage, pubMessage.getMessageID(), clientId);
		
		if (!messageDelivered && pubMessage.getQos() != MOST_ONE && !clientsession.isCleanSession()) {
			LOG.warn(
					"The PUBLISH message could not be delivered. It will be stored. MessageId = {}, mqttClientId = {}, topic = {}, qos = {}, cleanSession = {}.",
					pubMessage.getMessageID(), clientId, pubMessage.getTopicName(), pubMessage.getQos(), false);
			clientsession.enqueue(asStoredMessage(pubMessage));
		} else {
			LOG.warn(
					"The PUBLISH message could not be delivered. It will be discarded. MessageId = {}, mqttClientId = {}, topic = {}, qos = {}, cleanSession = {}.",
					pubMessage.getMessageID(), clientId, pubMessage.getTopicName(), pubMessage.getQos(), true);
		}
    }
}
