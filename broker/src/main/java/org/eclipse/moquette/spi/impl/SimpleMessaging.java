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
package org.eclipse.moquette.spi.impl;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.moquette.spi.IMessaging;
import org.eclipse.moquette.spi.ISessionsStore;
import org.eclipse.moquette.spi.IMessagesStore;
import org.eclipse.moquette.spi.impl.events.*;
import org.eclipse.moquette.spi.impl.subscriptions.SubscriptionsStore;
import org.eclipse.moquette.spi.persistence.MapDBPersistentStore;
import org.eclipse.moquette.proto.messages.*;
import org.eclipse.moquette.server.IAuthenticator;
import org.eclipse.moquette.server.ServerChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Singleton class that orchestrate the execution of the protocol.
 *
 * Uses the LMAX Disruptor to serialize the incoming, requests, because it work in a evented fashion;
 * the requests income from front Netty connectors and are dispatched to the 
 * ProtocolProcessor.
 *
 * @author andrea
 */
public class SimpleMessaging implements IMessaging, EventHandler<ValueEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleMessaging.class);
    
    private SubscriptionsStore subscriptions;
    
    private RingBuffer<ValueEvent> m_ringBuffer;

    private IMessagesStore m_storageService;
    private ISessionsStore m_sessionsStore;

    private ExecutorService m_executor;
    private Disruptor<ValueEvent> m_disruptor;

    private static SimpleMessaging INSTANCE;
    
    private final ProtocolProcessor m_processor = new ProtocolProcessor();
    private final AnnotationSupport annotationSupport = new AnnotationSupport();
    
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
        m_disruptor = new Disruptor<ValueEvent>(ValueEvent.EVENT_FACTORY, 1024 * 32, m_executor);
        /*Disruptor<ValueEvent> m_disruptor = new Disruptor<ValueEvent>(ValueEvent.EVENT_FACTORY, 1024 * 32, m_executor,
                ProducerType.MULTI, new BusySpinWaitStrategy());*/
        m_disruptor.handleEventsWith(this);
        m_disruptor.start();

        // Get the ring buffer from the Disruptor to be used for publishing.
        m_ringBuffer = m_disruptor.getRingBuffer();

        annotationSupport.processAnnotations(m_processor);
        processInit(configProps);
//        disruptorPublish(new InitEvent(configProps));
    }

    
    private void disruptorPublish(MessagingEvent msgEvent) {
        LOG.debug("disruptorPublish publishing event {}", msgEvent);
        long sequence = m_ringBuffer.next();
        ValueEvent event = m_ringBuffer.get(sequence);

        event.setEvent(msgEvent);
        
        m_ringBuffer.publish(sequence); 
    }
    
    @Override
    public void lostConnection(String clientID) {
        disruptorPublish(new LostConnectionEvent(clientID));
    }

    @Override
    public void handleProtocolMessage(ServerChannel session, AbstractMessage msg) {
        disruptorPublish(new ProtocolEvent(session, msg));
    }

    @Override
    public void stop() {
        m_stopLatch = new CountDownLatch(1);
        disruptorPublish(new StopEvent());
        try {
            //wait the callback notification from the protocol processor thread
            LOG.debug("waiting 10 sec to m_stopLatch");
            boolean elapsed = !m_stopLatch.await(10, TimeUnit.SECONDS);
            LOG.debug("after m_stopLatch");
            m_executor.shutdown();
            m_disruptor.shutdown();
            if (elapsed) {
                LOG.error("Can't stop the server in 10 seconds");
            }
        } catch (InterruptedException ex) {
            LOG.error(null, ex);
        }
    }
    
    @Override
    public void onEvent(ValueEvent t, long l, boolean bln) throws Exception {
        MessagingEvent evt = t.getEvent();
        LOG.info("onEvent processing messaging event from input ringbuffer {}", evt);
        if (evt instanceof StopEvent) {
            processStop();
            return;
        } 
        if (evt instanceof LostConnectionEvent) {
            LostConnectionEvent lostEvt = (LostConnectionEvent) evt;
            m_processor.processConnectionLost(lostEvt.getClientID());
            return;
        }
        
        if (evt instanceof ProtocolEvent) {
            ServerChannel session = ((ProtocolEvent) evt).getSession();
            AbstractMessage message = ((ProtocolEvent) evt).getMessage();
            annotationSupport.dispatch(session, message);
        }
    }

    private void processInit(Properties props) {
        //TODO use a property to select the storage path
        MapDBPersistentStore mapStorage = new MapDBPersistentStore();
        m_storageService = mapStorage;
        m_sessionsStore = mapStorage;

        m_storageService.initStore();
        
        //List<Subscription> storedSubscriptions = m_sessionsStore.listAllSubscriptions();
        //subscriptions.init(storedSubscriptions);
        subscriptions.init(m_sessionsStore);
        
        String passwdPath = props.getProperty("password_file", "");
        String configPath = System.getProperty("moquette.path", null);
        IAuthenticator authenticator;
        if (passwdPath.isEmpty()) {
            authenticator = new AcceptAllAuthenticator();
        } else {
            authenticator = new FileAuthenticator(configPath, passwdPath);
        }
        
        m_processor.init(subscriptions, m_storageService, m_sessionsStore, authenticator);
    }


    private void processStop() {
        LOG.debug("processStop invoked");
        m_storageService.close();
        LOG.debug("subscription tree {}", subscriptions.dumpTree());
//        m_eventProcessor.halt();
//        m_executor.shutdown();
        
        subscriptions = null;
        m_stopLatch.countDown();
    }
}
