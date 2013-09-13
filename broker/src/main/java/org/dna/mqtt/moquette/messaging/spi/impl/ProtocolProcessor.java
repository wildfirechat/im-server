package org.dna.mqtt.moquette.messaging.spi.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.dna.mqtt.moquette.messaging.spi.IMatchingCondition;
import org.dna.mqtt.moquette.messaging.spi.IStorageService;
import org.dna.mqtt.moquette.messaging.spi.impl.events.PubAckEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.events.PublishEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.Subscription;
import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.SubscriptionsStore;
import org.dna.mqtt.moquette.proto.messages.PubCompMessage;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.dna.mqtt.moquette.proto.messages.PubAckMessage;
import org.dna.mqtt.moquette.proto.messages.PubRecMessage;
import org.dna.mqtt.moquette.proto.messages.PubRelMessage;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;
import org.dna.mqtt.moquette.proto.messages.SubAckMessage;
import org.dna.mqtt.moquette.proto.messages.SubscribeMessage;
import org.dna.mqtt.moquette.proto.messages.UnsubAckMessage;
import org.dna.mqtt.moquette.server.ConnectionDescriptor;
import org.dna.mqtt.moquette.server.Constants;
import org.dna.mqtt.moquette.server.IAuthenticator;
import org.dna.mqtt.moquette.server.ServerChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class responsible to handle the logic of MQTT protocol it's the director of
 * the protocol execution. 
 * 
 * Used by the front facing class SimpleMessaging.
 * 
 * @author andrea
 */
class ProtocolProcessor {
    
    private static final Logger LOG = LoggerFactory.getLogger(ProtocolProcessor.class);
    
    private Map<String, ConnectionDescriptor> m_clientIDs;
    private SubscriptionsStore subscriptions;
    private IStorageService m_storageService;
    private IAuthenticator m_authenticator;

    ProtocolProcessor() {
        
    }
    
    void init(Map<String, ConnectionDescriptor> clientIDs, SubscriptionsStore subscriptions, 
            IStorageService storageService) {
        m_clientIDs = clientIDs;
        this.subscriptions = subscriptions;
        m_storageService = storageService;
    }
    
    void processConnect(ServerChannel session, ConnectMessage msg) {
        if (msg.getProcotolVersion() != 0x03) {
            ConnAckMessage badProto = new ConnAckMessage();
            badProto.setReturnCode(ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION);
            session.write(badProto);
            session.close(false);
            return;
        }

        if (msg.getClientID() == null || msg.getClientID().length() > 23) {
            ConnAckMessage okResp = new ConnAckMessage();
            okResp.setReturnCode(ConnAckMessage.IDENTIFIER_REJECTED);
            session.write(okResp);
            return;
        }

        //if an old client with the same ID already exists close its session.
        if (m_clientIDs.containsKey(msg.getClientID())) {
            //clean the subscriptions if the old used a cleanSession = true
            ServerChannel oldSession = m_clientIDs.get(msg.getClientID()).getSession();
            boolean cleanSession = (Boolean) oldSession.getAttribute(Constants.CLEAN_SESSION);
            if (cleanSession) {
                //cleanup topic subscriptions
                processRemoveAllSubscriptions(msg.getClientID());
            }

            m_clientIDs.get(msg.getClientID()).getSession().close(false);
        }

        ConnectionDescriptor connDescr = new ConnectionDescriptor(msg.getClientID(), session, msg.isCleanSession());
        m_clientIDs.put(msg.getClientID(), connDescr);

        int keepAlive = msg.getKeepAlive();
        LOG.debug(String.format("Connect with keepAlive %d s",  keepAlive));
        session.setAttribute("keepAlive", keepAlive);
        session.setAttribute(Constants.CLEAN_SESSION, msg.isCleanSession());
        //used to track the client in the subscription and publishing phases.
        session.setAttribute(Constants.ATTR_CLIENTID, msg.getClientID());

        session.setIdleTime(Math.round(keepAlive * 1.5f));

        //Handle will flag
        if (msg.isWillFlag()) {
            AbstractMessage.QOSType willQos = AbstractMessage.QOSType.values()[msg.getWillQos()];
            PublishEvent pubEvt = new PublishEvent(msg.getWillTopic(), willQos, msg.getWillMessage().getBytes(),
                    msg.isWillRetain(), msg.getClientID(), session);
            processPublish(pubEvt);
        }

        //handle user authentication
        if (msg.isUserFlag()) {
            String pwd = null;
            if (msg.isPasswordFlag()) {
                pwd = msg.getPassword();
            }
            if (!m_authenticator.checkValid(msg.getUsername(), pwd)) {
                ConnAckMessage okResp = new ConnAckMessage();
                okResp.setReturnCode(ConnAckMessage.BAD_USERNAME_OR_PASSWORD);
                session.write(okResp);
                return;
            }
        }

        subscriptions.activate(msg.getClientID());

        //handle clean session flag
        if (msg.isCleanSession()) {
            //remove all prev subscriptions
            //cleanup topic subscriptions
            processRemoveAllSubscriptions(msg.getClientID());
        }

        ConnAckMessage okResp = new ConnAckMessage();
        okResp.setReturnCode(ConnAckMessage.CONNECTION_ACCEPTED);
        session.write(okResp);
        
        if (!msg.isCleanSession()) {
            //force the republish of stored QoS1 and QoS2
            republishStored(msg.getClientID());
        }
    }
    
