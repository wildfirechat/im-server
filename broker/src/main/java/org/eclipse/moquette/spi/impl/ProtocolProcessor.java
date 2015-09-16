/*
 * Copyright (c) 2012-2015 The original author or authors
 * ------------------------------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * The Apache License v2.0 is available at
 * http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */
package org.eclipse.moquette.spi.impl;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.moquette.server.netty.NettyChannel;
import org.eclipse.moquette.spi.IMatchingCondition;
import org.eclipse.moquette.spi.IMessagesStore;
import org.eclipse.moquette.spi.ISessionsStore;
import org.eclipse.moquette.spi.impl.events.*;
import org.eclipse.moquette.spi.impl.security.IAuthorizator;
import org.eclipse.moquette.spi.impl.subscriptions.Subscription;
import org.eclipse.moquette.spi.impl.subscriptions.SubscriptionsStore;
import static org.eclipse.moquette.parser.netty.Utils.VERSION_3_1;
import static org.eclipse.moquette.parser.netty.Utils.VERSION_3_1_1;
import org.eclipse.moquette.proto.messages.AbstractMessage;
import org.eclipse.moquette.proto.messages.AbstractMessage.QOSType;
import org.eclipse.moquette.proto.messages.ConnAckMessage;
import org.eclipse.moquette.proto.messages.ConnectMessage;
import org.eclipse.moquette.proto.messages.DisconnectMessage;
import org.eclipse.moquette.proto.messages.PubAckMessage;
import org.eclipse.moquette.proto.messages.PubCompMessage;
import org.eclipse.moquette.proto.messages.PubRecMessage;
import org.eclipse.moquette.proto.messages.PubRelMessage;
import org.eclipse.moquette.proto.messages.PublishMessage;
import org.eclipse.moquette.proto.messages.SubAckMessage;
import org.eclipse.moquette.proto.messages.SubscribeMessage;
import org.eclipse.moquette.proto.messages.UnsubAckMessage;
import org.eclipse.moquette.proto.messages.UnsubscribeMessage;
import org.eclipse.moquette.server.ConnectionDescriptor;
import org.eclipse.moquette.spi.impl.security.IAuthenticator;
import org.eclipse.moquette.server.ServerChannel;
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
class ProtocolProcessor implements EventHandler<ValueEvent> {

    static final class WillMessage {
        private final String topic;
        private final ByteBuffer payload;
        private final boolean retained;
        private final QOSType qos;

        public WillMessage(String topic, ByteBuffer payload, boolean retained, QOSType qos) {
            this.topic = topic;
            this.payload = payload;
            this.retained = retained;
            this.qos = qos;
        }

        public String getTopic() {
            return topic;
        }

        public ByteBuffer getPayload() {
            return payload;
        }

        public boolean isRetained() {
            return retained;
        }

        public QOSType getQos() {
            return qos;
        }
        
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(ProtocolProcessor.class);
    
    private Map<String, ConnectionDescriptor> m_clientIDs = new HashMap<>();
    private SubscriptionsStore subscriptions;
    private boolean allowAnonymous;
    private IAuthorizator m_authorizator;
    private IMessagesStore m_messagesStore;
    private ISessionsStore m_sessionsStore;
    private IAuthenticator m_authenticator;
    private BrokerInterceptor m_interceptor;

    //maps clientID to Will testament, if specified on CONNECT
    private Map<String, WillMessage> m_willStore = new HashMap<>();
    
    private ExecutorService m_executor;
    private RingBuffer<ValueEvent> m_ringBuffer;
    private Disruptor<ValueEvent> m_disruptor;

    ProtocolProcessor() {}

