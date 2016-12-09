package io.moquette.spi.impl;

import io.moquette.parser.proto.messages.AbstractMessage;
import io.moquette.parser.proto.messages.PublishMessage;
import io.moquette.server.ConnectionDescriptor;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.ClientSession;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.SubscriptionsStore;
import io.moquette.spi.security.IAuthorizator;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static io.moquette.spi.impl.ProtocolProcessor.asStoredMessage;

public class Qos0PublishHandler {

    private static final Logger LOG = LoggerFactory.getLogger(Qos0PublishHandler.class);

    private final IAuthorizator m_authorizator;
    private final SubscriptionsStore subscriptions;
    private final IMessagesStore m_messagesStore;
    private final BrokerInterceptor m_interceptor;
    private final ConcurrentMap<String, ConnectionDescriptor> connectionDescriptors;
    private final ISessionsStore m_sessionsStore;

    public Qos0PublishHandler(IAuthorizator authorizator, SubscriptionsStore subscriptions,
                              IMessagesStore messagesStore, BrokerInterceptor interceptor, ConcurrentMap<String,
            ConnectionDescriptor> connectionDescriptors, ISessionsStore sessionsStore) {
        this.m_authorizator = authorizator;
        this.subscriptions = subscriptions;
        this.m_messagesStore = messagesStore;
        this.m_interceptor = interceptor;
        this.connectionDescriptors = connectionDescriptors;
        this.m_sessionsStore = sessionsStore;
    }

    void receivedPublishQos0(Channel channel, PublishMessage msg) {
        //verify if topic can be write
        final String topic = msg.getTopicName();
        if (checkWriteOnTopic(topic, channel)) {
            return;
        }

        //route message to subscribers
        IMessagesStore.StoredMessage toStoreMsg = asStoredMessage(msg);
        String clientID = NettyUtils.clientID(channel);
        toStoreMsg.setClientID(clientID);

        List<Subscription> topicMatchingSubscriptions = subscriptions.matches(topic);
        publish2Subscribers_qos0(toStoreMsg, topicMatchingSubscriptions);

        if (msg.isRetainFlag()) {
            //QoS == 0 && retain => clean old retained
            m_messagesStore.cleanRetained(topic);
        }

        String username = NettyUtils.userName(channel);
        m_interceptor.notifyTopicPublished(msg, clientID, username);
    }

    boolean checkWriteOnTopic(String topic, Channel channel) {
        String clientID = NettyUtils.clientID(channel);
        String username = NettyUtils.userName(channel);
        if (!m_authorizator.canWrite(topic, username, clientID)) {
            LOG.debug("topic {} doesn't have write credentials", topic);
            return true;
        }
        return false;
    }

    void publish2Subscribers_qos0(IMessagesStore.StoredMessage pubMsg, List<Subscription> topicMatchingSubscriptions) {
        final String topic = pubMsg.getTopic();
        final ByteBuffer origMessage = pubMsg.getMessage();
        LOG.debug("publish2Subscribers republishing to existing subscribers that matches the topic {}", topic);
        if (LOG.isTraceEnabled()) {
            LOG.trace("content <{}>", DebugUtils.payload2Str(origMessage));
            LOG.trace("subscription tree {}", subscriptions.dumpTree());
        }

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

    protected void publishQos0(ClientSession clientsession, String topic, ByteBuffer message) {
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
