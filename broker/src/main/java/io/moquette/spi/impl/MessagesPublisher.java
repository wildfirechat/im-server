package io.moquette.spi.impl;

import io.moquette.parser.proto.messages.AbstractMessage;
import io.moquette.parser.proto.messages.PublishMessage;
import io.moquette.server.ConnectionDescriptorStore;
import io.moquette.spi.ClientSession;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.MessageGUID;
import io.moquette.spi.impl.subscriptions.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;

import static io.moquette.spi.impl.ProtocolProcessor.lowerQosToTheSubscriptionDesired;

class MessagesPublisher {

    private static final Logger LOG = LoggerFactory.getLogger(MessagesPublisher.class);
    private final ConnectionDescriptorStore connectionDescriptors;
    private final ISessionsStore m_sessionsStore;
    private final IMessagesStore m_messagesStore;
    private final PersistentQueueMessageSender messageSender;

    public MessagesPublisher(ConnectionDescriptorStore connectionDescriptors, ISessionsStore sessionsStore,
                             IMessagesStore messagesStore, PersistentQueueMessageSender messageSender) {
        this.connectionDescriptors = connectionDescriptors;
        this.m_sessionsStore = sessionsStore;
        this.m_messagesStore = messagesStore;
        this.messageSender = messageSender;
    }

    static PublishMessage notRetainedPublish(String topic, AbstractMessage.QOSType qos, ByteBuffer message) {
        PublishMessage pubMessage = new PublishMessage();
        pubMessage.setRetainFlag(false);
        pubMessage.setTopicName(topic);
        pubMessage.setQos(qos);
        pubMessage.setPayload(message);
        return pubMessage;
    }

    void publish2Subscribers(IMessagesStore.StoredMessage pubMsg, List<Subscription> topicMatchingSubscriptions) {
        final String topic = pubMsg.getTopic();
        final AbstractMessage.QOSType publishingQos = pubMsg.getQos();
        final ByteBuffer origMessage = pubMsg.getMessage();

        //if QoS 1 or 2 store the message
        //TODO perhaps this block is not needed.
        MessageGUID guid = null;
        if (publishingQos != AbstractMessage.QOSType.MOST_ONE) {
            guid = m_messagesStore.storePublishForFuture(pubMsg);
        }
        
        for (final Subscription sub : topicMatchingSubscriptions) {
            AbstractMessage.QOSType qos = lowerQosToTheSubscriptionDesired(sub, publishingQos);
            ClientSession targetSession = m_sessionsStore.sessionForClient(sub.getClientId());

            boolean targetIsActive = this.connectionDescriptors.isConnected(sub.getClientId());

            ByteBuffer message = origMessage.duplicate();
            if (targetIsActive) {
                LOG.debug("Sending PUBLISH message to active subscriber. MessageId = {}, mqttClientId = {}, topicFilter = {}, " +
                        "qos = {}.", pubMsg.getMessageID(), sub.getClientId(), sub.getTopicFilter(), qos);
                PublishMessage publishMsg = notRetainedPublish(topic, qos, message);
                if (qos != AbstractMessage.QOSType.MOST_ONE) {
                    //QoS 1 or 2
                    int messageId = targetSession.nextPacketId();
                    targetSession.inFlightAckWaiting(guid, messageId);
                    //set the PacketIdentifier only for QoS > 0
                    publishMsg.setMessageID(messageId);
                }
                this.messageSender.sendPublish(targetSession, publishMsg);
            } else {
                if (!targetSession.isCleanSession()) {
                    LOG.debug("Storing pending PUBLISH inactive message. MessageId = {}, mqttClientId = {}, " +
                            "topicFilter = {}, qos = {}.",
                            pubMsg.getMessageID(), sub.getClientId(), sub.getTopicFilter(), qos);
                    //store the message in targetSession queue to deliver
                    targetSession.enqueue(pubMsg);
                }
            }
        }
    }
}