    /**
     * @param subscriptions the subscription store where are stored all the existing
     *  clients subscriptions.
     * @param storageService the persistent store to use for save/load of messages
     *  for QoS1 and QoS2 handling.
     * @param sessionsStore the clients sessions store, used to persist subscriptions.
     * @param authenticator the authenticator used in connect messages.
     * @param allowAnonymous true connection to clients without credentials.
     * @param authorizator used to apply ACL policies to publishes and subscriptions.
     * @param interceptor to notify events to an intercept handler
     */
    void init(SubscriptionsStore subscriptions, IMessagesStore storageService,
              ISessionsStore sessionsStore,
              IAuthenticator authenticator,
              boolean allowAnonymous, IAuthorizator authorizator, BrokerInterceptor interceptor) {
        //m_clientIDs = clientIDs;
        this.m_interceptor = interceptor;
        this.subscriptions = subscriptions;
        this.allowAnonymous = allowAnonymous;
        m_authorizator = authorizator;
        LOG.debug("subscription tree on init {}", subscriptions.dumpTree());
        m_authenticator = authenticator;
        m_messagesStore = storageService;
        m_sessionsStore = sessionsStore;
        
        //init the output ringbuffer
        m_executor = Executors.newFixedThreadPool(1);

        /*Disruptor<ValueEvent>*/ m_disruptor = new Disruptor<>(ValueEvent.EVENT_FACTORY, 1024 * 32, m_executor);
        m_disruptor.handleEventsWith(this);
        m_disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        m_ringBuffer = m_disruptor.getRingBuffer();
    }

    void stop() {
        m_executor.shutdown();
        m_disruptor.shutdown();
    }

    @MQTTMessage(message = ConnectMessage.class)
    void processConnect(ServerChannel session, ConnectMessage msg) {
        LOG.debug("CONNECT for client <{}>", msg.getClientID());
        if (msg.getProtocolVersion() != VERSION_3_1 && msg.getProtocolVersion() != VERSION_3_1_1) {
            ConnAckMessage badProto = new ConnAckMessage();
            badProto.setReturnCode(ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION);
            LOG.warn("processConnect sent bad proto ConnAck");
            session.write(badProto);
            session.close(false);
            return;
        }

        if (msg.getClientID() == null || msg.getClientID().length() == 0) {
            ConnAckMessage okResp = new ConnAckMessage();
            okResp.setReturnCode(ConnAckMessage.IDENTIFIER_REJECTED);
            session.write(okResp);
            m_interceptor.notifyClientConnected(msg);
            return;
        }

        //handle user authentication
        if (msg.isUserFlag()) {
            byte[] pwd = null;
            if (msg.isPasswordFlag()) {
                pwd = msg.getPassword();
            } else if (!this.allowAnonymous) {
                failedCredentials(session);
                return;
            }
            if (!m_authenticator.checkValid(msg.getUsername(), pwd)) {
                failedCredentials(session);
                return;
            }
            session.setAttribute(NettyChannel.ATTR_KEY_USERNAME, msg.getUsername());
        } else if (!this.allowAnonymous) {
            failedCredentials(session);
            return;
        }

        //if an old client with the same ID already exists close its session.
        if (m_clientIDs.containsKey(msg.getClientID())) {
            LOG.info("Found an existing connection with same client ID <{}>, forcing to close", msg.getClientID());
            //clean the subscriptions if the old used a cleanSession = true
            ServerChannel oldSession = m_clientIDs.get(msg.getClientID()).getSession();
            boolean cleanSession = (Boolean) oldSession.getAttribute(NettyChannel.ATTR_KEY_CLEANSESSION);
            if (cleanSession) {
                //cleanup topic subscriptions
                cleanSession(msg.getClientID());
            }

            oldSession.close(false);
            LOG.debug("Existing connection with same client ID <{}>, forced to close", msg.getClientID());
        }

        ConnectionDescriptor connDescr = new ConnectionDescriptor(msg.getClientID(), session, msg.isCleanSession());
        m_clientIDs.put(msg.getClientID(), connDescr);

        int keepAlive = msg.getKeepAlive();
        LOG.debug("Connect with keepAlive {} s",  keepAlive);
        session.setAttribute(NettyChannel.ATTR_KEY_KEEPALIVE, keepAlive);
        session.setAttribute(NettyChannel.ATTR_KEY_CLEANSESSION, msg.isCleanSession());
        //used to track the client in the subscription and publishing phases.
        session.setAttribute(NettyChannel.ATTR_KEY_CLIENTID, msg.getClientID());
        LOG.debug("Connect create session <{}>", session);

        session.setIdleTime(Math.round(keepAlive * 1.5f));

        //Handle will flag
        if (msg.isWillFlag()) {
            AbstractMessage.QOSType willQos = AbstractMessage.QOSType.values()[msg.getWillQos()];
            byte[] willPayload = msg.getWillMessage();
            ByteBuffer bb = (ByteBuffer) ByteBuffer.allocate(willPayload.length).put(willPayload).flip();
            //save the will testament in the clientID store
            WillMessage will = new WillMessage(msg.getWillTopic(), bb, msg.isWillRetain(),willQos );
            m_willStore.put(msg.getClientID(), will);
        }

        subscriptions.activate(msg.getClientID());

        //handle clean session flag
        if (msg.isCleanSession()) {
            //remove all prev subscriptions
            //cleanup topic subscriptions
            cleanSession(msg.getClientID());
        }

        ConnAckMessage okResp = new ConnAckMessage();
        okResp.setReturnCode(ConnAckMessage.CONNECTION_ACCEPTED);
        if (!msg.isCleanSession() && m_sessionsStore.contains(msg.getClientID())) {
            okResp.setSessionPresent(true);
        }
        session.write(okResp);
        m_interceptor.notifyClientConnected(msg);

        LOG.info("Create persistent session for clientID <{}>", msg.getClientID());
        m_sessionsStore.addNewSubscription(Subscription.createEmptySubscription(msg.getClientID(), true)); //null means EmptySubscription
        LOG.info("Connected client ID <{}> with clean session {}", msg.getClientID(), msg.isCleanSession());
        if (!msg.isCleanSession()) {
            //force the republish of stored QoS1 and QoS2
            republishStoredInSession(msg.getClientID());
        }
    }

