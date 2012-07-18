package org.dna.mqtt.moquette.client;

import java.util.logging.Logger;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;

/**
 *
 * @author andrea
 */
public class ClientMQTTHandler extends IoHandlerAdapter {
    
    private static final Logger LOG = Logger.getLogger(ClientMQTTHandler.class.getName());

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        AbstractMessage msg = (AbstractMessage) message;
        LOG.fine("Received a message of type " + msg.getMessageType());
//        switch (msg.getMessageType()) {
//            case CONNECT:
//                handleConnect(session, (ConnectMessage) msg);
//        break;
//            case SUBSCRIBE:
//                handleSubscribe(session, (SubscribeMessage) msg);
//        break;
//            case PUBLISH:
//                handlePublish(session, (PublishMessage) msg);
//        break;
//        }
    }
}
