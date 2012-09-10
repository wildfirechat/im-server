package org.dna.mqtt.moquette.messaging.spi.impl;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.messaging.spi.IMatchingCondition;
import org.dna.mqtt.moquette.messaging.spi.IMessaging;
import org.dna.mqtt.moquette.messaging.spi.IStorageService;
import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.SubscriptionsStore;
import org.dna.mqtt.moquette.messaging.spi.impl.events.*;
import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.Subscription;
import org.dna.mqtt.moquette.proto.PubCompMessage;
import org.dna.mqtt.moquette.proto.messages.*;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.server.ConnectionDescriptor;
import org.dna.mqtt.moquette.server.Constants;
import org.dna.mqtt.moquette.server.IAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.dna.mqtt.moquette.messaging.spi.impl.HawtDBStorageService.StoredMessage;

/**
 *
 * Singleton class that orchestrate the execution of the protocol.
 *
 * Uses the LMAX Disruptor to serialize the incoming, requests, because it work in a evented fashion.
 *
 * @author andrea
 */
public class SimpleMessaging implements IMessaging, EventHandler<ValueEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleMessaging.class);
    
    private SubscriptionsStore subscriptions = new SubscriptionsStore();
    
    private RingBuffer<ValueEvent> m_ringBuffer;

//    private INotifier m_notifier;

    private IStorageService m_storageService;

    Map<String, ConnectionDescriptor> m_clientIDs = new HashMap<String, ConnectionDescriptor>();

    private IAuthenticator m_authenticator;
    
    private ExecutorService m_executor;
    BatchEventProcessor<ValueEvent> m_eventProcessor;

    private static SimpleMessaging INSTANCE;
    
    private SimpleMessaging() {
    }

    public static SimpleMessaging getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SimpleMessaging();
        }
        return INSTANCE;
    }

    public void init() {
        m_executor = Executors.newFixedThreadPool(1);

        m_ringBuffer = new RingBuffer<ValueEvent>(ValueEvent.EVENT_FACTORY, 1024 * 32);

        SequenceBarrier barrier = m_ringBuffer.newBarrier();
        m_eventProcessor = new BatchEventProcessor<ValueEvent>(m_ringBuffer, barrier, this);
        m_ringBuffer.setGatingSequences(m_eventProcessor.getSequence());
        m_executor.submit(m_eventProcessor);

        disruptorPublish(new InitEvent());
    }

    
