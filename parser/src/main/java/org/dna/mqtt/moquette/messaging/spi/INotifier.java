package org.dna.mqtt.moquette.messaging.spi;

import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;

/**
 *
 * @author andrea
 */
public interface INotifier {

    public void notify(String clientId, String topic, QOSType qOSType, byte[] payload);
    
}