    private void republishStored(String clientID) {
        LOG.debug("republishStored invoked");
        List<PublishEvent> publishedEvents = m_storageService.retrivePersistedPublishes(clientID);
        if (publishedEvents == null) {
            LOG.debug("republishStored, no stored publish events");
            return;
        }

        for (PublishEvent pubEvt : publishedEvents) {
            sendPublish(pubEvt.getClientID(), pubEvt.getTopic(), pubEvt.getQos(),
                   pubEvt.getMessage(), false, pubEvt.getMessageID());
        }
    }
    
    private void processRemoveAllSubscriptions(String clientID) {
        LOG.debug("processRemoveAllSubscriptions invoked");
        subscriptions.removeForClient(clientID);

        //remove also the messages stored of type QoS1/2
        m_storageService.cleanPersistedPublishes(clientID);
    }
    
    protected void processPublish(PublishEvent evt) {
        LOG.debug("processPublish invoked with " + evt);
        final String topic = evt.getTopic();
        final AbstractMessage.QOSType qos = evt.getQos();
        final byte[] message = evt.getMessage();
        boolean retain = evt.isRetain();

        String publishKey = null;
        if (qos == AbstractMessage.QOSType.LEAST_ONE) {
            //store the temporary message
            publishKey = String.format("%s%d", evt.getClientID(), evt.getMessageID());
            m_storageService.addInFlight(evt, publishKey);
        }  else if (qos == AbstractMessage.QOSType.EXACTLY_ONCE) {
            publishKey = String.format("%s%d", evt.getClientID(), evt.getMessageID());
            //store the message in temp store
            m_storageService.persistQoS2Message(publishKey, evt);
            sendPubRec(evt.getClientID(), evt.getMessageID());
        }

        publish2Subscribers(topic, qos, message, retain, evt.getMessageID());

        if (qos == AbstractMessage.QOSType.LEAST_ONE) {
            if (publishKey == null) {
                throw new RuntimeException("Found a publish key null for QoS " + qos + " for message " + evt);
            }
            m_storageService.cleanInFlight(publishKey);
            sendPubAck(new PubAckEvent(evt.getMessageID(), evt.getClientID()));
        }

        if (retain) {
            m_storageService.storeRetained(topic, message, qos);
        }
    }
    
