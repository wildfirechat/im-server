package org.dna.mqtt.moquette.messaging.spi.impl;

import org.dna.mqtt.moquette.messaging.spi.IMatchingCondition;
import org.dna.mqtt.moquette.messaging.spi.IStorageService;
import org.dna.mqtt.moquette.messaging.spi.impl.events.PublishEvent;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.fusesource.hawtbuf.codec.StringCodec;
import org.fusesource.hawtdb.api.BTreeIndexFactory;
import org.fusesource.hawtdb.api.MultiIndexFactory;
import org.fusesource.hawtdb.api.PageFileFactory;
import org.fusesource.hawtdb.api.SortedIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.dna.mqtt.moquette.messaging.spi.impl.SimpleMessaging.StoredMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * Implementation of IStorageService backed by HawtDB
 */
public class HawtDBStorageService implements IStorageService {

    private static final Logger LOG = LoggerFactory.getLogger(HawtDBStorageService.class);

    private MultiIndexFactory m_multiIndexFactory;
    private PageFileFactory pageFactory;

    private SortedIndex<String, StoredMessage> m_retainedStore;

    public HawtDBStorageService(MultiIndexFactory multiIndexFactory) {
        m_multiIndexFactory =  multiIndexFactory;
    }


    public void initStore() {
        BTreeIndexFactory<String, StoredMessage> indexFactory = new BTreeIndexFactory<String, StoredMessage>();
        indexFactory.setKeyCodec(StringCodec.INSTANCE);

        m_retainedStore = (SortedIndex<String, StoredMessage>) m_multiIndexFactory.openOrCreate("retained", indexFactory);

    }

    public void storeRetained(String topic, byte[] message, AbstractMessage.QOSType qos) {
        if (message.length == 0) {
            //clean the message from topic
            m_retainedStore.remove(topic);
        } else {
            //store the message to the topic
            m_retainedStore.put(topic, new StoredMessage(message, qos));
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
}