    private void failedCredentials(ServerChannel session) {
        ConnAckMessage okResp = new ConnAckMessage();
        okResp.setReturnCode(ConnAckMessage.BAD_USERNAME_OR_PASSWORD);
        session.write(okResp);
        session.close(false);
    }

    /**
     * Republish QoS1 and QoS2 messages stored into the session for the clientID.
     * */
    private void republishStoredInSession(String clientID) {
        LOG.trace("republishStoredInSession for client <{}>", clientID);
        List<PublishEvent> publishedEvents = m_messagesStore.listMessagesInSession(clientID);
        if (publishedEvents.isEmpty()) {
            LOG.info("No stored messages for client <{}>", clientID);
            return;
        }

        LOG.info("republishing stored messages to client <{}>", clientID);
        for (PublishEvent pubEvt : publishedEvents) {
            sendPublish(pubEvt.getClientID(), pubEvt.getTopic(), pubEvt.getQos(),
                   pubEvt.getMessage(), false, pubEvt.getMessageID());
            m_messagesStore.removeMessageInSession(clientID, pubEvt.getMessageID());
        }
    }
    
    @MQTTMessage(message = PubAckMessage.class)
    void processPubAck(ServerChannel session, PubAckMessage msg) {
        String clientID = (String) session.getAttribute(NettyChannel.ATTR_KEY_CLIENTID);
        int messageID = msg.getMessageID();
        //Remove the message from message store
        m_messagesStore.removeMessageInSession(clientID, messageID);
    }
    
    private void cleanSession(String clientID) {
        LOG.info("cleaning old saved subscriptions for client <{}>", clientID);
        //remove from log all subscriptions
        //m_sessionsStore.wipeSubscriptions(clientID);
        subscriptions.removeForClient(clientID);

        //remove also the messages stored of type QoS1/2
        m_messagesStore.dropMessagesInSession(clientID);
    }
    