    /**
     * Flood the subscribers with the message to notify. MessageID is optional and should only used for QoS 1 and 2
     * */
    private void publish2Subscribers(String topic, AbstractMessage.QOSType qos, byte[] message, boolean retain, Integer messageID) {
        for (final Subscription sub : subscriptions.matches(topic)) {
            if (qos == AbstractMessage.QOSType.MOST_ONE) {
                //QoS 0
                sendPublish(sub.getClientId(), topic, qos, message, false);
            } else {
                //QoS 1 or 2
                //if the target subscription is not clean session and is not connected => store it
                if (!sub.isCleanSession() && !sub.isActive()) {
                    //clone the event with matching clientID
                    PublishEvent newPublishEvt = new PublishEvent(topic, qos, message, retain, sub.getClientId(), messageID, null);
                    m_storageService.storePublishForFuture(newPublishEvt);
                } else  {
                    //if QoS 2 then store it in temp memory
                    if (qos ==AbstractMessage.QOSType.EXACTLY_ONCE) {
                        String publishKey = String.format("%s%d", sub.getClientId(), messageID);
                        PublishEvent newPublishEvt = new PublishEvent(topic, qos, message, retain, sub.getClientId(), messageID, null);
                        m_storageService.addInFlight(newPublishEvt, publishKey);
                    }
                    //publish
                    sendPublish(sub.getClientId(), topic, qos, message, false);
                }
            }
        }
    }
    
    private void sendPublish(String clientId, String topic, AbstractMessage.QOSType qos, byte[] message, boolean retained) {
        sendPublish(clientId, topic, qos, message, retained, 0);
    }
    
    private void sendPublish(String clientId, String topic, AbstractMessage.QOSType qos, byte[] message, boolean retained, int messageID) {
        LOG.debug("notify invoked with event ");
        PublishMessage pubMessage = new PublishMessage();
        pubMessage.setRetainFlag(retained);
        pubMessage.setTopicName(topic);
        pubMessage.setQos(qos);
        pubMessage.setPayload(message);
        if (pubMessage.getQos() != AbstractMessage.QOSType.MOST_ONE) {
            pubMessage.setMessageID(messageID);
        }

        try {
            if (m_clientIDs == null) {
                throw new RuntimeException("Internal bad error, found m_clientIDs to null while it should be initialized, somewhere it's overwritten!!");
            }
            LOG.debug("clientIDs are " + m_clientIDs);
            if (m_clientIDs.get(clientId) == null) {
                throw new RuntimeException(String.format("Can't find a ConnectionDEwcriptor for client %s in cache %s", clientId, m_clientIDs));
            }
            LOG.debug("Session for clientId " + clientId + " is " + m_clientIDs.get(clientId).getSession());
            m_clientIDs.get(clientId).getSession().write(pubMessage);
        }catch(Throwable t) {
            LOG.error(null, t);
        }
    }
    
    private void sendPubRec(String clientID, int messageID) {
        LOG.debug(String.format("sendPubRec invoked for clientID %s ad messageID %d", clientID, messageID));
        PubRecMessage pubRecMessage = new PubRecMessage();
        pubRecMessage.setMessageID(messageID);

        m_clientIDs.get(clientID).getSession().write(pubRecMessage);
    }
    
    private void sendPubAck(PubAckEvent evt) {
        LOG.debug("sendPubAck invoked");

        String clientId = evt.getClientID();

        PubAckMessage pubAckMessage = new PubAckMessage();
        pubAckMessage.setMessageID(evt.getMessageId());

        try {
            if (m_clientIDs == null) {
                throw new RuntimeException("Internal bad error, found m_clientIDs to null while it should be initialized, somewhere it's overwritten!!");
            }
            LOG.debug("clientIDs are " + m_clientIDs);
           if (m_clientIDs.get(clientId) == null) {
                throw new RuntimeException(String.format("Can't find a ConnectionDEwcriptor for client %s in cache %s", clientId, m_clientIDs));
            }
            LOG.debug("Session for clientId " + clientId + " is " + m_clientIDs.get(clientId).getSession());
            m_clientIDs.get(clientId).getSession().write(pubAckMessage);
        }catch(Throwable t) {
            LOG.error(null, t);
        }
    }
    
    /**
     * Second phase of a publish QoS2 protocol, sent by publisher to the broker. Search the stored message and publish
     * to all interested subscribers.
     * */
    void processPubRel(String clientID, int messageID) {
        String publishKey = String.format("%s%d", clientID, messageID);
        PublishEvent evt = m_storageService.retrieveQoS2Message(publishKey);

        final String topic = evt.getTopic();
        final AbstractMessage.QOSType qos = evt.getQos();
        final byte[] message = evt.getMessage();
        boolean retain = evt.isRetain();

        publish2Subscribers(topic, qos, message, retain, evt.getMessageID());

        m_storageService.removeQoS2Message(publishKey);

        if (retain) {
            m_storageService.storeRetained(topic, message, qos);
        }

        sendPubComp(clientID, messageID);
    }
    
