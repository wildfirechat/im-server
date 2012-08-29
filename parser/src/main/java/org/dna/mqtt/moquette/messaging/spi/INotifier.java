package org.dna.mqtt.moquette.messaging.spi;

import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;

/**
 *
 * @author andrea
 */
public interface INotifier {

    public void notify(String clientId, String topic, QOSType qOSType, byte[] payload, boolean retained);

    public void disconnect(IoSession session);
    
}