    @MQTTMessage(message = PublishMessage.class)
    void processPublish(ServerChannel session, PublishMessage msg) {
        LOG.trace("PUB --PUBLISH--> SRV executePublish invoked with {}", msg);
        String clientID = (String) session.getAttribute(NettyChannel.ATTR_KEY_CLIENTID);
        final String topic = msg.getTopicName();
        //check if the topic can be wrote
        String user = (String) session.getAttribute(NettyChannel.ATTR_KEY_USERNAME);
        if (m_authorizator.canWrite(topic, user, clientID)) {
            executePublish(clientID, msg);
            m_interceptor.notifyTopicPublished(msg, clientID);
        } else {
            LOG.debug("topic {} doesn't have write credentials", topic);
        }
    }
        
    private void executePublish(String clientID, PublishMessage msg) {
        final String topic = msg.getTopicName();
        final AbstractMessage.QOSType qos = msg.getQos();
        final ByteBuffer message = msg.getPayload();
        boolean retain = msg.isRetainFlag();
        final Integer messageID = msg.getMessageID();
        LOG.info("PUBLISH from clientID <{}> on topic <{}> with QoS {}", clientID, topic, qos);

        PublishEvent publishEvt = new PublishEvent(clientID, msg);
        if (qos == AbstractMessage.QOSType.MOST_ONE) { //QoS0
            forward2Subscribers(publishEvt);
        } else if (qos == AbstractMessage.QOSType.LEAST_ONE) { //QoS1
            //TODO use a message store for TO PUBLISH MESSAGES it has nothing to do with inFlight!!
            m_messagesStore.addInFlight(publishEvt, clientID, messageID);
            forward2Subscribers(publishEvt);
            m_messagesStore.cleanInFlight(clientID, messageID);
            //NB the PUB_ACK could be sent also after the addInFlight
            sendPubAck(new PubAckEvent(messageID, clientID));
            LOG.debug("replying with PubAck to MSG ID {}", messageID);
        }  else if (qos == AbstractMessage.QOSType.EXACTLY_ONCE) { //QoS2
            String publishKey = String.format("%s%d", clientID, messageID);
            //store the message in temp store
            m_messagesStore.persistQoS2Message(publishKey, publishEvt);
            sendPubRec(clientID, messageID);
            //Next the client will send us a pub rel
            //NB publish to subscribers for QoS 2 happen upon PUBREL from publisher
        }

        if (retain) {
            if (qos == AbstractMessage.QOSType.MOST_ONE) {
                //QoS == 0 && retain => clean old retained 
                m_messagesStore.cleanRetained(topic);
            } else {
                m_messagesStore.storeRetained(topic, message, qos);
            }
        }
    }
    
    /**
     * Specialized version to publish will testament message.
     */
    private void forwardPublishWill(WillMessage will, String clientID) {
        //it has just to publish the message downstream to the subscribers
        //NB it's a will publish, it needs a PacketIdentifier for this conn, default to 1
        Integer messageId = null;
        if (will.getQos() != AbstractMessage.QOSType.MOST_ONE) {
            messageId = m_messagesStore.nextPacketID(clientID);
        }

        PublishEvent pub = new PublishEvent(will.getTopic(), will.getQos(), will.getPayload(), will.isRetained(),
                clientID, messageId);
        forward2Subscribers(pub);
    }


