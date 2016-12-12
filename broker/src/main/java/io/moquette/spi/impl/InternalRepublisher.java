package io.moquette.spi.impl;


import io.moquette.parser.proto.messages.AbstractMessage;
import io.moquette.parser.proto.messages.PublishMessage;
import io.moquette.server.ConnectionDescriptor;
import io.moquette.spi.ClientSession;
import io.moquette.spi.IMessagesStore;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

class InternalRepublisher {

    private static final Logger LOG = LoggerFactory.getLogger(InternalRepublisher.class);

    private final ConcurrentMap<String, ConnectionDescriptor> connectionDescriptors;

    InternalRepublisher(ConcurrentMap<String, ConnectionDescriptor> connectionDescriptors) {
        this.connectionDescriptors = connectionDescriptors;
    }

    void publishRetained(ClientSession targetSession, Collection<IMessagesStore.StoredMessage> messages) {
        for (IMessagesStore.StoredMessage storedMsg : messages) {
            //fire as retained the message
            Integer packetID = storedMsg.getQos() == AbstractMessage.QOSType.MOST_ONE ? null : targetSession.nextPacketId();
            if (packetID != null) {
                LOG.trace("Adding to inflight <{}>", packetID);
                targetSession.inFlightAckWaiting(storedMsg.getGuid(), packetID);
            }
            directSend(targetSession, storedMsg.getTopic(), storedMsg.getQos(), storedMsg.getPayload(), true, packetID);
        }
    }

    void publishStored(ClientSession clientSession, List<IMessagesStore.StoredMessage> publishedEvents) {
        for (IMessagesStore.StoredMessage pubEvt : publishedEvents) {
            //put in flight zone
            LOG.trace("Adding to inflight <{}>", pubEvt.getMessageID());
            clientSession.inFlightAckWaiting(pubEvt.getGuid(), pubEvt.getMessageID());
            directSend(clientSession, pubEvt.getTopic(), pubEvt.getQos(),
                    pubEvt.getMessage(), false, pubEvt.getMessageID());
        }
    }

    private void directSend(ClientSession clientsession, String topic, AbstractMessage.QOSType qos,
                              ByteBuffer message, boolean retained, Integer messageID) {
        String clientId = clientsession.clientID;
        LOG.debug("directSend invoked clientId <{}> on topic <{}> QoS {} retained {} messageID {}",
                clientId, topic, qos, retained, messageID);
        PublishMessage pubMessage = new PublishMessage();
        pubMessage.setRetainFlag(retained);
        pubMessage.setTopicName(topic);
        pubMessage.setQos(qos);
        pubMessage.setPayload(message);

        LOG.info("send publish message to <{}> on topic <{}>", clientId, topic);
        if (LOG.isDebugEnabled()) {
            LOG.debug("content <{}>", DebugUtils.payload2Str(message));
        }
        //set the PacketIdentifier only for QoS > 0
        if (pubMessage.getQos() != AbstractMessage.QOSType.MOST_ONE) {
            pubMessage.setMessageID(messageID);
        } else {
            if (messageID != null) {
                throw new RuntimeException("Internal bad error, trying to forwardPublish a QoS 0 message " +
                        "with PacketIdentifier: " + messageID);
            }
        }

        if (connectionDescriptors == null) {
            throw new RuntimeException("Internal bad error, found connectionDescriptors to null while it should be " +
                    "initialized, somewhere it's overwritten!!");
        }
        if (connectionDescriptors.get(clientId) == null) {
            //TODO while we were publishing to the target client, that client disconnected,
            // could happen is not an error HANDLE IT
            throw new RuntimeException(String.format("Can't find a ConnectionDescriptor for client <%s> in cache <%s>",
                    clientId, connectionDescriptors));
        }
        Channel channel = connectionDescriptors.get(clientId).channel;
        LOG.trace("Session for clientId {}", clientId);
        if (channel.isWritable()) {
            LOG.debug("channel is writable");
            //if channel is writable don't enqueue
            channel.writeAndFlush(pubMessage);
        } else {
            //enqueue to the client session
            LOG.debug("enqueue to client session");
            clientsession.enqueue(pubMessage);
        }
    }
}
