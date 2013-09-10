package org.dna.mqtt.moquette.proto.messages;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author andrea
 */
public class SubscribeMessage extends MessageIDMessage {

    public static class Couple {

        private byte m_qos;
        private String m_topic;

        public Couple(byte qos, String topic) {
            m_qos = qos;
            m_topic = topic;
        }
        
        public byte getQos() {
            return m_qos;
        }

        public String getTopic() {
            return m_topic;
        }
    }
    private List<Couple> m_subscriptions = new ArrayList<Couple>();

    public SubscribeMessage() {
        //Subscribe has always QoS 1
        m_qos = AbstractMessage.QOSType.LEAST_ONE;
    }
    
    public List<Couple> subscriptions() {
        return m_subscriptions;
    }

    public void addSubscription(Couple subscription) {
        m_subscriptions.add(subscription);
    }
}
