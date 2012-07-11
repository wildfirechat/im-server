package org.dna.mqtt.moquette.messaging.spi.impl;

import java.util.ArrayList;
import java.util.List;
import org.dna.mqtt.moquette.messaging.spi.IMessaging;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;

/**
 *
 * @author andrea
 */
public class SimpleMessaging implements IMessaging {
    
    private List<Subscription> subscriptions = new ArrayList<Subscription>();

    public void publish(String topic, Object message, byte qos, boolean retain) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void subscribe(String clientId, String topic, QOSType qos) {
        subscriptions.add(new Subscription(clientId, topic, qos));
    }
    
    protected List<Subscription> getSubscriptions() {
        return subscriptions;
    }
}
