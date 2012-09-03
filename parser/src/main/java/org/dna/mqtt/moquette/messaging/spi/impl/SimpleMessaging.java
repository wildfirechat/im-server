package org.dna.mqtt.moquette.messaging.spi.impl;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import java.io.Serializable;
import java.text.ParseException;
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
import org.dna.mqtt.moquette.messaging.spi.INotifier;
import org.dna.mqtt.moquette.messaging.spi.IStorageService;
import org.dna.mqtt.moquette.messaging.spi.impl.SubscriptionsStore.Token;
import org.dna.mqtt.moquette.messaging.spi.impl.events.*;
import org.dna.mqtt.moquette.proto.messages.*;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.server.ConnectionDescriptor;
import org.dna.mqtt.moquette.server.Constants;
import org.dna.mqtt.moquette.server.IAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
public class SimpleMessaging implements IMessaging, EventHandler<ValueEvent>/*, Runnable*/ {

    //TODO probably move this
    public static class StoredMessage implements Serializable {
        QOSType m_qos;
        byte[] m_payload;

        StoredMessage(byte[] message, QOSType qos) {
            m_qos = qos;
            m_payload = message;
        }
        
        QOSType getQos() {
            return m_qos;
        }
        
        byte[] getPayload() {
            return m_payload;
        }
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(SimpleMessaging.class);
    
    private SubscriptionsStore subscriptions = new SubscriptionsStore();
    
    //private BlockingQueue<MessagingEvent> m_inboundQueue = new LinkedBlockingQueue<MessagingEvent>();
    private RingBuffer<ValueEvent> m_ringBuffer;

//    private INotifier m_notifier;

    private IStorageService m_storageService;

    Map<String, ConnectionDescriptor> m_clientIDs = new HashMap<String, ConnectionDescriptor>();

    private IAuthenticator m_authenticator;
    
    private ExecutorService m_executor;
    BatchEventProcessor<ValueEvent> m_eventProcessor;

    private static SimpleMessaging INSTANCE;
    
