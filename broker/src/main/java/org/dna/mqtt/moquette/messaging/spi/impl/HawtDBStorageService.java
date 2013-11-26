package org.dna.mqtt.moquette.messaging.spi.impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.*;
import org.dna.mqtt.moquette.MQTTException;
import org.dna.mqtt.moquette.messaging.spi.IMatchingCondition;
import org.dna.mqtt.moquette.messaging.spi.IStorageService;
import org.dna.mqtt.moquette.messaging.spi.impl.events.PublishEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.storage.StoredPublishEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.subscriptions.Subscription;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.server.Server;
import org.fusesource.hawtbuf.codec.StringCodec;
import org.fusesource.hawtdb.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation of IStorageService backed by HawtDB
 */
public class HawtDBStorageService implements IStorageService {

    public static class StoredMessage implements Serializable {
        AbstractMessage.QOSType m_qos;
        byte[] m_payload;
        String m_topic;

        StoredMessage(byte[] message, AbstractMessage.QOSType qos, String topic) {
            m_qos = qos;
            m_payload = message;
            m_topic = topic;
        }

        AbstractMessage.QOSType getQos() {
            return m_qos;
        }

        ByteBuffer getPayload() {
            return (ByteBuffer) ByteBuffer.allocate(m_payload.length).put(m_payload).flip();
        }
        
        String getTopic() {
            return m_topic;
        }
    }


    private static final Logger LOG = LoggerFactory.getLogger(HawtDBStorageService.class);

    private MultiIndexFactory m_multiIndexFactory;
    private PageFileFactory pageFactory;

    //maps clientID to the list of pending messages stored
    private SortedIndex<String, List<StoredPublishEvent>> m_persistentMessageStore;
    private SortedIndex<String, StoredMessage> m_retainedStore;
    //bind clientID+MsgID -> evt message published
    private SortedIndex<String, StoredPublishEvent> m_inflightStore;
    //bind clientID+MsgID -> evt message published
    private SortedIndex<String, StoredPublishEvent> m_qos2Store;

    //persistent Map of clientID, set of Subscriptions
    private SortedIndex<String, Set<Subscription>> m_persistentSubscriptions;

    public HawtDBStorageService() {
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
    }


    public void initStore() {
        initRetainedStore();
        //init the message store for QoS 1/2 messages in clean sessions
        initPersistentMessageStore();
        initInflightMessageStore();
        initPersistentSubscriptions();
        initPersistentQoS2MessageStore();
    }

    private void initRetainedStore() {
        BTreeIndexFactory<String, StoredMessage> indexFactory = new BTreeIndexFactory<String, StoredMessage>();
        indexFactory.setKeyCodec(StringCodec.INSTANCE);

        m_retainedStore = (SortedIndex<String, StoredMessage>) m_multiIndexFactory.openOrCreate("retained", indexFactory);
    }


    private void initPersistentMessageStore() {
        BTreeIndexFactory<String, List<StoredPublishEvent>> indexFactory = new BTreeIndexFactory<String, List<StoredPublishEvent>>();
        indexFactory.setKeyCodec(StringCodec.INSTANCE);

        m_persistentMessageStore = (SortedIndex<String, List<StoredPublishEvent>>) m_multiIndexFactory.openOrCreate("persistedMessages", indexFactory);
    }

    private void initPersistentSubscriptions() {
        BTreeIndexFactory<String, Set<Subscription>> indexFactory = new BTreeIndexFactory<String, Set<Subscription>>();
        indexFactory.setKeyCodec(StringCodec.INSTANCE);

        m_persistentSubscriptions = (SortedIndex<String, Set<Subscription>>) m_multiIndexFactory.openOrCreate("subscriptions", indexFactory);
    }

    /**
     * Initialize the message store used to handle the temporary storage of QoS 1,2
     * messages in flight.
     */
    private void initInflightMessageStore() {
        BTreeIndexFactory<String, StoredPublishEvent> indexFactory = new BTreeIndexFactory<String, StoredPublishEvent>();
        indexFactory.setKeyCodec(StringCodec.INSTANCE);

        m_inflightStore = (SortedIndex<String, StoredPublishEvent>) m_multiIndexFactory.openOrCreate("inflight", indexFactory);
    }

    private void initPersistentQoS2MessageStore() {
        BTreeIndexFactory<String, StoredPublishEvent> indexFactory = new BTreeIndexFactory<String, StoredPublishEvent>();
        indexFactory.setKeyCodec(StringCodec.INSTANCE);

        m_qos2Store = (SortedIndex<String, StoredPublishEvent>) m_multiIndexFactory.openOrCreate("qos2Store", indexFactory);
    }

