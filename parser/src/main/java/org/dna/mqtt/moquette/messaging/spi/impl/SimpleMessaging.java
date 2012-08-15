package org.dna.mqtt.moquette.messaging.spi.impl;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.dna.mqtt.moquette.MQTTException;
import org.dna.mqtt.moquette.messaging.spi.IMessaging;
import org.dna.mqtt.moquette.messaging.spi.INotifier;
import org.dna.mqtt.moquette.messaging.spi.impl.SubscriptionsStore.Token;
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
public class SimpleMessaging implements IMessaging {
    
    private static class StoredMessage {
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
    
    //TODO is poorly performace on concurrent load, use at least ReadWriteLock
    
    private ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private SubscriptionsStore subscriptions = new SubscriptionsStore();
    
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
    
    public void setNotifier(INotifier notifier) {
        m_notifier = notifier;
    }

    public void publish(String topic, byte[] message, QOSType qos, boolean retain) {
        //TODO for this moment  no qos > 0 trivial implementation
        QOSType defQos = QOSType.MOST_ONE;
        
        rwLock.readLock().lock();
        try {
            for (Subscription sub : subscriptions.matches(topic)) {
                m_notifier.notify(sub.clientId, topic, defQos, message, false);
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
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void subscribe(String clientId, String topic, QOSType qos) {
        Subscription newSubscription = new Subscription(clientId, topic, qos);
        rwLock.writeLock().lock();
        subscriptions.add(newSubscription);
        
        //scans reatained messages to be published to the new subscription
        LOG.debug("Scanning all retained messages...");
        for (Map.Entry<String, StoredMessage> entry : m_retainedStore) {
            StoredMessage storedMsg = entry.getValue();
            if (matchTopics(entry.getKey(), topic)) {
                //fire the as retained the message
                m_notifier.notify(newSubscription.clientId, topic, storedMsg.getQos(), 
                        storedMsg.getPayload(), true);
            }
        }
        LOG.debug("Finished firing retained messages");
        rwLock.writeLock().unlock();
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
    
    protected SubscriptionsStore getSubscriptions() {
        return subscriptions;
    }

    public void removeSubscriptions(String clientID) {
        rwLock.writeLock().lock();
        subscriptions.removeForClient(clientID);
        rwLock.writeLock().unlock();
    }

    public void close() {
        rwLock.writeLock().lock();
        try {
            pageFactory.close();
        } catch (IOException ex) {
            LOG.error(null, ex);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
