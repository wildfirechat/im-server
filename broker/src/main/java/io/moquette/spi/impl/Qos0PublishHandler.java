
package io.moquette.spi.impl;

import io.moquette.server.netty.NettyUtils;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.SubscriptionsStore;
import io.moquette.spi.impl.subscriptions.Topic;
import io.moquette.spi.security.IAuthorizator;
import io.netty.channel.Channel;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import static io.moquette.spi.impl.ProtocolProcessor.asStoredMessage;

class Qos0PublishHandler extends QosPublishHandler {

    private static final Logger LOG = LoggerFactory.getLogger(Qos0PublishHandler.class);

    private final SubscriptionsStore subscriptions;
    private final IMessagesStore m_messagesStore;
    private final BrokerInterceptor m_interceptor;
    private final MessagesPublisher publisher;

    public Qos0PublishHandler(IAuthorizator authorizator, SubscriptionsStore subscriptions,
            IMessagesStore messagesStore, BrokerInterceptor interceptor, MessagesPublisher messagesPublisher) {
        super(authorizator);
        this.subscriptions = subscriptions;
        this.m_messagesStore = messagesStore;
        this.m_interceptor = interceptor;
        this.publisher = messagesPublisher;
    }

    void receivedPublishQos0(Channel channel, MqttPublishMessage msg) {
        // verify if topic can be write
        final Topic topic = new Topic(msg.variableHeader().topicName());
        if (checkWriteOnTopic(topic, channel)) {
            return;
        }

        // route message to subscribers
        IMessagesStore.StoredMessage toStoreMsg = asStoredMessage(msg);
        String clientID = NettyUtils.clientID(channel);
        toStoreMsg.setClientID(clientID);

        final int messageID = msg.variableHeader().messageId();

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

        if (msg.fixedHeader().isRetain()) {
            // QoS == 0 && retain => clean old retained
            m_messagesStore.cleanRetained(topic);
        }

        String username = NettyUtils.userName(channel);
        m_interceptor.notifyTopicPublished(msg, clientID, username);
    }
}