    public void storeRetained(String topic, ByteBuffer message, AbstractMessage.QOSType qos) {
        if (!message.hasRemaining()) {
            //clean the message from topic
            m_retainedStore.remove(topic);
        } else {
            //store the message to the topic
            byte[] raw = new byte[message.remaining()];
            message.get(raw);
            m_retainedStore.put(topic, new StoredMessage(raw, qos, topic));
        }
    }

    public Collection<StoredMessage> searchMatching(IMatchingCondition condition) {
        LOG.debug("searchMatching scanning all retained messages, presents are " + m_retainedStore.size());

        List<StoredMessage> results = new ArrayList<StoredMessage>();

        for (Map.Entry<String, StoredMessage> entry : m_retainedStore) {
            StoredMessage storedMsg = entry.getValue();
            if (condition.match(entry.getKey())) {
                results.add(storedMsg);
            }
        }

        return results;
    }

    public void storePublishForFuture(PublishEvent evt) {
        List<StoredPublishEvent> storedEvents;
        String clientID = evt.getClientID();
        if (!m_persistentMessageStore.containsKey(clientID)) {
            storedEvents = new ArrayList<StoredPublishEvent>();
        } else {
            storedEvents = m_persistentMessageStore.get(clientID);
        }
        storedEvents.add(convertToStored(evt));
        m_persistentMessageStore.put(clientID, storedEvents);
        //NB rewind the evt message content
        LOG.debug(String.format("Stored published message for client <%s> on topic <%s>", clientID, evt.getTopic()));
    }

    public List<PublishEvent> retrivePersistedPublishes(String clientID) {
        List<StoredPublishEvent> storedEvts = m_persistentMessageStore.get(clientID);
        if (storedEvts == null) {
            return null;
        }
        List<PublishEvent> liveEvts = new ArrayList<PublishEvent>();
        for (StoredPublishEvent storedEvt : storedEvts) {
            liveEvts.add(convertFromStored(storedEvt));
        }
        return liveEvts;
    }

    public void cleanPersistedPublishes(String clientID) {
        m_persistentMessageStore.remove(clientID);
    }

    public void cleanInFlight(String msgID) {
        m_inflightStore.remove(msgID);
    }

    public void addInFlight(PublishEvent evt, String publishKey) {
        StoredPublishEvent storedEvt = convertToStored(evt);
        m_inflightStore.put(publishKey, storedEvt);
    }

    public void addNewSubscription(Subscription newSubscription, String clientID) {
        if (!m_persistentSubscriptions.containsKey(clientID)) {
            m_persistentSubscriptions.put(clientID, new HashSet<Subscription>());
        }

        Set<Subscription> subs = m_persistentSubscriptions.get(clientID);
        if (!subs.contains(newSubscription)) {
            subs.add(newSubscription);
            m_persistentSubscriptions.put(clientID, subs);
        }
    }

    public void removeAllSubscriptions(String clientID) {
        m_persistentSubscriptions.remove(clientID);
    }

    public List<Subscription> retrieveAllSubscriptions() {
        List<Subscription> allSubscriptions = new ArrayList<Subscription>();
        for (Map.Entry<String, Set<Subscription>> entry : m_persistentSubscriptions) {
            allSubscriptions.addAll(entry.getValue());
        }
        return allSubscriptions;
    }

    public void close() {
        try {
            pageFactory.close();
        } catch (IOException ex) {
            LOG.error(null, ex);
        }
    }

    /*-------- QoS 2  storage management --------------*/
    public void persistQoS2Message(String publishKey, PublishEvent evt) {
        LOG.debug(String.format("persistQoS2Message store pubKey %s, evt %s", publishKey, evt));
        m_qos2Store.put(publishKey, convertToStored(evt));
    }

    public void removeQoS2Message(String publishKey) {
        m_qos2Store.remove(publishKey);
    }

    public PublishEvent retrieveQoS2Message(String publishKey) {
        StoredPublishEvent storedEvt = m_qos2Store.get(publishKey);
        return convertFromStored(storedEvt);
    }
    
    private StoredPublishEvent convertToStored(PublishEvent evt) {
        StoredPublishEvent storedEvt = new StoredPublishEvent(evt);
        return storedEvt;
    }
    
    private PublishEvent convertFromStored(StoredPublishEvent evt) {
        byte[] message = evt.getMessage();
        ByteBuffer bbmessage = ByteBuffer.wrap(message);
        //bbmessage.flip();
        PublishEvent liveEvt = new PublishEvent(evt.getTopic(), evt.getQos(), 
                bbmessage, evt.isRetain(), evt.getClientID(), evt.getMessageID(), null);
        return liveEvt;
    }
}
