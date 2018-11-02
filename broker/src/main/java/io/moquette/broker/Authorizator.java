package io.moquette.broker;

import io.moquette.spi.impl.subscriptions.Topic;
import io.moquette.spi.security.IAuthorizatorPolicy;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static io.moquette.spi.impl.Utils.messageId;
import static io.netty.handler.codec.mqtt.MqttQoS.FAILURE;

final class Authorizator {

    private static final Logger LOG = LoggerFactory.getLogger(Authorizator.class);

    private final IAuthorizatorPolicy policy;

    Authorizator(IAuthorizatorPolicy policy) {
        this.policy = policy;
    }

    /**
     * @param clientID
     *            the clientID
     * @param username
     *            the username
     * @param msg
     *            the subscribe message to verify
     * @return the list of verified topics for the given subscribe message.
     */
    List<MqttTopicSubscription> verifyTopicsReadAccess(String clientID, String username, MqttSubscribeMessage msg) {
        List<MqttTopicSubscription> ackTopics = new ArrayList<>();

        final int messageId = messageId(msg);
        for (MqttTopicSubscription req : msg.payload().topicSubscriptions()) {
            Topic topic = new Topic(req.topicName());
            if (!policy.canRead(topic, username, clientID)) {
                // send SUBACK with 0x80, the user hasn't credentials to read the topic
                LOG.warn("Client does not have read permissions on the topic CId={}, username: {}, messageId: {}, " +
                         "topic: {}", clientID, username, messageId, topic);
                ackTopics.add(new MqttTopicSubscription(topic.toString(), FAILURE));
            } else {
                MqttQoS qos;
                if (topic.isValid()) {
                    LOG.debug("Client will be subscribed to the topic CId={}, username: {}, messageId: {}, topic: {}",
                              clientID, username, messageId, topic);
                    qos = req.qualityOfService();
                } else {
                    LOG.warn("Topic filter is not valid CId={}, username: {}, messageId: {}, topic: {}", clientID,
                             username, messageId, topic);
                    qos = FAILURE;
                }
                ackTopics.add(new MqttTopicSubscription(topic.toString(), qos));
            }
        }
        return ackTopics;
    }

    /**
     * Ask the authorization policy if the topic can be used in a publish.
     *
     * @param topic
     *            the topic to write to.
     * @param user
     *            the user
     * @param client
     *            the client
     * @return true if the user from client can publish data on topic.
     */
    boolean canWrite(Topic topic, String user, String client) {
        return policy.canWrite(topic, user, client);
    }

}
