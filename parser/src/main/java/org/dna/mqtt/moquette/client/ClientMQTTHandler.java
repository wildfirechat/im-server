package org.dna.mqtt.moquette.client;

import java.util.logging.Logger;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import static org.dna.mqtt.moquette.proto.messages.AbstractMessage.*;

/**
 *
 * @author andrea
 */
public class ClientMQTTHandler extends IoHandlerAdapter {
    
    private static final Logger LOG = Logger.getLogger(ClientMQTTHandler.class.getName());
    
    Client m_callback;

    ClientMQTTHandler(Client callback)  {
        m_callback = callback;
    } 
    
    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        AbstractMessage msg = (AbstractMessage) message;
        LOG.fine("Received a message of type " + msg.getMessageType());
        switch (msg.getMessageType()) {
            case CONNACK:
                handleConnectAck(session, (ConnAckMessage) msg);
        break;
//            case SUBSCRIBE:
//                handleSubscribe(session, (SubscribeMessage) msg);
//        break;
//            case PUBLISH:
//                handlePublish(session, (PublishMessage) msg);
//        break;
        }
    }

    private void handleConnectAck(IoSession session, ConnAckMessage connAckMessage) {
        m_callback.connectionAckCallback(connAckMessage.getReturnCode());
    }
}
