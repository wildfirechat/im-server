
package io.moquette.spi.impl;

import io.moquette.server.ConnectionDescriptorStore;
import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.MessageGUID;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.SubscriptionsStore;
import io.moquette.spi.impl.subscriptions.Topic;
import io.moquette.spi.security.IAuthorizator;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import static io.moquette.spi.impl.ProtocolProcessor.asStoredMessage;
import static io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader.from;
import static io.netty.handler.codec.mqtt.MqttQoS.AT_MOST_ONCE;

class Qos1PublishHandler extends QosPublishHandler {

    private static final Logger LOG = LoggerFactory.getLogger(Qos1PublishHandler.class);

    private final SubscriptionsStore subscriptions;
    private final IMessagesStore m_messagesStore;
    private final BrokerInterceptor m_interceptor;
    private final ConnectionDescriptorStore connectionDescriptors;
    private final MessagesPublisher publisher;

    public Qos1PublishHandler(IAuthorizator authorizator, SubscriptionsStore subscriptions,
            IMessagesStore messagesStore, BrokerInterceptor interceptor,
            ConnectionDescriptorStore connectionDescriptors, String brokerPort, MessagesPublisher messagesPublisher) {
        super(authorizator);
        this.subscriptions = subscriptions;
        this.m_messagesStore = messagesStore;
        this.m_interceptor = interceptor;
        this.connectionDescriptors = connectionDescriptors;
        this.publisher = messagesPublisher;
    }

    void receivedPublishQos1(Channel channel, MqttPublishMessage msg) {
        // verify if topic can be write
        final Topic topic = new Topic(msg.variableHeader().topicName());
        if (checkWriteOnTopic(topic, channel)) {
            return;
        }

        final int messageID = msg.variableHeader().messageId();

        // route message to subscribers
        IMessagesStore.StoredMessage toStoreMsg = asStoredMessage(msg);
        String clientID = NettyUtils.clientID(channel);
        toStoreMsg.setClientID(clientID);

        if (LOG.isTraceEnabled()) {
            LOG.trace(
                    "Sending publish message to subscribers. "
                    + "MqttClientId = {}, topic = {}, messageId = {}, payload = {}, subscriptionTree = {}.",
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

        List<Subscription> topicMatchingSubscriptions = subscriptions.matches(topic);
        this.publisher.publish2Subscribers(toStoreMsg, topicMatchingSubscriptions);

        // send PUBACK
        // TODO Don't send PUBREC for Hz publish notification, if (msg.isLocal()) {
        sendPubAck(clientID, messageID);
        // }

        if (msg.fixedHeader().isRetain()) {
            if (!msg.payload().isReadable()) {
                m_messagesStore.cleanRetained(topic);
            } else {
                // before wasn't stored
                MessageGUID guid = m_messagesStore.storePublishForFuture(toStoreMsg);
                m_messagesStore.storeRetained(topic, guid);
            }
        }

        String username = NettyUtils.userName(channel);
        m_interceptor.notifyTopicPublished(msg, clientID, username);
    }

    private void sendPubAck(String clientId, int messageID) {
        LOG.trace("sendPubAck invoked");
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PUBACK, false, AT_MOST_ONCE, false, 0);
        MqttPubAckMessage pubAckMessage = new MqttPubAckMessage(fixedHeader, from(messageID));

        try {
            if (connectionDescriptors == null) {
                throw new RuntimeException(
                        "Internal bad error, found connectionDescriptors to null while it should be initialized, "
                        + "somewhere it's overwritten!!");
            }
            LOG.debug("clientIDs are {}", connectionDescriptors);
            if (!connectionDescriptors.isConnected(clientId)) {
                throw new RuntimeException(
                        String.format(
                                "Can't find a ConnectionDescriptor for client %s in cache %s",
                                clientId,
                                connectionDescriptors));
            }
            connectionDescriptors.sendMessage(pubAckMessage, messageID, clientId);
        } catch (Throwable t) {
            LOG.error(null, t);
        }
    }

}
