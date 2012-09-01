package org.dna.mqtt.moquette.messaging.spi.impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.MQTTException;
import org.dna.mqtt.moquette.messaging.spi.IMessaging;
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

    private static class StoredMessage implements Serializable {
        QOSType m_qos;
        byte[] m_payload;

        private StoredMessage(byte[] message, QOSType qos) {
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
    private BlockingQueue<MessagingEvent> m_outboundQueue = new LinkedBlockingQueue<MessagingEvent>();
    
//    private INotifier m_notifier;
    private MultiIndexFactory m_multiIndexFactory;
    private PageFileFactory pageFactory;
    private SortedIndex<String, StoredMessage> m_retainedStore;
    //bind clientID+MsgID -> evt message published
    private SortedIndex<String, PublishEvent> m_inflightStore;
    
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
        initRetainedMessageStore();
        initPersistentMessageStore();
    }
    
    private void initRetainedMessageStore() {
        BTreeIndexFactory<String, StoredMessage> indexFactory = new BTreeIndexFactory<String, StoredMessage>();
        indexFactory.setKeyCodec(StringCodec.INSTANCE);

//        m_persistent = indexFactory.openOrCreate(pageFile);
        m_retainedStore = (SortedIndex<String, StoredMessage>) m_multiIndexFactory.openOrCreate("retained", indexFactory);
    }
    
    /**
     * Initialize the message store used to handle the temporary storage of QoS 1,2 
     * messages.
     */
    private void initPersistentMessageStore() {
        BTreeIndexFactory<String, PublishEvent> indexFactory = new BTreeIndexFactory<String, PublishEvent>();
        indexFactory.setKeyCodec(StringCodec.INSTANCE);

        m_inflightStore = (SortedIndex<String, PublishEvent>) m_multiIndexFactory.openOrCreate("inflight", indexFactory);
    }
    
    public void run() {
        eventLoop();
    }
    

    public void publish(String topic, byte[] message, QOSType qos, boolean retain, String clientID, IoSession session) {
        //TODO for this moment  no qos > 0 trivial implementation
//        QOSType defQos = QOSType.MOST_ONE;
        
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

    public void subscribe(String clientId, String topic, QOSType qos) {
        Subscription newSubscription = new Subscription(clientId, topic, qos);
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
    public void refill(MessagingEvent evt) throws InterruptedException {
        m_inboundQueue.put(evt);
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
    
    public BlockingQueue<MessagingEvent> getNotifyEventQueue() {
        return m_outboundQueue;
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
                    m_outboundQueue.put(evt);
                } else if (evt instanceof CleanInFlightEvent) {
                    //remove the message from inflight storage
                    m_inflightStore.remove(((CleanInFlightEvent)evt).getMsgId());

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
                m_outboundQueue.put(new NotifyEvent(sub.clientId, topic, qos, message, false));
            } else {
                //QoS 1 or 2
                //if the target subscription is not clean session and is not connected => store it
                m_outboundQueue.put(new NotifyEvent(sub.clientId, topic, qos, message, false, evt.getMessageID()));
            }
        }

        if (cleanEvt != null) {
            m_outboundQueue.put(cleanEvt);
            m_outboundQueue.put(new PubAckEvent(evt.getMessageID(), evt.getClientID()));
        }

        if (retain) {
            if (message.length == 0) {
                //clean the message from topic
                m_retainedStore.remove(topic);
            } else {    
                //store the message to the topic
                m_retainedStore.put(topic, new StoredMessage(message, qos));
            }
        }
    }


    protected void processSubscribe(SubscribeEvent evt)/* throws InterruptedException*/ {
        LOG.debug("processSubscribe invoked");
        Subscription newSubscription = evt.getSubscription();
        String topic = newSubscription.getTopic();
        
        subscriptions.add(newSubscription);

        //scans retained messages to be published to the new subscription
        LOG.debug("Scanning all retained messages, presents are " + m_retainedStore.size());
        for (Map.Entry<String, StoredMessage> entry : m_retainedStore) {
            StoredMessage storedMsg = entry.getValue();
            if (matchTopics(entry.getKey(), topic)) {
                try {
                    //fire the as retained the message
                    LOG.debug("Inserting NotifyEvent into outbound for topic " + topic + " entry " + entry.getKey());
                    m_outboundQueue.put(new NotifyEvent(newSubscription.clientId,
                            topic, storedMsg.getQos(), storedMsg.getPayload(), true));
                } catch (InterruptedException ex) {
                    LOG.error(null, ex);
                }
            }
        }
    }
    
    protected void processUnsubscribe(UnsubscribeEvent evt) {
        LOG.debug("processSubscribe invoked");
        subscriptions.removeSubscription(evt.getTopic(), evt.getClientID());
    }
    
    protected void processRemoveAllSubscriptions(RemoveAllSubscriptionsEvent evt) {
        LOG.debug("processRemoveAllSubscriptions invoked");
        subscriptions.removeForClient(evt.getClientID());
    }
    
    private void processClose() {
        LOG.debug("processClose invoked");
        try {
            pageFactory.close();
        } catch (IOException ex) {
            LOG.error(null, ex);
        }
    }
}
