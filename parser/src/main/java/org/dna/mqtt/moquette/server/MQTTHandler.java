package org.dna.mqtt.moquette.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.messaging.spi.IMessaging;
import org.dna.mqtt.moquette.messaging.spi.INotifier;
import org.dna.mqtt.moquette.messaging.spi.impl.events.NotifyEvent;
import org.dna.mqtt.moquette.messaging.spi.impl.events.PubAckEvent;
import org.dna.mqtt.moquette.proto.messages.*;

import static org.dna.mqtt.moquette.proto.messages.AbstractMessage.*;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MINA MQTT Handler used to route messages to protocol logic
 *
 * @author andrea
 */
public class MQTTHandler extends IoHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(MQTTHandler.class);
    private IMessaging m_messaging;

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        AbstractMessage msg = (AbstractMessage) message;
        LOG.info("Received a message of type {0}", msg.getMessageType());
        try {
            switch (msg.getMessageType()) {
                case CONNECT:
                    handleConnect(session, (ConnectMessage) msg);
                    break;
                case SUBSCRIBE:
                    handleSubscribe(session, (SubscribeMessage) msg);
                    break;
                case UNSUBSCRIBE:
                    handleUnsubscribe(session, (UnsubscribeMessage) msg);
                    break;    
                case PUBLISH:
                    handlePublish(session, (PublishMessage) msg);
                    break;
                case PINGREQ:
                    session.write(new PingRespMessage());
                    break;
                case DISCONNECT:
                    handleDisconnect(session, (DisconnectMessage) msg);
                    break;
            }
        } catch (Exception ex) {
            LOG.error("Bad error in processing the message", ex);
        }
    }

    protected void handleConnect(IoSession session, ConnectMessage msg) {
        LOG.info("handleConnect invoked");

        m_messaging.connect(session, msg);
    }

    protected void handleSubscribe(IoSession session, SubscribeMessage msg) {
        LOG.debug("handleSubscribe, registering the subscriptions");
        for (SubscribeMessage.Couple req : msg.subscriptions()) {
            String clientID = (String) session.getAttribute(Constants.ATTR_CLIENTID);
            boolean cleanSession = (Boolean) session.getAttribute(Constants.CLEAN_SESSION);
            m_messaging.subscribe(clientID, req.getTopic(), AbstractMessage.QOSType.values()[req.getQos()],
                    cleanSession, msg.getMessageID());
        }

        //ack the client
        SubAckMessage ackMessage = new SubAckMessage();
        ackMessage.setMessageID(msg.getMessageID());

        //TODO by now it handles only QoS 0 messages
        for (int i = 0; i < msg.subscriptions().size(); i++) {
            ackMessage.addType(QOSType.MOST_ONE);
        }
        LOG.info("replying with SubAct to MSG ID {0}", msg.getMessageID());
        session.write(ackMessage);
    }
    
    private void handleUnsubscribe(IoSession session, UnsubscribeMessage msg) {
        LOG.info("unregistering the subscriptions");
        for (String topic : msg.topics()) {
            m_messaging.unsubscribe(topic, (String) session.getAttribute(Constants.ATTR_CLIENTID));
        }
        //ack the client
        UnsubAckMessage ackMessage = new UnsubAckMessage();
        ackMessage.setMessageID(msg.getMessageID());

        LOG.info("replying with UnsubAck to MSG ID {0}", msg.getMessageID());
        session.write(ackMessage);
    }

    protected void handlePublish(IoSession session, PublishMessage message) {
        String clientID = (String) session.getAttribute(Constants.ATTR_CLIENTID);

        if (message.getQos() == QOSType.MOST_ONE) {
            m_messaging.publish(message.getTopicName(), message.getPayload(),
                    message.getQos(), message.isRetainFlag(), clientID, session);
        } else {
            m_messaging.publish(message.getTopicName(), message.getPayload(),
                    message.getQos(), message.isRetainFlag(), clientID, message.getMessageID(), session);
        }
    }

    protected void handleDisconnect(IoSession session, DisconnectMessage disconnectMessage) {
        String clientID = (String) session.getAttribute(Constants.ATTR_CLIENTID);
        boolean cleanSession = (Boolean) session.getAttribute(Constants.CLEAN_SESSION);
        if (cleanSession) {
            //cleanup topic subscriptions
            m_messaging.removeSubscriptions(clientID);
        }
        
        //close the TCP connection
        //session.close(true);
        m_messaging.disconnect(session);
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {
        session.close(false);
    }

    public void setMessaging(IMessaging messaging) {
        m_messaging = messaging;
    }

}
