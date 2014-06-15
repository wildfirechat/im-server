/*
 * Copyright (c) 2012-2014 The original author or authors
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
package org.dna.mqtt.moquette.messaging.spi.impl;

import com.lmax.disruptor.BatchEventProcessor;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SequenceBarrier;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.dna.mqtt.moquette.messaging.spi.IMessaging;
import org.dna.mqtt.moquette.messaging.spi.IStorageService;
import org.dna.mqtt.moquette.messaging.spi.impl.events.*;
import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.SubscriptionsStore;
import org.dna.mqtt.moquette.proto.messages.PubCompMessage;
import org.dna.mqtt.moquette.proto.messages.*;
import org.dna.mqtt.moquette.server.Constants;
import org.dna.mqtt.moquette.server.IAuthenticator;
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
    
    private SubscriptionsStore subscriptions;
    
    private RingBuffer<ValueEvent> m_ringBuffer;

    private IStorageService m_storageService;

    private ExecutorService m_executor;
    BatchEventProcessor<ValueEvent> m_eventProcessor;

    private static SimpleMessaging INSTANCE;
    
    private ProtocolProcessor m_processor = new ProtocolProcessor();
    
    CountDownLatch m_stopLatch;
    
    private SimpleMessaging() {
    }

    public static SimpleMessaging getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SimpleMessaging();
        }
        return INSTANCE;
    }

    public void init(Properties configProps) {
        subscriptions = new SubscriptionsStore();
        m_executor = Executors.newFixedThreadPool(1);

        m_ringBuffer = new RingBuffer<ValueEvent>(ValueEvent.EVENT_FACTORY, 1024 * 32);

        SequenceBarrier barrier = m_ringBuffer.newBarrier();
        m_eventProcessor = new BatchEventProcessor<ValueEvent>(m_ringBuffer, barrier, this);
        //TODO in a presentation is said to don't do the followinf line!!
        m_ringBuffer.setGatingSequences(m_eventProcessor.getSequence());
        m_executor.submit(m_eventProcessor);

        disruptorPublish(new InitEvent(configProps));
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
        m_stopLatch = new CountDownLatch(1);
        disruptorPublish(new StopEvent());
        try {
            //wait the callback notification from the protocol processor thread
            boolean elapsed = !m_stopLatch.await(10, TimeUnit.SECONDS);
            if (elapsed) {
                LOG.error("Can't stop the server in 10 seconds");
            }
        } catch (InterruptedException ex) {
            LOG.error(null, ex);
        }
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
                pubEvt = new PublishEvent((PublishMessage) message, clientID);
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
            } else if (message instanceof PubAckMessage) {
                String clientID = (String) session.getAttribute(Constants.ATTR_CLIENTID);
                int messageID = ((PubAckMessage) message).getMessageID();
                m_processor.processPubAck(clientID, messageID);
            } else {
                throw new RuntimeException("Illegal message received " + message);
            }

        } else if (evt instanceof InitEvent) {
            processInit(((InitEvent) evt).getConfig());
        } else if (evt instanceof LostConnectionEvent) {
            LostConnectionEvent lostEvt = (LostConnectionEvent) evt;
            m_processor.proccessConnectionLost(lostEvt.getClientID());
        }
    }

    private void processInit(Properties props) {
        m_storageService = new HawtDBStorageService();
        m_storageService.initStore();

        subscriptions.init(m_storageService);
        
        String passwdPath = props.getProperty("password_file");
        String configPath = System.getProperty("moquette.path", null);
        IAuthenticator authenticator = new FileAuthenticator(configPath, passwdPath);
        
        m_processor.init(subscriptions, m_storageService, authenticator);
    }


    private void processStop() {
        LOG.debug("processStop invoked");
        m_storageService.close();

//        m_eventProcessor.halt();
        m_executor.shutdown();
        
        subscriptions = null;
        m_stopLatch.countDown();
    }
}