    /**
     * Flood the subscribers with the message to notify. MessageID is optional and should only used for QoS 1 and 2
     * */
    void forward2Subscribers(PublishEvent pubEvt) {
        final String topic = pubEvt.getTopic();
        final AbstractMessage.QOSType publishingQos = pubEvt.getQos();
        final ByteBuffer origMessage = pubEvt.getMessage();
        boolean retain = pubEvt.isRetain();
        final Integer messageID = pubEvt.getMessageID();
        LOG.debug("forward2Subscribers republishing to existing subscribers that matches the topic {}", topic);
        if (LOG.isDebugEnabled()) {
            LOG.debug("content <{}>", DebugUtils.payload2Str(origMessage));
            LOG.debug("subscription tree {}", subscriptions.dumpTree());
        }
        for (final Subscription sub : subscriptions.matches(topic)) {
            AbstractMessage.QOSType qos = publishingQos;
            if (qos.ordinal() > sub.getRequestedQos().ordinal()) {
                qos = sub.getRequestedQos();
            }

            LOG.debug("Broker republishing to client <{}> topic <{}> qos <{}>, active {}",
                    sub.getClientId(), sub.getTopicFilter(), qos, sub.isActive());
            ByteBuffer message = origMessage.duplicate();
            if (qos == AbstractMessage.QOSType.MOST_ONE && sub.isActive()) {
                //QoS 0
                //forwardPublishQoS0(sub.getClientId(), topic, qos, message, false);
                sendPublish(sub.getClientId(), topic, qos, message, false, null);
            } else {
                //QoS 1 or 2
                //if the target subscription is not clean session and is not connected => store it
                if (!sub.isCleanSession() && !sub.isActive()) {
                    //clone the event with matching clientID
                    PublishEvent newPublishEvt = new PublishEvent(topic, qos, message, retain, sub.getClientId(), messageID != null ? messageID : 0);
                    m_messagesStore.storePublishForFuture(newPublishEvt);
                } else  {
                    //TODO also QoS 1 has to be stored in Flight Zone
                    //if QoS 2 then store it in temp memory
                    if (qos == AbstractMessage.QOSType.EXACTLY_ONCE) {
                        PublishEvent newPublishEvt = new PublishEvent(topic, qos, message, retain, sub.getClientId(), messageID != null ? messageID : 0);
                        m_messagesStore.addInFlight(newPublishEvt, sub.getClientId(), messageID);
                    }
                    //publish
                    if (sub.isActive()) {
                        int messageId = m_messagesStore.nextPacketID(sub.getClientId());
                        sendPublish(sub.getClientId(), topic, qos, message, false, messageId);
                    }
                }
            }
        }
    }

    protected void sendPublish(String clientId, String topic, AbstractMessage.QOSType qos, ByteBuffer message, boolean retained, Integer messageID) {
        LOG.debug("sendPublish invoked clientId <{}> on topic <{}> QoS {} retained {} messageID {}", clientId, topic, qos, retained, messageID);
        PublishMessage pubMessage = new PublishMessage();
        pubMessage.setRetainFlag(retained);
        pubMessage.setTopicName(topic);
        pubMessage.setQos(qos);
        pubMessage.setPayload(message);
        
        LOG.info("send publish message to <{}> on topic <{}>", clientId, topic);
        if (LOG.isDebugEnabled()) {
            LOG.debug("content <{}>", DebugUtils.payload2Str(message));
        }
        //set the PacketIdentifier only for QoS > 0
        if (pubMessage.getQos() != AbstractMessage.QOSType.MOST_ONE) {
            pubMessage.setMessageID(messageID);
        } else {
            if (messageID != null) {
                throw new RuntimeException("Internal bad error, trying to forwardPublish a QoS 0 message with PacketIdentifier: " + messageID);
            }
        }

        if (m_clientIDs == null) {
            throw new RuntimeException("Internal bad error, found m_clientIDs to null while it should be initialized, somewhere it's overwritten!!");
        }
        LOG.debug("clientIDs are {}", m_clientIDs);
        if (m_clientIDs.get(clientId) == null) {
            throw new RuntimeException(String.format("Can't find a ConnectionDescriptor for client <%s> in cache <%s>", clientId, m_clientIDs));
        }
        ServerChannel session = m_clientIDs.get(clientId).getSession();
        LOG.debug("Session for clientId {} is {}", clientId, session);

        String user = (String) session.getAttribute(NettyChannel.ATTR_KEY_USERNAME);
        if (!m_authorizator.canRead(topic, user, clientId)) {
            LOG.debug("topic {} doesn't have read credentials", topic);
            return;
        }

        disruptorPublish(new OutputMessagingEvent(session, pubMessage));
    }
    
