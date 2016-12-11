package io.moquette.spi.impl;

import io.moquette.parser.proto.messages.AbstractMessage;
import io.moquette.parser.proto.messages.PublishMessage;
import io.moquette.server.ConnectionDescriptor;
import io.moquette.spi.ClientSession;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.SubscriptionsStore;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

class Qos0Publisher {

    private static final Logger LOG = LoggerFactory.getLogger(Qos0Publisher.class);
    private final ConcurrentMap<String, ConnectionDescriptor> connectionDescriptors;
    private final ISessionsStore m_sessionsStore;

    public Qos0Publisher(ConcurrentMap<String, ConnectionDescriptor> connectionDescriptors, ISessionsStore sessionsStore) {
        this.connectionDescriptors = connectionDescriptors;
        this.m_sessionsStore = sessionsStore;
    }

    void publish2Subscribers_qos0(IMessagesStore.StoredMessage pubMsg, List<Subscription> topicMatchingSubscriptions) {
        final String topic = pubMsg.getTopic();
        final ByteBuffer origMessage = pubMsg.getMessage();
//        LOG.debug("publish2Subscribers republishing to existing subscribers that matches the topic {}", topic);
//        if (LOG.isTraceEnabled()) {
//            LOG.trace("content <{}>", DebugUtils.payload2Str(origMessage));
//            LOG.trace("subscription tree {}", subscriptions.dumpTree());
//        }

        LOG.trace("Found {} matching subscriptions to <{}>", topicMatchingSubscriptions.size(), topic);
        for (final Subscription sub : topicMatchingSubscriptions) {
            boolean targetIsActive = connectionDescriptors.containsKey(sub.getClientId());

            LOG.debug("Broker republishing to client <{}> topic <{}> qos <{}>, active {}",
                    sub.getClientId(), sub.getTopicFilter(), AbstractMessage.QOSType.MOST_ONE, targetIsActive);
            if (targetIsActive) {
                ClientSession targetSession = m_sessionsStore.sessionForClient(sub.getClientId());
                ByteBuffer message = origMessage.duplicate();
                publishQos0(targetSession, topic, message);
            }
        }
    }

    private void publishQos0(ClientSession clientsession, String topic, ByteBuffer message) {
        String clientId = clientsession.clientID;

        LOG.debug("publishQos0 invoked clientId <{}> on topic <{}>", clientId, topic);
        PublishMessage pubMessage = new PublishMessage();
        pubMessage.setRetainFlag(false);
        pubMessage.setTopicName(topic);
        pubMessage.setQos(AbstractMessage.QOSType.MOST_ONE);
        pubMessage.setPayload(message);

        LOG.info("send publish message to <{}> on topic <{}>", clientId, topic);
        if (LOG.isDebugEnabled()) {
            LOG.debug("content <{}>", DebugUtils.payload2Str(message));
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
        //TODO attention channel could be null, because in the mean time it get closed

        LOG.trace("Session for clientId {}", clientId);
        if (channel.isWritable()) {
            LOG.debug("channel is writable");
            //if channel is writable don't enqueue
            channel.writeAndFlush(pubMessage);
        }
    }
}
