package org.dna.mqtt.moquette.client;

import java.util.logging.Logger;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import static org.dna.mqtt.moquette.proto.messages.AbstractMessage.*;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;
import org.dna.mqtt.moquette.proto.messages.SubAckMessage;

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
            case SUBACK:
                handleSubscribeAck(session, (SubAckMessage) msg);
                break;
//            case SUBSCRIBE:
//                handleSubscribe(session, (SubscribeMessage) msg);
//        break;
            case PUBLISH:
                handlePublish(session, (PublishMessage) msg);
                break;
        }
    }

    private void handleConnectAck(IoSession session, ConnAckMessage connAckMessage) {
        m_callback.connectionAckCallback(connAckMessage.getReturnCode());
    }

    private void handleSubscribeAck(IoSession session, SubAckMessage subAckMessage) {
        m_callback.subscribeAckCallback();
    }
    
    private void handlePublish(IoSession session, PublishMessage pubMessage) {
        m_callback.publishCallback(pubMessage.getTopicName(), pubMessage.getPayload());
    }
}
