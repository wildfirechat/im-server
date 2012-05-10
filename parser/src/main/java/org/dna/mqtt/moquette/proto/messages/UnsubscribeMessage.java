package org.dna.mqtt.moquette.proto.messages;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author andrea
 */
public class UnsubscribeMessage extends MessageIDMessage {
    List<String> m_types = new ArrayList<String>();

    public List<String> topics() {
        return m_types;
    }

    public void addTopic(String type) {
        m_types.add(type);
    }
}
