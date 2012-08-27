package org.dna.mqtt.moquette.messaging.spi.impl;

import org.dna.mqtt.moquette.messaging.spi.impl.events.RemoveAllSubscriptionsEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.events.MessagingEvent;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.dna.mqtt.moquette.MQTTException;
import org.dna.mqtt.moquette.messaging.spi.IMessaging;
import org.dna.mqtt.moquette.messaging.spi.INotifier;
import org.dna.mqtt.moquette.messaging.spi.impl.SubscriptionsStore.Token;
import org.dna.mqtt.moquette.messaging.spi.impl.events.CloseEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.events.PublishEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.events.SubscribeEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.events.UnsubscribeEvent;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
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
    
    private INotifier m_notifier;
    private MultiIndexFactory m_multiIndexFactory;
    private PageFileFactory pageFactory;
    private SortedIndex<String, StoredMessage> m_retainedStore;
    
    
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
    }
    
    private void initRetainedMessageStore() {
        BTreeIndexFactory<String, StoredMessage> indexFactory = new BTreeIndexFactory<String, StoredMessage>();
        indexFactory.setKeyCodec(StringCodec.INSTANCE);

//        m_persistent = indexFactory.openOrCreate(pageFile);
        m_retainedStore = (SortedIndex<String, StoredMessage>) m_multiIndexFactory.openOrCreate("retained", indexFactory);
    }
    
    public void run() {
        eventLoop();
    }
    
    public void setNotifier(INotifier notifier) {
        m_notifier = notifier;
    }

    public void publish(String topic, byte[] message, QOSType qos, boolean retain) {
        //TODO for this moment  no qos > 0 trivial implementation
        QOSType defQos = QOSType.MOST_ONE;
        
        try {
            m_inboundQueue.put(new PublishEvent(topic, defQos, message, retain));
        } catch (InterruptedException ex) {
            LOG.error(null, ex);
        }
    }

    public void subscribe(String clientId, String topic, QOSType qos) {
        Subscription newSubscription = new Subscription(clientId, topic, qos);
        try {
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
                }
            } catch (InterruptedException ex) {
                processClose();
                interrupted = true;
            }
        }
    }
    
    protected void processPublish(PublishEvent evt) {
        LOG.debug("processPublish invoked");
        String topic = evt.getTopic();
        QOSType qos = evt.getQos();
        byte[] message = evt.getMessage();
        boolean retain = evt.isRetain();
        
        for (Subscription sub : subscriptions.matches(topic)) {
            m_notifier.notify(sub.clientId, topic, qos, message, false);
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
    
    
    protected void processSubscribe(SubscribeEvent evt) {
        LOG.debug("processSubscribe invoked");
        Subscription newSubscription = evt.getSubscription();
        String topic = newSubscription.getTopic();
        
        subscriptions.add(newSubscription);

        //scans retained messages to be published to the new subscription
        LOG.debug("Scanning all retained messages...");
        for (Map.Entry<String, StoredMessage> entry : m_retainedStore) {
            StoredMessage storedMsg = entry.getValue();
            if (matchTopics(entry.getKey(), topic)) {
                //fire the as retained the message
                m_notifier.notify(newSubscription.clientId, topic, storedMsg.getQos(), 
                        storedMsg.getPayload(), true);
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
