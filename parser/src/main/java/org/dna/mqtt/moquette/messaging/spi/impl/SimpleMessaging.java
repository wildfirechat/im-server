package org.dna.mqtt.moquette.messaging.spi.impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.MQTTException;
import org.dna.mqtt.moquette.messaging.spi.IMatchingCondition;
import org.dna.mqtt.moquette.messaging.spi.IMessaging;
import org.dna.mqtt.moquette.messaging.spi.INotifier;
import org.dna.mqtt.moquette.messaging.spi.IStorageService;
import org.dna.mqtt.moquette.messaging.spi.impl.SubscriptionsStore.Token;
import org.dna.mqtt.moquette.messaging.spi.impl.events.*;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.server.Constants;
import org.dna.mqtt.moquette.server.Server;
import org.fusesource.hawtbuf.codec.StringCodec;
import org.fusesource.hawtdb.api.BTreeIndexFactory;
import org.fusesource.hawtdb.api.MultiIndexFactory;
import org.fusesource.hawtdb.api.PageFile;
import org.fusesource.hawtdb.api.PageFileFactory;
import org.fusesource.hawtdb.api.SortedIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
public class SimpleMessaging implements IMessaging, Runnable {

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
    
    private BlockingQueue<MessagingEvent> m_inboundQueue = new LinkedBlockingQueue<MessagingEvent>();

    private INotifier m_notifier;

    //TODO remove
    private MultiIndexFactory m_multiIndexFactory;
    private PageFileFactory pageFactory;

    //bind clientID+MsgID -> evt message published
    private SortedIndex<String, PublishEvent> m_inflightStore;

    private IStorageService m_storageService;
    
    public SimpleMessaging() {
        String storeFile = Server.STORAGE_FILE_PATH;
        
        pageFactory = new PageFileFactory();
        File tmpFile;
        try {
            tmpFile = new File(storeFile);
            tmpFile.createNewFile();
        } catch (IOException ex) {
            LOG.error(null, ex);
            throw new MQTTException("Can't create temp file for subscriptions storage [" + storeFile + "]", ex);
        }
        pageFactory.setFile(tmpFile);
        pageFactory.open();
        PageFile pageFile = pageFactory.getPageFile();
        m_multiIndexFactory = new MultiIndexFactory(pageFile);
        
        subscriptions.init(m_multiIndexFactory);
        initInflightMessageStore();

        m_storageService = new HawtDBStorageService(m_multiIndexFactory);
        m_storageService.initStore();
    }

    
    /**
     * Initialize the message store used to handle the temporary storage of QoS 1,2 
     * messages in flight.
     */
    private void initInflightMessageStore() {
        BTreeIndexFactory<String, PublishEvent> indexFactory = new BTreeIndexFactory<String, PublishEvent>();
        indexFactory.setKeyCodec(StringCodec.INSTANCE);

        m_inflightStore = (SortedIndex<String, PublishEvent>) m_multiIndexFactory.openOrCreate("inflight", indexFactory);
    }


    public void setNotifier(INotifier notifier) {
        m_notifier= notifier;
    }
    
    public void run() {
        eventLoop();
    }
    

    public void publish(String topic, byte[] message, QOSType qos, boolean retain, String clientID, IoSession session) {
        try {
            m_inboundQueue.put(new PublishEvent(topic, qos, message, retain, clientID, session));
        } catch (InterruptedException ex) {
            LOG.error(null, ex);
        }
    }

    public void publish(String topic, byte[] message, QOSType qos, boolean retain, String clientID, int messageID, IoSession session) {
        try {
            m_inboundQueue.put(new PublishEvent(topic, qos, message, retain, clientID, messageID, session));
        } catch (InterruptedException ex) {
            LOG.error(null, ex);
        }
    }

