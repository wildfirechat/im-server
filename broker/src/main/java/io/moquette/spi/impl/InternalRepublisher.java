package io.moquette.spi.impl;


import io.moquette.parser.proto.messages.AbstractMessage;
import io.moquette.parser.proto.messages.PublishMessage;
import io.moquette.spi.ClientSession;
import io.moquette.spi.IMessagesStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

class InternalRepublisher {

    private static final Logger LOG = LoggerFactory.getLogger(InternalRepublisher.class);

    private final PersistentQueueMessageSender messageSender;

    InternalRepublisher(PersistentQueueMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    void publishRetained(ClientSession targetSession, Collection<IMessagesStore.StoredMessage> messages) {
        for (IMessagesStore.StoredMessage storedMsg : messages) {
            //fire as retained the message
            Integer packetID = storedMsg.getQos() == AbstractMessage.QOSType.MOST_ONE ? null : targetSession.nextPacketId();
            if (packetID != null) {
                LOG.trace("Adding to inflight <{}>", packetID);
                targetSession.inFlightAckWaiting(storedMsg.getGuid(), packetID);
            }
            PublishMessage publishMsg = retainedPublish(storedMsg);
            //set the PacketIdentifier only for QoS > 0
            if (publishMsg.getQos() != AbstractMessage.QOSType.MOST_ONE) {
                publishMsg.setMessageID(packetID);
            }
            this.messageSender.sendPublish(targetSession, publishMsg);
        }
    }

    void publishStored(ClientSession clientSession, List<IMessagesStore.StoredMessage> publishedEvents) {
        for (IMessagesStore.StoredMessage pubEvt : publishedEvents) {
            //put in flight zone
            LOG.trace("Adding to inflight <{}>", pubEvt.getMessageID());
            clientSession.inFlightAckWaiting(pubEvt.getGuid(), pubEvt.getMessageID());
            PublishMessage publishMsg = notRetainedPublish(pubEvt);
            //set the PacketIdentifier only for QoS > 0
            if (publishMsg.getQos() != AbstractMessage.QOSType.MOST_ONE) {
                publishMsg.setMessageID(pubEvt.getMessageID());
            }
            this.messageSender.sendPublish(clientSession, publishMsg);
        }
    }

    private PublishMessage notRetainedPublish(IMessagesStore.StoredMessage storedMessage) {
        return createPublishForQos(storedMessage.getTopic(), storedMessage.getQos(), storedMessage.getMessage(), false);
    }

    private PublishMessage retainedPublish(IMessagesStore.StoredMessage storedMessage) {
        return createPublishForQos(storedMessage.getTopic(), storedMessage.getQos(), storedMessage.getMessage(), true);
    }

    private PublishMessage createPublishForQos(String topic, AbstractMessage.QOSType qos, ByteBuffer message, boolean retained) {
        PublishMessage pubMessage = new PublishMessage();
        pubMessage.setRetainFlag(retained);
        pubMessage.setTopicName(topic);
        pubMessage.setQos(qos);
        pubMessage.setPayload(message);
        return pubMessage;
    }
}
