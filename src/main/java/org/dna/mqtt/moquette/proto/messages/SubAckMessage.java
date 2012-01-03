package org.dna.mqtt.moquette.proto.messages;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author andrea
 */
public class SubAckMessage extends MessageIDMessage {

    List<QOSType> m_types = new ArrayList<QOSType>();

    public List<QOSType> types() {
        return m_types;
    }

    public void addType(QOSType type) {
        m_types.add(type);
    }
}
