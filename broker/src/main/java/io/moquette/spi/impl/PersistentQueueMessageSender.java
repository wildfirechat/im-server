
package io.moquette.spi.impl;

import io.moquette.server.ConnectionDescriptorStore;
import io.moquette.spi.ClientSession;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static io.moquette.spi.impl.ProtocolProcessor.asStoredMessage;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;

class PersistentQueueMessageSender {

    private static final Logger LOG = LoggerFactory.getLogger(PersistentQueueMessageSender.class);
    private final ConnectionDescriptorStore connectionDescriptorStore;

    public PersistentQueueMessageSender(ConnectionDescriptorStore connectionDescriptorStore) {
        this.connectionDescriptorStore = connectionDescriptorStore;
    }

    void sendPublish(ClientSession clientsession, MqttPublishMessage pubMessage) {
        String clientId = clientsession.clientID;
        final int messageId = pubMessage.variableHeader().messageId();
        final String topicName = pubMessage.variableHeader().topicName();
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "Sending PUBLISH message. MessageId={}, CId={}, topic={}, qos={}, payload={}",
                    messageId,
                    clientId,
                    topicName,
                    DebugUtils.payload2Str(pubMessage.payload()));
        } else {
            LOG.info("Sending PUBLISH message. MessageId={}, CId={}, topic={}", messageId, clientId, topicName);
        }

        boolean messageDelivered = connectionDescriptorStore.sendMessage(pubMessage, messageId, clientId);

        MqttQoS qos = pubMessage.fixedHeader().qosLevel();
        if (!messageDelivered && qos != AT_MOST_ONCE && !clientsession.isCleanSession()) {
            LOG.warn(
                    "The PUBLISH message could not be delivered. It will be stored. "
                    + "MessageId = {}, CId={}, topic = {}, qos = {}, cleanSession = {}.",
                    messageId,
                    clientId,
                    topicName,
                    qos,
                    false);
            clientsession.enqueue(asStoredMessage(pubMessage));
        } else {
            LOG.warn(
                    "The PUBLISH message could not be delivered. It will be discarded. "
                    + "MessageId = {}, CId={}, topic = {}, qos = {}, cleanSession = {}.",
                    messageId,
                    clientId,
                    topicName,
                    qos,
                    true);
        }
    }
}
