
package io.moquette.spi.impl;

import io.moquette.server.ConnectionDescriptorStore;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.ClientSession;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.MessageGUID;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.SubscriptionsStore;
import io.moquette.spi.impl.subscriptions.Topic;
import io.moquette.spi.security.IAuthorizator;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import static io.moquette.spi.impl.ProtocolProcessor.asStoredMessage;
import static io.moquette.spi.impl.Utils.messageId;
import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;

class Qos2PublishHandler extends QosPublishHandler {

    private static final Logger LOG = LoggerFactory.getLogger(Qos1PublishHandler.class);

    private final SubscriptionsStore subscriptions;
    private final IMessagesStore m_messagesStore;
    private final BrokerInterceptor m_interceptor;
    private final ConnectionDescriptorStore connectionDescriptors;
    private final ISessionsStore m_sessionsStore;
    private final MessagesPublisher publisher;

    public Qos2PublishHandler(IAuthorizator authorizator, SubscriptionsStore subscriptions,
            IMessagesStore messagesStore, BrokerInterceptor interceptor,
            ConnectionDescriptorStore connectionDescriptors, ISessionsStore sessionsStore, String brokerPort,
            MessagesPublisher messagesPublisher) {
        super(authorizator);
        this.subscriptions = subscriptions;
        this.m_messagesStore = messagesStore;
        this.m_interceptor = interceptor;
        this.connectionDescriptors = connectionDescriptors;
        this.m_sessionsStore = sessionsStore;
        this.publisher = messagesPublisher;
    }

    void receivedPublishQos2(Channel channel, MqttPublishMessage msg) {
        final Topic topic = new Topic(msg.variableHeader().topicName());
        // check if the topic can be wrote
        if (checkWriteOnTopic(topic, channel)) {
            return;
        }
        final int messageID = msg.variableHeader().messageId();

        IMessagesStore.StoredMessage toStoreMsg = asStoredMessage(msg);
        String clientID = NettyUtils.clientID(channel);
        toStoreMsg.setClientID(clientID);

        if (LOG.isTraceEnabled()) {
            LOG.trace(
                    "Sending publish message to subscribers. MqttClientId = {}, topic = {}, messageId = {}, "
                            + "payload = {}, subscriptionTree = {}.",
                    clientID,
                    topic,
                    messageID,
                    DebugUtils.payload2Str(toStoreMsg.getMessage()),
                    subscriptions.dumpTree());
        } else {
            LOG.info(
                    "Sending publish message to subscribers. MqttClientId = {}, topic = {}, messageId = {}.",
                    clientID,
                    topic,
                    messageID);
        }

        // QoS2
        MessageGUID guid = m_messagesStore.storePublishForFuture(toStoreMsg);
        // TODO Don't send PUBREC for Hz publish notification, if (msg.isLocal()) {
        sendPubRec(clientID, messageID);
        // }
        // Next the client will send us a pub rel
        // NB publish to subscribers for QoS 2 happen upon PUBREL from publisher

        if (msg.fixedHeader().isRetain()) {
            if (msg.payload().readableBytes() == 0) {
                m_messagesStore.cleanRetained(topic);
            } else {
                m_messagesStore.storeRetained(topic, guid);
            }
        }
        String username = NettyUtils.userName(channel);
        m_interceptor.notifyTopicPublished(msg, clientID, username);
    }

    /**
     * Second phase of a publish QoS2 protocol, sent by publisher to the broker. Search the stored
     * message and publish to all interested subscribers.
     */
    void processPubRel(Channel channel, MqttMessage msg) {
        String clientID = NettyUtils.clientID(channel);
        int messageID = messageId(msg);
        LOG.info("Processing PUBREL message. MqttClientId = {}, messageId = {}.", clientID, messageID);
        ClientSession targetSession = m_sessionsStore.sessionForClient(clientID);
        IMessagesStore.StoredMessage evt = targetSession.storedMessage(messageID);
        final Topic topic = new Topic(evt.getTopic());
        List<Subscription> topicMatchingSubscriptions = subscriptions.matches(topic);

        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "Sending publish message to subscribers. "
                    + "MqttClientId = {}, topic = {}, messageId = {}, payload = {}, subscriptionTree = {}.",
                    clientID,
                    topic,
                    messageID,
                    DebugUtils.payload2Str(evt.getMessage()),
                    subscriptions.dumpTree());
        } else {
            LOG.info(
                    "Sending publish message to subscribers. MqttClientId = {}, topic = {}, messageId = {}.",
                    clientID,
                    topic,
                    messageID);
        }
        this.publisher.publish2Subscribers(evt, topicMatchingSubscriptions);

        if (evt.isRetained()) {
            if (evt.getMessage().readableBytes() == 0) {
                m_messagesStore.cleanRetained(topic);
            } else {
                m_messagesStore.storeRetained(topic, evt.getGuid());
            }
        }

        sendPubComp(clientID, messageID);
    }

    private void sendPubRec(String clientID, int messageID) {
        LOG.debug("Sending PUBREC message. MqttClientId = {}, messageId = {}.", clientID, messageID);
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBREC, false, AT_MOST_ONCE, false, 0);
        MqttMessage pubRecMessage = new MqttMessage(fixedHeader, from(messageID));
        connectionDescriptors.sendMessage(pubRecMessage, messageID, clientID);
    }

    private void sendPubComp(String clientID, int messageID) {
        LOG.debug("Sending PUBCOMP message. MqttClientId = {}, messageId = {}.", clientID, messageID);
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBCOMP, false, AT_MOST_ONCE, false, 0);
        MqttMessage pubCompMessage = new MqttMessage(fixedHeader, from(messageID));
        connectionDescriptors.sendMessage(pubCompMessage, messageID, clientID);
    }
}
