package io.moquette.spi.impl;

import io.moquette.parser.proto.messages.PubAckMessage;
import io.moquette.parser.proto.messages.PublishMessage;
import io.moquette.server.ConnectionDescriptor;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.MessageGUID;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.SubscriptionsStore;
import io.moquette.spi.security.IAuthorizator;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static io.moquette.spi.impl.ProtocolProcessor.asStoredMessage;

class Qos1PublishHandler {
    private static final Logger LOG = LoggerFactory.getLogger(Qos1PublishHandler.class);

    private final IAuthorizator m_authorizator;
    private final SubscriptionsStore subscriptions;
    private final IMessagesStore m_messagesStore;
    private final BrokerInterceptor m_interceptor;
    private final ConcurrentMap<String, ConnectionDescriptor> connectionDescriptors;
    private final String brokerPort;
    private final MessagesPublisher publisher;

    public Qos1PublishHandler(IAuthorizator authorizator, SubscriptionsStore subscriptions,
                              IMessagesStore messagesStore, BrokerInterceptor interceptor,
                              ConcurrentMap<String, ConnectionDescriptor> connectionDescriptors,
                              String brokerPort, MessagesPublisher messagesPublisher) {
        this.m_authorizator = authorizator;
        this.subscriptions = subscriptions;
        this.m_messagesStore = messagesStore;
        this.m_interceptor = interceptor;
        this.connectionDescriptors = connectionDescriptors;
        this.brokerPort = brokerPort;
        this.publisher = messagesPublisher;
    }

    void receivedPublishQos1(Channel channel, PublishMessage msg) {
        //verify if topic can be write
        final String topic = msg.getTopicName();
        if (checkWriteOnTopic(topic, channel)) {
            return;
        }

        //route message to subscribers
        IMessagesStore.StoredMessage toStoreMsg = asStoredMessage(msg);
        String clientID = NettyUtils.clientID(channel);
        toStoreMsg.setClientID(clientID);

        LOG.debug("publish2Subscribers_qos1 republishing to existing subscribers that matches the topic {}", topic);
        if (LOG.isTraceEnabled()) {
            LOG.trace("content <{}>", DebugUtils.payload2Str(toStoreMsg.getMessage()));
            LOG.trace("subscription tree {}", subscriptions.dumpTree());
        }
        List<Subscription> topicMatchingSubscriptions = subscriptions.matches(topic);
        this.publisher.publish2Subscribers(toStoreMsg, topicMatchingSubscriptions);

        //send PUBACK
        final Integer messageID = msg.getMessageID();
        if (msg.isLocal()) {
            sendPubAck(clientID, messageID);
        }
        LOG.info("server {} replying with PubAck to MSG ID {}", brokerPort, messageID);

        if (msg.isRetainFlag()) {
            if (!msg.getPayload().hasRemaining()) {
                m_messagesStore.cleanRetained(topic);
            } else {
                //before wasn't stored
                MessageGUID guid = m_messagesStore.storePublishForFuture(toStoreMsg);
                m_messagesStore.storeRetained(topic, guid);
            }
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

    private void sendPubAck(String clientId, int messageID) {
        LOG.trace("sendPubAck invoked");
        PubAckMessage pubAckMessage = new PubAckMessage();
        pubAckMessage.setMessageID(messageID);

        try {
            if (connectionDescriptors == null) {
                throw new RuntimeException("Internal bad error, found connectionDescriptors to null while it should be initialized, somewhere it's overwritten!!");
            }
            LOG.debug("clientIDs are {}", connectionDescriptors);
            if (connectionDescriptors.get(clientId) == null) {
                throw new RuntimeException(String.format("Can't find a ConnectionDescriptor for client %s in cache %s", clientId, connectionDescriptors));
            }
            connectionDescriptors.get(clientId).channel.writeAndFlush(pubAckMessage);
        } catch(Throwable t) {
            LOG.error(null, t);
        }
    }

}