    private SimpleMessaging() {
        /*m_ringBuffer = new RingBuffer<ValueEvent>(ValueEvent.EVENT_FACTORY, 1024 * 32);
        
        SequenceBarrier barrier = m_ringBuffer.newBarrier();       
        m_eventProcessor = new BatchEventProcessor<ValueEvent>(m_ringBuffer, barrier, this);
        m_ringBuffer.setGatingSequences(m_eventProcessor.getSequence());  
        m_executor.submit(m_eventProcessor);

        disruptorPublish(new InitEvent());   */
        
 /*       m_storageService = new HawtDBStorageService();
        m_storageService.initStore();

        subscriptions.init(m_storageService); */
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
    
//    public void run() {
//        eventLoop();
//    }
    
    private void disruptorPublish(MessagingEvent msgEvent) {
        long sequence = m_ringBuffer.next();
        ValueEvent event = m_ringBuffer.get(sequence);

        event.setEvent(msgEvent);
        
        m_ringBuffer.publish(sequence); 
    }
    

    public void publish(String topic, byte[] message, QOSType qos, boolean retain, String clientID, IoSession session) {
        disruptorPublish(new PublishEvent(topic, qos, message, retain, clientID, session));
    }

    public void publish(String topic, byte[] message, QOSType qos, boolean retain, String clientID, int messageID, IoSession session) {
        disruptorPublish(new PublishEvent(topic, qos, message, retain, clientID, messageID, session));
    }

    public void subscribe(String clientId, String topic, QOSType qos, boolean cleanSession, int messageID) {
        Subscription newSubscription = new Subscription(clientId, topic, qos, cleanSession);
        LOG.debug("subscribe invoked for topic: " + topic);
        disruptorPublish(new SubscribeEvent(newSubscription, messageID));
    }
    
    
    public void unsubscribe(String topic, String clientID) {
        disruptorPublish(new UnsubscribeEvent(topic, clientID));
    }
    
    
    public void disconnect(IoSession session) {
        disruptorPublish(new DisconnectEvent(session));
    }

    //method used by hte Notifier to re-put an event on the inbound queue
    private void refill(MessagingEvent evt) throws InterruptedException {
        disruptorPublish(evt);
    }

    public void republishStored(String clientID) {
        //create the event to push
        disruptorPublish(new RepublishEvent(clientID));
    }

    public void connect(IoSession session, ConnectMessage msg) {
        disruptorPublish(new ConnectEvent(session, msg));
    }

    /**
     * Verify if the 2 topics matching respecting the rules of MQTT Appendix A
     */
    protected boolean matchTopics(String msgTopic, String subscriptionTopic) {
        try {
            List<Token> msgTokens = SubscriptionsStore.splitTopic(msgTopic);
            List<Token> subscriptionTokens = SubscriptionsStore.splitTopic(subscriptionTopic);
            int i = 0;
            for (; i< subscriptionTokens.size(); i++) {
                Token subToken = subscriptionTokens.get(i);
                if (subToken != Token.MULTI && subToken != Token.SINGLE) {
                    Token msgToken = msgTokens.get(i);
                    if (!msgToken.equals(subToken)) {
                        return false;
                    }
                } else {
                    if (subToken == Token.MULTI) {
                        return true;
                    }
                    if (subToken == Token.SINGLE) {
                        //skip a step forward
                    }
                }
            }
            return i == msgTokens.size();
        } catch (ParseException ex) {
            LOG.error(null, ex);
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * NOT SAFE Method, to be removed because used only in tests
     */
    protected SubscriptionsStore getSubscriptions() {
        return subscriptions;
    }

    public void removeSubscriptions(String clientID) {
        disruptorPublish(new RemoveAllSubscriptionsEvent(clientID));
    }

    public void close() {
        disruptorPublish(new CloseEvent());
    }
    
    public void onEvent(ValueEvent t, long l, boolean bln) throws Exception {
        LOG.debug("onEvent processing event");
        MessagingEvent evt = t.getEvent();
        if (evt instanceof PublishEvent) {
            processPublish((PublishEvent) evt);
        } else if (evt instanceof SubscribeEvent) {
            processSubscribe((SubscribeEvent) evt);
        } else if (evt instanceof UnsubscribeEvent) {
            processUnsubscribe((UnsubscribeEvent) evt);
        } else if (evt instanceof RemoveAllSubscriptionsEvent) {
            processRemoveAllSubscriptions((RemoveAllSubscriptionsEvent) evt);
        } else if (evt instanceof CloseEvent) {
            processClose();
        } else if (evt instanceof DisconnectEvent) {
            processDisconnect((DisconnectEvent) evt);
        } else if (evt instanceof CleanInFlightEvent) {
            //remove the message from inflight storage
            m_storageService.cleanInFlight(((CleanInFlightEvent) evt).getMsgId());
        } else if (evt instanceof RepublishEvent) {
            processRepublish((RepublishEvent) evt);
        } else if (evt instanceof ConnectEvent) {
            processConnect((ConnectEvent) evt);
        } else if (evt instanceof InitEvent) {
            processInit();
        }
    }

    private void processInit() {
        m_storageService = new HawtDBStorageService();
        m_storageService.initStore();

        subscriptions.init(m_storageService);
    }

//    protected void eventLoop() {
//        LOG.debug("Started event loop");
//        boolean interrupted = false;
//        while (!interrupted) {
//            try { 
//                MessagingEvent evt = m_inboundQueue.take();
//                if (evt instanceof PublishEvent) {
//                    processPublish((PublishEvent) evt);
//                } else if (evt instanceof SubscribeEvent) {
//                    processSubscribe((SubscribeEvent) evt);
//                } else if (evt instanceof UnsubscribeEvent) {
//                    processUnsubscribe((UnsubscribeEvent) evt);
//                } else if (evt instanceof RemoveAllSubscriptionsEvent) {
//                    processRemoveAllSubscriptions((RemoveAllSubscriptionsEvent) evt);
//                } else if (evt instanceof CloseEvent) {
//                    processClose();
//                } else if (evt instanceof DisconnectEvent) {
//                    processDisconnect((DisconnectEvent) evt);
//                } else if (evt instanceof CleanInFlightEvent) {
//                    //remove the message from inflight storage
//                    m_storageService.cleanInFlight(((CleanInFlightEvent) evt).getMsgId());
//                } else if (evt instanceof RepublishEvent) {
//                    processRepublish((RepublishEvent) evt);
//                } else if (evt instanceof ConnectEvent) {
//                    processConnect((ConnectEvent) evt);
//                }
//            } catch (InterruptedException ex) {
//                processClose();
//                interrupted = true;
//            }
//        }
//    }

    protected void processConnect(ConnectEvent evt) {
        ConnectMessage msg = evt.getMessage();
        IoSession session = evt.getSession();

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
                removeSubscriptions(msg.getClientID());
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
            publish(msg.getWillTopic(), msg.getWillMessage().getBytes(),
                    willQos, msg.isWillRetain(), msg.getClientID(), session);
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

        //handle clean session flag
        if (msg.isCleanSession()) {
            //remove all prev subscriptions
            //cleanup topic subscriptions
            removeSubscriptions(msg.getClientID());
        }  else {
            //force the republish of stored QoS1 and QoS2
            republishStored(msg.getClientID());
        }

        ConnAckMessage okResp = new ConnAckMessage();
        okResp.setReturnCode(ConnAckMessage.CONNECTION_ACCEPTED);
        session.write(okResp);
    }

    protected void processPublish(PublishEvent evt) throws InterruptedException {
        LOG.debug("processPublish invoked");
        final String topic = evt.getTopic();
        final QOSType qos = evt.getQos();
        final byte[] message = evt.getMessage();
        boolean retain = evt.isRetain();

        CleanInFlightEvent cleanEvt = null;

        if (qos == QOSType.LEAST_ONE) {
            //store the temporary message
            String publishKey = String.format("%s%d", evt.getClientID(), evt.getMessageID());
            m_storageService.addInFlight(evt, publishKey);
            cleanEvt = new CleanInFlightEvent(publishKey);
        }
        
        for (final Subscription sub : subscriptions.matches(topic)) {
            if (qos == QOSType.MOST_ONE) {
                //QoS 0
                notify(new NotifyEvent(sub.clientId, topic, qos, message, false));
            } else {
                //QoS 1 or 2
                //if the target subscription is not clean session and is not connected => store it
                if (!sub.isCleanSession() && !sub.isActive()) {
                    m_storageService.storePublishForFuture(evt);
                }
                notify(new NotifyEvent(sub.clientId, topic, qos, message, false));
            }
        }

        if (cleanEvt != null) {
            refill(cleanEvt);
            sendPubAck(new PubAckEvent(evt.getMessageID(), evt.getClientID()));
        }

        if (retain) {
            m_storageService.storeRetained(topic, message, qos);
        }
    }


    protected void processSubscribe(SubscribeEvent evt) {
        LOG.debug("processSubscribe invoked");
        Subscription newSubscription = evt.getSubscription();
        final String topic = newSubscription.getTopic();
        
        subscriptions.add(newSubscription);

        //scans retained messages to be published to the new subscription
        Collection<StoredMessage> messages = m_storageService.searchMatching(new IMatchingCondition() {
            public boolean match(String key) {
                return matchTopics(key, topic);
            }
        });

        for (StoredMessage storedMsg : messages) {
            //fire the as retained the message
            LOG.debug("Inserting NotifyEvent into outbound for topic " + topic);
            notify(new NotifyEvent(newSubscription.clientId, topic, storedMsg.getQos(),
                    storedMsg.getPayload(), true));
        }

//        //ack the client
//        SubAckMessage ackMessage = new SubAckMessage();
//        ackMessage.setMessageID(evt.getMessageID());
//
//        //TODO by now it handles only QoS 0 messages
//        /*for (int i = 0; i < evt.subscriptions().size(); i++) {
//            ackMessage.addType(QOSType.MOST_ONE);
//        } */
//        LOG.info("replying with SubAct to MSG ID {0}", evt.getMessageID());
//        //session.write(ackMessage);
//        IoSession session = m_clientIDs.get(evt.getSubscription().clientId).getSession();
//        session.write(ackMessage).awaitUninterruptibly();
    }
    
    protected void processUnsubscribe(UnsubscribeEvent evt) {
        LOG.debug("processSubscribe invoked");
        subscriptions.removeSubscription(evt.getTopic(), evt.getClientID());
    }
    
    protected void processRemoveAllSubscriptions(RemoveAllSubscriptionsEvent evt) {
        LOG.debug("processRemoveAllSubscriptions invoked");
        subscriptions.removeForClient(evt.getClientID());

        //remove also the messages stored of type QoS1/2
        m_storageService.cleanPersistedPublishes(evt.getClientID());
    }

    private void processDisconnect(DisconnectEvent evt) throws InterruptedException {
//        m_notifier.disconnect(evt.getSession());
        IoSession session = evt.getSession();
        String clientID = (String) session.getAttribute(Constants.ATTR_CLIENTID);
        m_clientIDs.remove(clientID);
        session.close(true);

        //de-activate the subscriptions for this ClientID
//        String clientID = (String) evt.getSession().getAttribute(Constants.ATTR_CLIENTID);
        subscriptions.disconnect(clientID);
    }
    
    private void processClose() {
        LOG.debug("processClose invoked");
        m_storageService.close();

//        m_eventProcessor.halt();
        m_executor.shutdown();
    }

    private void processRepublish(RepublishEvent evt) throws InterruptedException {
        List<PublishEvent> publishedEvents = m_storageService.retrivePersistedPublishes(evt.getClientID());
        if (publishedEvents == null) {
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

        LOG.debug("notify invoked");
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
}
