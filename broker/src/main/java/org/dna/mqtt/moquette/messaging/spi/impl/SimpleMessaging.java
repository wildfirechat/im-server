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
import org.dna.mqtt.moquette.messaging.spi.impl.HawtDBStorageService.StoredMessage;
import org.dna.mqtt.moquette.messaging.spi.impl.events.*;
import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.Subscription;
import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.SubscriptionsStore;
import org.dna.mqtt.moquette.proto.PubCompMessage;
import org.dna.mqtt.moquette.proto.messages.*;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.server.ConnectionDescriptor;
import org.dna.mqtt.moquette.server.Constants;
import org.dna.mqtt.moquette.server.IAuthenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private ExecutorService m_executor;
    BatchEventProcessor<ValueEvent> m_eventProcessor;

    private static SimpleMessaging INSTANCE;
    
    private ProtocolProcessor m_processor = new ProtocolProcessor();
    
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
                m_processor.processConnect(session, (ConnectMessage) message);
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
                    m_processor.processRemoveAllSubscriptions(clientID);
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
                m_processor.processPubRel(clientID, messageID);
            } else if (message instanceof PubRecMessage) {
                String clientID = (String) session.getAttribute(Constants.ATTR_CLIENTID);
                int messageID = ((PubRecMessage) message).getMessageID();
                m_processor.processPubRec(clientID, messageID);
            } else if (message instanceof PubCompMessage) {
                String clientID = (String) session.getAttribute(Constants.ATTR_CLIENTID);
                int messageID = ((PubCompMessage) message).getMessageID();
                m_processor.processPubComp(clientID, messageID);
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
        m_processor.init(m_clientIDs, subscriptions, m_storageService, this);
    }


    protected void processPublish(PublishEvent evt) {
        m_processor.processPublish(evt);
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
            m_processor.notify(new NotifyEvent(newSubscription.getClientId(), storedMsg.getTopic(), storedMsg.getQos(), storedMsg.getPayload(), true));
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
            m_processor.notify(new NotifyEvent(pubEvt.getClientID(), pubEvt.getTopic(), pubEvt.getQos(),
                    pubEvt.getMessage(), false, pubEvt.getMessageID()));
        }
    }
}
