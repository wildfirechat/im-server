package org.dna.mqtt.moquette.messaging.spi.impl;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.dna.mqtt.moquette.messaging.spi.IMessaging;
import org.dna.mqtt.moquette.messaging.spi.IStorageService;
import org.dna.mqtt.moquette.messaging.spi.impl.events.*;
import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.SubscriptionsStore;
import org.dna.mqtt.moquette.proto.messages.PubCompMessage;
import org.dna.mqtt.moquette.proto.messages.*;
import org.dna.mqtt.moquette.server.Constants;
import org.dna.mqtt.moquette.server.ServerChannel;
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

    private IStorageService m_storageService;

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
        //TODO in a presentation is said to don't do the followinf line!!
        m_ringBuffer.setGatingSequences(m_eventProcessor.getSequence());
        m_executor.submit(m_eventProcessor);

        disruptorPublish(new InitEvent());
    }

    
    private void disruptorPublish(MessagingEvent msgEvent) {
        LOG.debug("disruptorPublish publishing event {}", msgEvent);
        long sequence = m_ringBuffer.next();
        ValueEvent event = m_ringBuffer.get(sequence);

        event.setEvent(msgEvent);
        
        m_ringBuffer.publish(sequence); 
    }
    

    public void disconnect(ServerChannel session) {
        disruptorPublish(new DisconnectEvent(session));
    }
    
    public void lostConnection(String clientID) {
        disruptorPublish(new LostConnectionEvent(clientID));
    }

    public void handleProtocolMessage(ServerChannel session, AbstractMessage msg) {
        disruptorPublish(new ProtocolEvent(session, msg));
    }

    public void stop() {
        disruptorPublish(new StopEvent());
    }
    
    public void onEvent(ValueEvent t, long l, boolean bln) throws Exception {
        MessagingEvent evt = t.getEvent();
        LOG.info("onEvent processing messaging event from input ringbuffer {}", evt);
        if (evt instanceof PublishEvent) {
            m_processor.processPublish((PublishEvent) evt);
        } else if (evt instanceof StopEvent) {
            processStop();
        } else if (evt instanceof DisconnectEvent) {
            DisconnectEvent disEvt = (DisconnectEvent) evt;
            String clientID = (String) disEvt.getSession().getAttribute(Constants.ATTR_CLIENTID);
            m_processor.processDisconnect(disEvt.getSession(), clientID, false);
        } else if (evt instanceof ProtocolEvent) {
            ServerChannel session = ((ProtocolEvent) evt).getSession();
            AbstractMessage message = ((ProtocolEvent) evt).getMessage();
            if (message instanceof ConnectMessage) {
                m_processor.processConnect(session, (ConnectMessage) message);
            } else if (message instanceof  PublishMessage) {
                PublishEvent pubEvt;
                String clientID = (String) session.getAttribute(Constants.ATTR_CLIENTID);
                pubEvt = new PublishEvent((PublishMessage) message, clientID, session);
//                if (message.getQos() == QOSType.MOST_ONE) {
//                    pubEvt = new PublishEvent(pubMsg.getTopicName(), pubMsg.getQos(), pubMsg.getPayload(), pubMsg.isRetainFlag(), clientID, session);
//
//                } else {
//                    pubEvt = new PublishEvent(pubMsg.getTopicName(), pubMsg.getQos(), pubMsg.getPayload(), pubMsg.isRetainFlag(), clientID, pubMsg.getMessageID(), session);
//                }
                m_processor.processPublish(pubEvt);
            } else if (message instanceof DisconnectMessage) {
                String clientID = (String) session.getAttribute(Constants.ATTR_CLIENTID);
                boolean cleanSession = (Boolean) session.getAttribute(Constants.CLEAN_SESSION);

                //close the TCP connection
                //session.close(true);
                m_processor.processDisconnect(session, clientID, cleanSession);
            } else if (message instanceof UnsubscribeMessage) {
                UnsubscribeMessage unsubMsg = (UnsubscribeMessage) message;
                String clientID = (String) session.getAttribute(Constants.ATTR_CLIENTID);
                m_processor.processUnsubscribe(session, clientID, unsubMsg.topics(), unsubMsg.getMessageID());
            } else if (message instanceof SubscribeMessage) {
                String clientID = (String) session.getAttribute(Constants.ATTR_CLIENTID);
                boolean cleanSession = (Boolean) session.getAttribute(Constants.CLEAN_SESSION);
                m_processor.processSubscribe(session, (SubscribeMessage) message, clientID, cleanSession);
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
        } else if (evt instanceof LostConnectionEvent) {
            LostConnectionEvent lostEvt = (LostConnectionEvent) evt;
            m_processor.proccessConnectionLost(lostEvt.getClientID());
        }
    }

    private void processInit() {
        m_storageService = new HawtDBStorageService();
        m_storageService.initStore();

        subscriptions.init(m_storageService);
        m_processor.init(subscriptions, m_storageService);
    }


    private void processStop() {
        LOG.debug("processStop invoked");
        m_storageService.close();

//        m_eventProcessor.halt();
        m_executor.shutdown();
    }
}
