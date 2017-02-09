
package io.moquette.spi.impl;

import io.moquette.spi.ClientSession;
import io.moquette.spi.IMessagesStore;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;

class InternalRepublisher {

    private static final Logger LOG = LoggerFactory.getLogger(InternalRepublisher.class);

    private final PersistentQueueMessageSender messageSender;

    InternalRepublisher(PersistentQueueMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    void publishRetained(ClientSession targetSession, Collection<IMessagesStore.StoredMessage> messages) {
        for (IMessagesStore.StoredMessage storedMsg : messages) {
            // fire as retained the message
            Integer packetID = storedMsg.getQos() == MqttQoS.AT_MOST_ONCE ? null : targetSession.nextPacketId();
            if (packetID != null) {
                LOG.debug(
                        "Adding message to inflight zone. "
                        + "MqttClientId = {}, packetId = {}, messageId = {}, topic = {}.",
                        targetSession.clientID,
                        packetID,
                        storedMsg.getMessageID(),
                        storedMsg.getTopic());
                targetSession.inFlightAckWaiting(storedMsg.getGuid(), packetID);
            }
            MqttPublishMessage publishMsg = retainedPublish(storedMsg);
            // set the PacketIdentifier only for QoS > 0
            if (publishMsg.fixedHeader().qosLevel() != MqttQoS.AT_MOST_ONCE) {
                publishMsg = retainedPublish(storedMsg, packetID);
            }
            this.messageSender.sendPublish(targetSession, publishMsg);
        }
    }

    void publishStored(ClientSession clientSession, BlockingQueue<IMessagesStore.StoredMessage> publishedEvents) {
        List<IMessagesStore.StoredMessage> storedPublishes = new ArrayList<>();
        publishedEvents.drainTo(storedPublishes);

        for (IMessagesStore.StoredMessage pubEvt : storedPublishes) {
            // put in flight zone
            LOG.debug(
                    "Adding message ot inflight zone. MqttClientId = {}, guid = {}, messageId = {}, topic = {}.",
                    clientSession.clientID,
                    pubEvt.getGuid(),
                    pubEvt.getMessageID(),
                    pubEvt.getTopic());
            clientSession.inFlightAckWaiting(pubEvt.getGuid(), pubEvt.getMessageID());
            MqttPublishMessage publishMsg = notRetainedPublish(pubEvt);
            // set the PacketIdentifier only for QoS > 0
            if (publishMsg.fixedHeader().qosLevel() != MqttQoS.AT_MOST_ONCE) {
                publishMsg = notRetainedPublish(pubEvt, pubEvt.getMessageID());
            }
            this.messageSender.sendPublish(clientSession, publishMsg);
        }
    }

    private MqttPublishMessage notRetainedPublish(IMessagesStore.StoredMessage storedMessage, Integer messageID) {
        return createPublishForQos(
                storedMessage.getTopic(),
                storedMessage.getQos(),
                storedMessage.getMessage(),
                false,
                messageID);
    }

    private MqttPublishMessage notRetainedPublish(IMessagesStore.StoredMessage storedMessage) {
        return createPublishForQos(
                storedMessage.getTopic(),
                storedMessage.getQos(),
                storedMessage.getMessage(),
                false,
                0);
    }

    private MqttPublishMessage retainedPublish(IMessagesStore.StoredMessage storedMessage) {
        return createPublishForQos(
                storedMessage.getTopic(),
                storedMessage.getQos(),
                storedMessage.getMessage(),
                true,
                0);
    }

    private MqttPublishMessage retainedPublish(IMessagesStore.StoredMessage storedMessage, Integer packetID) {
        return createPublishForQos(
                storedMessage.getTopic(),
                storedMessage.getQos(),
                storedMessage.getMessage(),
                true,
                packetID);
    }

    public static MqttPublishMessage createPublishForQos(String topic, MqttQoS qos, ByteBuf message, boolean retained,
            int messageId) {
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBLISH, false, qos, retained, 0);
        MqttPublishVariableHeader varHeader = new MqttPublishVariableHeader(topic, messageId);
        return new MqttPublishMessage(fixedHeader, varHeader, message);
    }
}