/*    public void setNotifier(INotifier notifier) {
        m_notifier= notifier;
    }  */
    

    private void disruptorPublish(MessagingEvent msgEvent) {
        long sequence = m_ringBuffer.next();
        ValueEvent event = m_ringBuffer.get(sequence);

        event.setEvent(msgEvent);
        
        m_ringBuffer.publish(sequence); 
    }
    

    public void disconnect(IoSession session) {
        disruptorPublish(new DisconnectEvent(session));
    }

    //method used by hte Notifier to re-put an event on the inbound queue
    private void refill(MessagingEvent evt) {
        disruptorPublish(evt);
    }

    public void republishStored(String clientID) {
        //create the event to push
        LOG.debug("republishStored invoked to publish soterd messages for clientID " + clientID);
        disruptorPublish(new RepublishEvent(clientID));
    }

    public void handleProtocolMessage(IoSession session, AbstractMessage msg) {
        disruptorPublish(new ProtocolEvent(session, msg));
    }


    /**
     * NOT SAFE Method, to be removed because used only in tests
     */
    protected SubscriptionsStore getSubscriptions() {
        return subscriptions;
    }


    public void stop() {
        disruptorPublish(new StopEvent());
    }
    
    public void onEvent(ValueEvent t, long l, boolean bln) throws Exception {
        MessagingEvent evt = t.getEvent();
        LOG.debug("onEvent processing messaging event " + evt);
        if (evt instanceof PublishEvent) {
            processPublish((PublishEvent) evt);
        } else if (evt instanceof StopEvent) {
            processStop();
        } else if (evt instanceof DisconnectEvent) {
            DisconnectEvent disEvt = (DisconnectEvent) evt;
            String clientID = (String) disEvt.getSession().getAttribute(Constants.ATTR_CLIENTID);
            processDisconnect(disEvt.getSession(), clientID);
        } else if (evt instanceof RepublishEvent) {
            processRepublish((RepublishEvent) evt);
        } else if (evt instanceof ProtocolEvent) {
            IoSession session = ((ProtocolEvent) evt).getSession();
            AbstractMessage message = ((ProtocolEvent) evt).getMessage();
            if (message instanceof ConnectMessage) {
                processConnect(session, (ConnectMessage) message);
            } else if (message instanceof  PublishMessage) {
                PublishMessage pubMsg = (PublishMessage) message;
                PublishEvent pubEvt;

                String clientID = (String) session.getAttribute(Constants.ATTR_CLIENTID);

                if (message.getQos() == QOSType.MOST_ONE) {
                    pubEvt = new PublishEvent(pubMsg.getTopicName(), pubMsg.getQos(), pubMsg.getPayload(), pubMsg.isRetainFlag(), clientID, session);

                } else {
                    pubEvt = new PublishEvent(pubMsg.getTopicName(), pubMsg.getQos(), pubMsg.getPayload(), pubMsg.isRetainFlag(), clientID, pubMsg.getMessageID(), session);
                }
                processPublish(pubEvt);
            } else if (message instanceof DisconnectMessage) {
                String clientID = (String) session.getAttribute(Constants.ATTR_CLIENTID);
                boolean cleanSession = (Boolean) session.getAttribute(Constants.CLEAN_SESSION);
                if (cleanSession) {
                    //cleanup topic subscriptions
                    processRemoveAllSubscriptions(clientID);
                }

                //close the TCP connection
                //session.close(true);
                processDisconnect(session, clientID);
            } else if (message instanceof UnsubscribeMessage) {
                UnsubscribeMessage unsubMsg = (UnsubscribeMessage) message;
                String clientID = (String) session.getAttribute(Constants.ATTR_CLIENTID);
                processUnsubscribe(session, clientID, unsubMsg.topics(), unsubMsg.getMessageID());
            } else if (message instanceof SubscribeMessage) {
                String clientID = (String) session.getAttribute(Constants.ATTR_CLIENTID);
                boolean cleanSession = (Boolean) session.getAttribute(Constants.CLEAN_SESSION);
                processSubscribe(session, (SubscribeMessage) message, clientID, cleanSession);
            } else if (message instanceof PubRelMessage) {
                String clientID = (String) session.getAttribute(Constants.ATTR_CLIENTID);
                int messageID = ((PubRelMessage) message).getMessageID();
                processPubRel(clientID, messageID);
            } else {
                throw new RuntimeException("Illegal message received " + message);
            }

        } else if (evt instanceof InitEvent) {
            processInit();
        }
    }

    private void processInit() {
        m_storageService = new HawtDBStorageService();
        m_storageService.initStore();

        subscriptions.init(m_storageService);
    }


    protected void processConnect(IoSession session, ConnectMessage msg) {
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
            IoSession oldSession = m_clientIDs.get(msg.getClientID()).getSession();
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
        session.setAttribute("keepAlive", keepAlive);
        session.setAttribute(Constants.CLEAN_SESSION, msg.isCleanSession());
        //used to track the client in the subscription and publishing phases.
        session.setAttribute(Constants.ATTR_CLIENTID, msg.getClientID());

        session.getConfig().setIdleTime(IdleStatus.READER_IDLE, Math.round(keepAlive * 1.5f));

        //Handle will flag
        if (msg.isWillFlag()) {
            QOSType willQos = QOSType.values()[msg.getWillQos()];
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
        }  else {
            //force the republish of stored QoS1 and QoS2
            republishStored(msg.getClientID());
        }

        ConnAckMessage okResp = new ConnAckMessage();
        okResp.setReturnCode(ConnAckMessage.CONNECTION_ACCEPTED);
        session.write(okResp);
    }

    /**
     * Second phase of a publish QoS2 protocol, sent by publisher to the broker. Search the stored message and publish
     * to all interested subscribers.
     * */
    protected void processPubRel(String clientID, int messageID) {
        String publishKey = String.format("%s%d", clientID, messageID);
        PublishEvent evt = m_storageService.retrieveQoS2Message(publishKey);

        final String topic = evt.getTopic();
        final QOSType qos = evt.getQos();
        final byte[] message = evt.getMessage();
        boolean retain = evt.isRetain();

        publish2Subscribers(topic, qos, message, retain, evt.getMessageID());

        m_storageService.removeQoS2Message(publishKey);

        if (retain) {
            m_storageService.storeRetained(topic, message, qos);
        }

        sendPubComp(clientID, messageID);
    }

    protected void processPublish(PublishEvent evt) {
        LOG.debug("processPublish invoked with " + evt);
        final String topic = evt.getTopic();
        final QOSType qos = evt.getQos();
        final byte[] message = evt.getMessage();
        boolean retain = evt.isRetain();

        String publishKey = null;
        if (qos == QOSType.LEAST_ONE) {
            //store the temporary message
            publishKey = String.format("%s%d", evt.getClientID(), evt.getMessageID());
            m_storageService.addInFlight(evt, publishKey);
        }  else if (qos == QOSType.EXACTLY_ONCE) {
            publishKey = String.format("%s%d", evt.getClientID(), evt.getMessageID());
            //store the message in temp store
            m_storageService.persistQoS2Message(publishKey, evt);
            sendPubRec(evt.getClientID(), evt.getMessageID());
        }

        publish2Subscribers(topic, qos, message, retain, evt.getMessageID());

        if (qos == QOSType.LEAST_ONE) {
            assert publishKey != null;
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
    private void publish2Subscribers(String topic, QOSType qos, byte[] message, boolean retain, Integer messageID) {
        for (final Subscription sub : subscriptions.matches(topic)) {
            if (qos == QOSType.MOST_ONE) {
                //QoS 0
                notify(new NotifyEvent(sub.getClientId(), topic, qos, message, false));
            } else {
                //QoS 1 or 2
                //if the target subscription is not clean session and is not connected => store it
                if (!sub.isCleanSession() && !sub.isActive()) {
                    //clone the event with matching clientID
                    PublishEvent newPublishEvt = new PublishEvent(topic, qos, message, retain, sub.getClientId(), messageID, null);
                    m_storageService.storePublishForFuture(newPublishEvt);
                } else  {
                    notify(new NotifyEvent(sub.getClientId(), topic, qos, message, false));
                }
            }
        }
    }


    private void subscribeSingleTopic(Subscription newSubscription, final String topic) {
        subscriptions.add(newSubscription);

        //scans retained messages to be published to the new subscription
        Collection<StoredMessage> messages = m_storageService.searchMatching(new IMatchingCondition() {
            public boolean match(String key) {
                return  SubscriptionsStore.matchTopics(key, topic);
            }
        });

        for (StoredMessage storedMsg : messages) {
            //fire the as retained the message
            LOG.debug("Inserting NotifyEvent into outbound for topic " + topic);
            notify(new NotifyEvent(newSubscription.getClientId(), topic, storedMsg.getQos(), storedMsg.getPayload(), true));
        }
    }

    protected void processSubscribe(IoSession session, SubscribeMessage msg, String clientID, boolean cleanSession) {
        LOG.debug("processSubscribe invoked");

        for (SubscribeMessage.Couple req : msg.subscriptions()) {
            QOSType qos = AbstractMessage.QOSType.values()[req.getQos()];
            Subscription newSubscription = new Subscription(clientID, req.getTopic(), qos, cleanSession);
            subscribeSingleTopic(newSubscription, req.getTopic());
        }

        //ack the client
        SubAckMessage ackMessage = new SubAckMessage();
        ackMessage.setMessageID(msg.getMessageID());

        //TODO by now it handles only QoS 0 messages
        for (int i = 0; i < msg.subscriptions().size(); i++) {
            ackMessage.addType(QOSType.MOST_ONE);
        }
        LOG.info("replying with SubAct to MSG ID " + msg.getMessageID());
        session.write(ackMessage);

    }

    /**
     * Remove the clientID from topic subscription, if not previously subscribed,
     * doesn't reply any error
     */
    protected void processUnsubscribe(IoSession session, String clientID, List<String> topics, int messageID) {
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
    
    protected void processRemoveAllSubscriptions(String clientID) {
        LOG.debug("processRemoveAllSubscriptions invoked");
        subscriptions.removeForClient(clientID);

        //remove also the messages stored of type QoS1/2
        m_storageService.cleanPersistedPublishes(clientID);
    }

    private void processDisconnect(IoSession session, String clientID) throws InterruptedException {
//        m_notifier.disconnect(evt.getSession());
        m_clientIDs.remove(clientID);
        session.close(true);

        //de-activate the subscriptions for this ClientID
//        String clientID = (String) evt.getSession().getAttribute(Constants.ATTR_CLIENTID);
        subscriptions.deactivate(clientID);
    }
    
    private void processStop() {
        LOG.debug("processStop invoked");
        m_storageService.close();

//        m_eventProcessor.halt();
        m_executor.shutdown();
    }

    private void processRepublish(RepublishEvent evt) throws InterruptedException {
        LOG.debug("processRepublish invoked");
        List<PublishEvent> publishedEvents = m_storageService.retrivePersistedPublishes(evt.getClientID());
        if (publishedEvents == null) {
            LOG.debug("processRepublish, no stored publish events");
            return;
        }

        for (PublishEvent pubEvt : publishedEvents) {
            notify(new NotifyEvent(pubEvt.getClientID(), pubEvt.getTopic(), pubEvt.getQos(),
                    pubEvt.getMessage(), false, pubEvt.getMessageID()));
        }
    }

    private void notify(NotifyEvent evt) {
        LOG.debug("notify invoked with event " + evt);
        String clientId = evt.getClientId();
        PublishMessage pubMessage = new PublishMessage();
        pubMessage.setRetainFlag(evt.isRetained());
        pubMessage.setTopicName(evt.getTopic());
        pubMessage.setQos(evt.getQos());
        pubMessage.setPayload(evt.getMessage());
        if (pubMessage.getQos() != QOSType.MOST_ONE) {
            pubMessage.setMessageID(evt.getMessageID());
        }

        try {
            assert m_clientIDs != null;
            LOG.debug("clientIDs are " + m_clientIDs);
            assert m_clientIDs.get(clientId) != null;
            LOG.debug("Session for clientId " + clientId + " is " + m_clientIDs.get(clientId).getSession());
            m_clientIDs.get(clientId).getSession().write(pubMessage);
        }catch(Throwable t) {
            LOG.error(null, t);
        }
    }

    private void sendPubAck(PubAckEvent evt) {
        LOG.debug("sendPubAck invoked");

        String clientId = evt.getClientID();

        PubAckMessage pubAckMessage = new PubAckMessage();
        pubAckMessage.setMessageID(evt.getMessageId());

        try {
            assert m_clientIDs != null;
            LOG.debug("clientIDs are " + m_clientIDs);
            assert m_clientIDs.get(clientId) != null;
            LOG.debug("Session for clientId " + clientId + " is " + m_clientIDs.get(clientId).getSession());
            m_clientIDs.get(clientId).getSession().write(pubAckMessage);
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

    private void sendPubComp(String clientID, int messageID) {
        LOG.debug(String.format("sendPubComp invoked for clientID %s ad messageID %d", clientID, messageID));
        PubCompMessage pubCompMessage = new PubCompMessage();
        pubCompMessage.setMessageID(messageID);

        m_clientIDs.get(clientID).getSession().write(pubCompMessage);
    }
}
