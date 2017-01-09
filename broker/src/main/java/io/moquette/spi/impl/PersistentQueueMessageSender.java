package io.moquette.spi.impl;

import io.moquette.parser.proto.messages.PublishMessage;
import io.moquette.server.ConnectionDescriptor;
import io.moquette.spi.ClientSession;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentMap;

import static io.moquette.parser.proto.messages.AbstractMessage.QOSType.MOST_ONE;
import static io.moquette.spi.impl.ProtocolProcessor.asStoredMessage;

class PersistentQueueMessageSender {

    private static final Logger LOG = LoggerFactory.getLogger(PersistentQueueMessageSender.class);
    private final ConcurrentMap<String, ConnectionDescriptor> connectionDescriptors;

    public PersistentQueueMessageSender(ConcurrentMap<String, ConnectionDescriptor> connectionDescriptors) {
        this.connectionDescriptors = connectionDescriptors;
    }

    void sendPublish(ClientSession clientsession, PublishMessage pubMessage) {
        String clientId = clientsession.clientID;
        LOG.info("send publish message to <{}> on topic <{}>", clientId, pubMessage.getTopicName());
        if (LOG.isDebugEnabled()) {
            LOG.debug("directSend invoked clientId <{}> on topic <{}> QoS {} retained {} messageID {}",
                    clientId, pubMessage.getTopicName(), pubMessage.getQos(), false, pubMessage.getMessageID());
            LOG.debug("content <{}>", DebugUtils.payload2Str(pubMessage.getPayload()));
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
        } else if (pubMessage.getQos() != MOST_ONE) {
            //enqueue to the client session
            LOG.debug("enqueue to client session");
            clientsession.enqueue(asStoredMessage(pubMessage));
        }
    }
}