    private void sendPubRec(String clientID, int messageID) {
        LOG.trace("PUB <--PUBREC-- SRV sendPubRec invoked for clientID {} with messageID {}", clientID, messageID);
        PubRecMessage pubRecMessage = new PubRecMessage();
        pubRecMessage.setMessageID(messageID);

//        m_clientIDs.get(clientID).getSession().write(pubRecMessage);
        disruptorPublish(new OutputMessagingEvent(m_clientIDs.get(clientID).getSession(), pubRecMessage));
    }
    
    private void sendPubAck(PubAckEvent evt) {
        LOG.trace("sendPubAck invoked");

        String clientId = evt.getClientID();

        PubAckMessage pubAckMessage = new PubAckMessage();
        pubAckMessage.setMessageID(evt.getMessageId());

        try {
            if (m_clientIDs == null) {
                throw new RuntimeException("Internal bad error, found m_clientIDs to null while it should be initialized, somewhere it's overwritten!!");
            }
            LOG.debug("clientIDs are {}", m_clientIDs);
            if (m_clientIDs.get(clientId) == null) {
                throw new RuntimeException(String.format("Can't find a ConnectionDescriptor for client %s in cache %s", clientId, m_clientIDs));
            }
//            LOG.debug("Session for clientId " + clientId + " is " + m_clientIDs.get(clientId).getSession());
//            m_clientIDs.get(clientId).getSession().write(pubAckMessage);
            disruptorPublish(new OutputMessagingEvent(m_clientIDs.get(clientId).getSession(), pubAckMessage));
        }catch(Throwable t) {
            LOG.error(null, t);
        }
    }
    
    /**
     * Second phase of a publish QoS2 protocol, sent by publisher to the broker. Search the stored message and publish
     * to all interested subscribers.
     * */
    @MQTTMessage(message = PubRelMessage.class)
    void processPubRel(ServerChannel session, PubRelMessage msg) {
        String clientID = (String) session.getAttribute(NettyChannel.ATTR_KEY_CLIENTID);
        int messageID = msg.getMessageID();
        LOG.debug("PUB --PUBREL--> SRV processPubRel invoked for clientID {} ad messageID {}", clientID, messageID);
        String publishKey = String.format("%s%d", clientID, messageID);
        PublishEvent evt = m_messagesStore.retrieveQoS2Message(publishKey);
        forward2Subscribers(evt);
        m_messagesStore.removeQoS2Message(publishKey);

        if (evt.isRetain()) {
            final String topic = evt.getTopic();
            final AbstractMessage.QOSType qos = evt.getQos();
            m_messagesStore.storeRetained(topic, evt.getMessage(), qos);
        }

        sendPubComp(clientID, messageID);
    }
    
    private void sendPubComp(String clientID, int messageID) {
        LOG.debug("PUB <--PUBCOMP-- SRV sendPubComp invoked for clientID {} ad messageID {}", clientID, messageID);
        PubCompMessage pubCompMessage = new PubCompMessage();
        pubCompMessage.setMessageID(messageID);

//        m_clientIDs.get(clientID).getSession().write(pubCompMessage);
        disruptorPublish(new OutputMessagingEvent(m_clientIDs.get(clientID).getSession(), pubCompMessage));
    }
    