    public void subscribe(String clientId, String topic, QOSType qos, boolean cleanSession) {
        Subscription newSubscription = new Subscription(clientId, topic, qos, cleanSession);
        try {
            LOG.debug("subscribe invoked for topic: " + topic);
            m_inboundQueue.put(new SubscribeEvent(newSubscription));
        } catch (InterruptedException ex) {
            LOG.error(null, ex);
        }
    }
    
    
    public void unsubscribe(String topic, String clientID) {
        try {
            m_inboundQueue.put(new UnsubscribeEvent(topic, clientID));
        } catch (InterruptedException ex) {
            LOG.error(null, ex);
        }
    }
    
    
    public void disconnect(IoSession session) {
        try {
            m_inboundQueue.put(new DisconnectEvent(session));
        } catch (InterruptedException ex) {
            LOG.error(null, ex);
        }
    }

    //method used by hte Notifier to re-put an event on the inbound queue
    private void refill(MessagingEvent evt) throws InterruptedException {
        m_inboundQueue.put(evt);
    }

    public void republishStored(String clientID) {
        //create the event to push
        try {
            m_inboundQueue.put(new RepublishEvent(clientID));
        } catch (InterruptedException iex) {
            LOG.error(null, iex);
        }
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
        try {
            m_inboundQueue.put(new RemoveAllSubscriptionsEvent(clientID));
        } catch (InterruptedException ex) {
            LOG.error(null, ex);
        }
    }

    public void close() {
        try {
            m_inboundQueue.put(new CloseEvent());
        } catch (InterruptedException ex) {
            LOG.error(null, ex);
        } 
    }
    

    protected void eventLoop() {
        LOG.debug("Started event loop");
        boolean interrupted = false;
        while (!interrupted) {
            try { 
                MessagingEvent evt = m_inboundQueue.take();
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
                    m_inflightStore.remove(((CleanInFlightEvent)evt).getMsgId());
                } else if (evt instanceof RepublishEvent) {
                    processRepublish((RepublishEvent) evt);
                }
            } catch (InterruptedException ex) {
                processClose();
                interrupted = true;
            }
        }
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
            m_inflightStore.put(publishKey, evt);
            cleanEvt = new CleanInFlightEvent(publishKey);
        }
        
        for (final Subscription sub : subscriptions.matches(topic)) {
            if (qos == QOSType.MOST_ONE) {
                //QoS 0
                m_notifier.notify(new NotifyEvent(sub.clientId, topic, qos, message, false));
            } else {
                //QoS 1 or 2
                //if the target subscription is not clean session and is not connected => store it
                if (!sub.isCleanSession() && !sub.isActive()) {
                    m_storageService.storePublishForFuture(evt);
                }
                m_notifier.notify(new NotifyEvent(sub.clientId, topic, qos, message, false));
            }
        }

        if (cleanEvt != null) {
            refill(cleanEvt);
            m_notifier.sendPubAck(new PubAckEvent(evt.getMessageID(), evt.getClientID()));
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
                return matchTopics(key, topic);  //To change body of implemented methods use File | Settings | File Templates.
            }
        });

        for (StoredMessage storedMsg : messages) {
            //fire the as retained the message
            LOG.debug("Inserting NotifyEvent into outbound for topic " + topic);
            m_notifier.notify(new NotifyEvent(newSubscription.clientId, topic, storedMsg.getQos(),
                    storedMsg.getPayload(), true));
        }
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
        m_notifier.disconnect(evt.getSession());

        //de-activate the subscriptions for this ClientID
        String clientID = (String) evt.getSession().getAttribute(Constants.ATTR_CLIENTID);
        subscriptions.disconnect(clientID);
    }
    
    private void processClose() {
        LOG.debug("processClose invoked");
        try {
            pageFactory.close();
        } catch (IOException ex) {
            LOG.error(null, ex);
        }
    }

    private void processRepublish(RepublishEvent evt) throws InterruptedException {
        List<PublishEvent> publishedEvents = m_storageService.retrivePersistedPublishes(evt.getClientID());
        if (publishedEvents == null) {
            return;
        }

        for (PublishEvent pubEvt : publishedEvents) {
            m_notifier.notify(new NotifyEvent(pubEvt.getClientID(), pubEvt.getTopic(), pubEvt.getQos(),
                    pubEvt.getMessage(), false, pubEvt.getMessageID()));
        }
    }
}
