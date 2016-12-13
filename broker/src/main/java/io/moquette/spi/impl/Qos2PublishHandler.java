package io.moquette.spi.impl;

import io.moquette.parser.proto.messages.*;
import io.moquette.server.ConnectionDescriptor;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.ClientSession;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
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

class Qos2PublishHandler {

    private static final Logger LOG = LoggerFactory.getLogger(Qos1PublishHandler.class);

    private final IAuthorizator m_authorizator;
    private final SubscriptionsStore subscriptions;
    private final IMessagesStore m_messagesStore;
    private final BrokerInterceptor m_interceptor;
    private final ConcurrentMap<String, ConnectionDescriptor> connectionDescriptors;
    private final ISessionsStore m_sessionsStore;
    private final String brokerPort;
    private final MessagesPublisher publisher;

    public Qos2PublishHandler(IAuthorizator authorizator, SubscriptionsStore subscriptions,
                              IMessagesStore messagesStore, BrokerInterceptor interceptor,
                              ConcurrentMap<String, ConnectionDescriptor> connectionDescriptors,
                              ISessionsStore sessionsStore, String brokerPort, MessagesPublisher messagesPublisher) {
        this.m_authorizator = authorizator;
        this.subscriptions = subscriptions;
        this.m_messagesStore = messagesStore;
        this.m_interceptor = interceptor;
        this.connectionDescriptors = connectionDescriptors;
        this.m_sessionsStore = sessionsStore;
        this.brokerPort = brokerPort;
        this.publisher = messagesPublisher;
    }

    void receivedPublishQos2(Channel channel, PublishMessage msg) {
        final AbstractMessage.QOSType qos = AbstractMessage.QOSType.EXACTLY_ONCE;
        final String topic = msg.getTopicName();
        //check if the topic can be wrote
        if (checkWriteOnTopic(topic, channel)) {
            return;
        }
        final Integer messageID = msg.getMessageID();

        IMessagesStore.StoredMessage toStoreMsg = asStoredMessage(msg);
        String clientID = NettyUtils.clientID(channel);
        toStoreMsg.setClientID(clientID);
        LOG.info("PUBLISH on server {} from clientID <{}> on topic <{}> with QoS {}", this.brokerPort, clientID, topic, qos);
        //QoS2
        MessageGUID guid = m_messagesStore.storePublishForFuture(toStoreMsg);
        if (msg.isLocal()) {
            sendPubRec(clientID, messageID);
        }
        //Next the client will send us a pub rel
        //NB publish to subscribers for QoS 2 happen upon PUBREL from publisher

        if (msg.isRetainFlag()) {
            if (!msg.getPayload().hasRemaining()) {
                m_messagesStore.cleanRetained(topic);
            } else {
                m_messagesStore.storeRetained(topic, guid);
            }
        }
        String username = NettyUtils.userName(channel);
        m_interceptor.notifyTopicPublished(msg, clientID, username);
    }

    /**
     * Second phase of a publish QoS2 protocol, sent by publisher to the broker. Search the stored message and publish
     * to all interested subscribers.
     * */
    void processPubRel(Channel channel, PubRelMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        int messageID = msg.getMessageID();
        LOG.debug("PUB --PUBREL--> SRV processPubRel invoked for clientID {} ad messageID {}", clientID, messageID);
        ClientSession targetSession = m_sessionsStore.sessionForClient(clientID);
        IMessagesStore.StoredMessage evt = targetSession.storedMessage(messageID);
        final String topic = evt.getTopic();
        List<Subscription> topicMatchingSubscriptions = subscriptions.matches(topic);
        LOG.debug("publish2Subscribers republishing to existing subscribers that matches the topic {}", topic);
        if (LOG.isTraceEnabled()) {
            LOG.trace("content <{}>", DebugUtils.payload2Str(evt.getMessage()));
            LOG.trace("subscription tree {}", subscriptions.dumpTree());
        }
        this.publisher.publish2Subscribers(evt, topicMatchingSubscriptions);

        if (evt.isRetained()) {
            if (!evt.getMessage().hasRemaining()) {
                m_messagesStore.cleanRetained(topic);
            } else {
                m_messagesStore.storeRetained(topic, evt.getGuid());
            }
        }

        sendPubComp(clientID, messageID);
    }

    private boolean checkWriteOnTopic(String topic, Channel channel) {
        String clientID = NettyUtils.clientID(channel);
        String username = NettyUtils.userName(channel);
        if (!m_authorizator.canWrite(topic, username, clientID)) {
            LOG.debug("topic {} doesn't have write credentials", topic);
            return true;
        }
        return false;
    }

    private void sendPubRec(String clientID, int messageID) {
        LOG.trace("PUB <--PUBREC-- SRV sendPubRec invoked for clientID {} with messageID {}", clientID, messageID);
        PubRecMessage pubRecMessage = new PubRecMessage();
        pubRecMessage.setMessageID(messageID);
        connectionDescriptors.get(clientID).channel.writeAndFlush(pubRecMessage);
    }

    private void sendPubComp(String clientID, int messageID) {
        LOG.debug("PUB <--PUBCOMP-- SRV sendPubComp invoked for clientID {} ad messageID {}", clientID, messageID);
        PubCompMessage pubCompMessage = new PubCompMessage();
        pubCompMessage.setMessageID(messageID);
        connectionDescriptors.get(clientID).channel.writeAndFlush(pubCompMessage);
    }
}