    @MQTTMessage(message = PubRecMessage.class)
    void processPubRec(ServerChannel session, PubRecMessage msg) {
        String clientID = (String) session.getAttribute(NettyChannel.ATTR_KEY_CLIENTID);
        int messageID = msg.getMessageID();
        //once received a PUBREC reply with a PUBREL(messageID)
        LOG.debug("\t\tSRV <--PUBREC-- SUB processPubRec invoked for clientID {} ad messageID {}", clientID, messageID);
        PubRelMessage pubRelMessage = new PubRelMessage();
        pubRelMessage.setMessageID(messageID);
        pubRelMessage.setQos(AbstractMessage.QOSType.LEAST_ONE);

//        m_clientIDs.get(clientID).getSession().write(pubRelMessage);
        //disruptorPublish(new OutputMessagingEvent(m_clientIDs.get(clientID).getSession(), pubRelMessage));
        session.write(pubRelMessage);
    }
    
    @MQTTMessage(message = PubCompMessage.class)
    void processPubComp(ServerChannel session, PubCompMessage msg) {
        String clientID = (String) session.getAttribute(NettyChannel.ATTR_KEY_CLIENTID);
        int messageID = msg.getMessageID();
        LOG.debug("\t\tSRV <--PUBCOMP-- SUB processPubComp invoked for clientID {} ad messageID {}", clientID, messageID);
        //once received the PUBCOMP then remove the message from the temp memory
        m_messagesStore.cleanInFlight(clientID, messageID);
    }
    
    @MQTTMessage(message = DisconnectMessage.class)
    void processDisconnect(ServerChannel session, DisconnectMessage msg) throws InterruptedException {
        String clientID = (String) session.getAttribute(NettyChannel.ATTR_KEY_CLIENTID);
        boolean cleanSession = (Boolean) session.getAttribute(NettyChannel.ATTR_KEY_CLEANSESSION);
        if (cleanSession) {
            //cleanup topic subscriptions
            cleanSession(clientID);
        }
//        m_notifier.disconnect(evt.getSession());
        m_clientIDs.remove(clientID);
        session.close(true);

        //de-activate the subscriptions for this ClientID
        subscriptions.deactivate(clientID);
        //cleanup the will store
        m_willStore.remove(clientID);
        
        LOG.info("DISCONNECT client <{}> with clean session {}", clientID, cleanSession);
        m_interceptor.notifyClientDisconnected(clientID);
    }
    
    void processConnectionLost(LostConnectionEvent evt) {
        String clientID = evt.clientID;
        //If already removed a disconnect message was already processed for this clientID
        if (m_clientIDs.remove(clientID) != null) {

            //de-activate the subscriptions for this ClientID
            subscriptions.deactivate(clientID);
            LOG.info("Lost connection with client <{}>", clientID);
        }
        //publish the Will message (if any) for the clientID
        if (m_willStore.containsKey(clientID)) {
            WillMessage will = m_willStore.get(clientID);
            forwardPublishWill(will, clientID);
            m_willStore.remove(clientID);
        }
    }
    
    /**
     * Remove the clientID from topic subscription, if not previously subscribed,
     * doesn't reply any error
     */
    @MQTTMessage(message = UnsubscribeMessage.class)
    void processUnsubscribe(ServerChannel session, UnsubscribeMessage msg) {
        List<String> topics = msg.topicFilters();
        int messageID = msg.getMessageID();
        String clientID = (String) session.getAttribute(NettyChannel.ATTR_KEY_CLIENTID);
        LOG.debug("UNSUBSCRIBE subscription on topics {} for clientID <{}>", topics, clientID);

        for (String topic : topics) {
            boolean validTopic = SubscriptionsStore.validate(topic);
            if (!validTopic) {
                //close the connection, not valid topicFilter is a protocol violation
                session.close(true);
                LOG.warn("UNSUBSCRIBE found an invalid topic filter <{}> for clientID <{}>", topic, clientID);
                return;
            }

            subscriptions.removeSubscription(topic, clientID);
            m_sessionsStore.removeSubscription(topic, clientID);
            m_interceptor.notifyTopicUnsubscribed(topic, clientID);
        }

        //ack the client
        UnsubAckMessage ackMessage = new UnsubAckMessage();
        ackMessage.setMessageID(messageID);

        LOG.info("replying with UnsubAck to MSG ID {}", messageID);
        session.write(ackMessage);
    }
    