    private void sendPubComp(String clientID, int messageID) {
        LOG.debug(String.format("sendPubComp invoked for clientID %s ad messageID %d", clientID, messageID));
        PubCompMessage pubCompMessage = new PubCompMessage();
        pubCompMessage.setMessageID(messageID);

        m_clientIDs.get(clientID).getSession().write(pubCompMessage);
    }
    
    void processPubRec(String clientID, int messageID) {
        //once received a PUBREC reply with a PUBREL(messageID)
        LOG.debug(String.format("processPubRec invoked for clientID %s ad messageID %d", clientID, messageID));
        PubRelMessage pubRelMessage = new PubRelMessage();
        pubRelMessage.setMessageID(messageID);

        m_clientIDs.get(clientID).getSession().write(pubRelMessage);
    }
    
    void processPubComp(String clientID, int messageID) {
        //once received the PUBCOMP then remove the message from the temp memory
        String publishKey = String.format("%s%d", clientID, messageID);
        m_storageService.cleanInFlight(publishKey);
    }
    
    void processDisconnect(ServerChannel session, String clientID, boolean cleanSession) throws InterruptedException {
        if (cleanSession) {
            //cleanup topic subscriptions
            processRemoveAllSubscriptions(clientID);
        }
//        m_notifier.disconnect(evt.getSession());
        m_clientIDs.remove(clientID);
        session.close(true);

        //de-activate the subscriptions for this ClientID
//        String clientID = (String) evt.getSession().getAttribute(Constants.ATTR_CLIENTID);
        subscriptions.deactivate(clientID);
    }
    
    /**
     * Remove the clientID from topic subscription, if not previously subscribed,
     * doesn't reply any error
     */
    void processUnsubscribe(ServerChannel session, String clientID, List<String> topics, int messageID) {
        LOG.debug("processSubscribe invoked");

        for (String topic : topics) {
            subscriptions.removeSubscription(topic, clientID);
        }
        //ack the client
        UnsubAckMessage ackMessage = new UnsubAckMessage();
        ackMessage.setMessageID(messageID);

        LOG.info("replying with UnsubAck to MSG ID {0}", messageID);
        session.write(ackMessage);
    }
    
    
    void processSubscribe(ServerChannel session, SubscribeMessage msg, String clientID, boolean cleanSession) {
        LOG.debug("processSubscribe invoked");

        for (SubscribeMessage.Couple req : msg.subscriptions()) {
            AbstractMessage.QOSType qos = AbstractMessage.QOSType.values()[req.getQos()];
            Subscription newSubscription = new Subscription(clientID, req.getTopic(), qos, cleanSession);
            subscribeSingleTopic(newSubscription, req.getTopic());
        }

        //ack the client
        SubAckMessage ackMessage = new SubAckMessage();
        ackMessage.setMessageID(msg.getMessageID());

        //TODO by now it handles only QoS 0 messages
        for (int i = 0; i < msg.subscriptions().size(); i++) {
            ackMessage.addType(AbstractMessage.QOSType.MOST_ONE);
        }
        LOG.info("replying with SubAct to MSG ID " + msg.getMessageID());
        session.write(ackMessage);
    }
    
    private void subscribeSingleTopic(Subscription newSubscription, final String topic) {
        subscriptions.add(newSubscription);

        //scans retained messages to be published to the new subscription
        Collection<HawtDBStorageService.StoredMessage> messages = m_storageService.searchMatching(new IMatchingCondition() {
            public boolean match(String key) {
                return  SubscriptionsStore.matchTopics(key, topic);
            }
        });

        for (HawtDBStorageService.StoredMessage storedMsg : messages) {
            //fire the as retained the message
            LOG.debug("send publish message for topic " + topic);
            sendPublish(newSubscription.getClientId(), storedMsg.getTopic(), storedMsg.getQos(), storedMsg.getPayload(), true);
        }
    }

}
