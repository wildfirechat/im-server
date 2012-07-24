package org.dna.mqtt.moquette.messaging.spi.impl;

import java.util.ArrayList;
import java.util.List;
import org.dna.mqtt.moquette.messaging.spi.IMessaging;
import org.dna.mqtt.moquette.messaging.spi.INotifier;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;

/**
 *
 * @author andrea
 */
public class SimpleMessaging implements IMessaging {
    //TODO is poorly performace on concurrent load, use at least ReadWriteLock
    private List<Subscription> subscriptions = new ArrayList<Subscription>();
    
    private INotifier m_notifier;
    
    public void setNotifier(INotifier notifier) {
        m_notifier = notifier;
    }

    public void publish(String topic, byte[] message, QOSType qos, boolean retain) {
        //TODO for this moment no retain management and no qos > 0
        //trivial implementation
        QOSType defQos = QOSType.MOST_ONE;
        synchronized(subscriptions) {
            for (Subscription sub : subscriptions) {
                if (sub.match(topic)) {
                    m_notifier.notify(sub.clientId, topic, defQos, message);
                }
            }
        }
    }

    public void subscribe(String clientId, String topic, QOSType qos) {
        Subscription newSubscription = new Subscription(clientId, topic, qos);
        synchronized(subscriptions) {
            if (subscriptions.contains(newSubscription)) {
                return;
            }
            subscriptions.add(newSubscription);
        }
    }
    
    protected List<Subscription> getSubscriptions() {
        return subscriptions;
    }
}