    @MQTTMessage(message = SubscribeMessage.class)
    void processSubscribe(ServerChannel session, SubscribeMessage msg) {
        String clientID = (String) session.getAttribute(NettyChannel.ATTR_KEY_CLIENTID);
        boolean cleanSession = (Boolean) session.getAttribute(NettyChannel.ATTR_KEY_CLEANSESSION);
        LOG.debug("SUBSCRIBE client <{}> packetID {}", clientID, msg.getMessageID());

        //ack the client
        SubAckMessage ackMessage = new SubAckMessage();
        ackMessage.setMessageID(msg.getMessageID());

        for (SubscribeMessage.Couple req : msg.subscriptions()) {
            AbstractMessage.QOSType qos = AbstractMessage.QOSType.values()[req.getQos()];
            Subscription newSubscription = new Subscription(clientID, req.getTopicFilter(), qos, cleanSession);
            boolean valid = subscribeSingleTopic(newSubscription, req.getTopicFilter());
            ackMessage.addType(valid ? qos : AbstractMessage.QOSType.FAILURE);
        }

        LOG.debug("SUBACK for packetID {}", msg.getMessageID());
        session.write(ackMessage);
    }
    
    private boolean subscribeSingleTopic(Subscription newSubscription, final String topic) {
        LOG.info("<{}> subscribed to topic <{}> with QoS {}", 
                newSubscription.getClientId(), topic, 
                AbstractMessage.QOSType.formatQoS(newSubscription.getRequestedQos()));
        boolean validTopic = SubscriptionsStore.validate(newSubscription.getTopicFilter());
        if (!validTopic) {
            //send SUBACK with 0x80 for this topic filter
            return false;
        }
        m_sessionsStore.addNewSubscription(newSubscription);
        subscriptions.add(newSubscription);

        //notify the Observables
        m_interceptor.notifyTopicSubscribed(newSubscription);

        //scans retained messages to be published to the new subscription
        Collection<IMessagesStore.StoredMessage> messages = m_messagesStore.searchMatching(new IMatchingCondition() {
            public boolean match(String key) {
                return SubscriptionsStore.matchTopics(key, topic);
            }
        });

        for (IMessagesStore.StoredMessage storedMsg : messages) {
            //fire the as retained the message
            LOG.debug("send publish message for topic {}", topic);
            //forwardPublishQoS0(newSubscription.getClientId(), storedMsg.getTopic(), storedMsg.getQos(), storedMsg.getPayload(), true);
            Integer packetID = storedMsg.getQos() == QOSType.MOST_ONE ? null :
                    m_messagesStore.nextPacketID(newSubscription.getClientId());
            sendPublish(newSubscription.getClientId(), storedMsg.getTopic(), storedMsg.getQos(), storedMsg.getPayload(), true, packetID);
        }
        return true;
    }
    
    private void disruptorPublish(OutputMessagingEvent msgEvent) {
        LOG.debug("disruptorPublish publishing event on output {}", msgEvent);
        long sequence = m_ringBuffer.next();
        ValueEvent event = m_ringBuffer.get(sequence);

        event.setEvent(msgEvent);
        
        m_ringBuffer.publish(sequence); 
    }

    public void onEvent(ValueEvent t, long l, boolean bln) throws Exception {
        try {
            MessagingEvent evt = t.getEvent();
            //It's always of type OutputMessagingEvent
            OutputMessagingEvent outEvent = (OutputMessagingEvent) evt;
            LOG.debug("Output event, sending {}", outEvent.getMessage());
            outEvent.getChannel().write(outEvent.getMessage());
        } finally {
            t.setEvent(null); //free the reference to all Netty stuff
        }
    }

}
