package org.dna.mqtt.moquette.messaging.spi;

import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.messaging.spi.impl.events.NotifyEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.events.PubAckEvent;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.proto.messages.PubAckMessage;

/**
 *
 * @author andrea
 */
public interface INotifier {

    public void notify(NotifyEvent evt/*String clientId, String topic, QOSType qOSType, byte[] payload, boolean retained*/);

    public void disconnect(IoSession session);

    void sendPubAck(PubAckEvent evt);

}
