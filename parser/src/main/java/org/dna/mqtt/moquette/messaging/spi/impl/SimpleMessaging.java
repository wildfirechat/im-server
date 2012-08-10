package org.dna.mqtt.moquette.messaging.spi.impl;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.dna.mqtt.moquette.messaging.spi.IMessaging;
import org.dna.mqtt.moquette.messaging.spi.INotifier;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;

/**
 *
 * @author andrea
 */
public class SimpleMessaging implements IMessaging {
    //TODO is poorly performace on concurrent load, use at least ReadWriteLock
    
    private ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private SubscriptionsStore subscriptions = new SubscriptionsStore();
    
    private INotifier m_notifier;
    
    public void setNotifier(INotifier notifier) {
        m_notifier = notifier;
    }

    public void publish(String topic, byte[] message, QOSType qos, boolean retain) {
        //TODO for this moment no retain management and no qos > 0
        //trivial implementation
        QOSType defQos = QOSType.MOST_ONE;
        
        rwLock.readLock().lock();
        try {
            for (Subscription sub : subscriptions.matches(topic)) {
                m_notifier.notify(sub.clientId, topic, defQos, message);
            }
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void subscribe(String clientId, String topic, QOSType qos) {
        Subscription newSubscription = new Subscription(clientId, topic, qos);
        rwLock.writeLock().lock();
        subscriptions.add(newSubscription);
        rwLock.writeLock().unlock();
    }
    
    protected SubscriptionsStore getSubscriptions() {
        return subscriptions;
    }

    public void removeSubscriptions(String clientID) {
        rwLock.writeLock().lock();
        subscriptions.removeForClient(clientID);
        rwLock.writeLock().unlock();
    }
}
