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
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import static org.dna.mqtt.moquette.proto.messages.AbstractMessage.*;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage.QOSType;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.dna.mqtt.moquette.proto.messages.DisconnectMessage;
import org.dna.mqtt.moquette.proto.messages.PingRespMessage;
import org.dna.mqtt.moquette.proto.messages.PublishMessage;
import org.dna.mqtt.moquette.proto.messages.SubAckMessage;
import org.dna.mqtt.moquette.proto.messages.SubscribeMessage;
import org.dna.mqtt.moquette.proto.messages.UnsubAckMessage;
import org.dna.mqtt.moquette.proto.messages.UnsubscribeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
public class MQTTHandler extends IoHandlerAdapter implements INotifier {

    protected static final String ATTR_CLIENTID = "ClientID";
    private static final Logger LOG = LoggerFactory.getLogger(MQTTHandler.class);
    /**
     * Maps CLIENT_ID to the IoSession that represents the connection
     */
    Lock m_clientIDsLock = new ReentrantLock();
    Map<String, ConnectionDescriptor> m_clientIDs = new HashMap<String, ConnectionDescriptor>();
    private IMessaging m_messaging;
    private IAuthenticator m_authenticator;

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

            }
        } catch (Exception ex) {
            LOG.error("Bad error in processing the message", ex);
        }
    }

    protected void handleConnect(IoSession session, ConnectMessage msg) {
        LOG.info("handleConnect invoked");
        if (msg.getProcotolVersion() != 0x03) {
            ConnAckMessage badProto = new ConnAckMessage();
            badProto.setReturnCode(ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION);
            session.write(badProto);
            session.close(false);
            return;
        }

        if (msg.getClientID() == null || msg.getClientID().length() > 23) {
            ConnAckMessage okResp = new ConnAckMessage();
            okResp.setReturnCode(ConnAckMessage.IDENTIFIER_REJECTED);
            session.write(okResp);
            return;
        }

        m_clientIDsLock.lock();
        try {
            //if an old client with the same ID already exists close its session.
            if (m_clientIDs.containsKey(msg.getClientID())) {
                //clean the subscriptions if the old used a cleanSession = true
                IoSession oldSession = m_clientIDs.get(msg.getClientID()).getSession();
                boolean cleanSession = (Boolean) oldSession.getAttribute("cleanSession");
                if (cleanSession) {
                    //cleanup topic subscriptions
                    m_messaging.removeSubscriptions(msg.getClientID());
                }

                m_clientIDs.get(msg.getClientID()).getSession().close(false);
            }

            ConnectionDescriptor connDescr = new ConnectionDescriptor(msg.getClientID(), session, msg.isCleanSession());
            m_clientIDs.put(msg.getClientID(), connDescr);
        } finally {
            m_clientIDsLock.unlock();
        }

        int keepAlive = msg.getKeepAlive();
        session.setAttribute("keepAlive", keepAlive);
        session.setAttribute("cleanSession", msg.isCleanSession());
        //used to track the client in the subscription and publishing phases. 
        session.setAttribute(ATTR_CLIENTID, msg.getClientID());

        session.getConfig().setIdleTime(IdleStatus.READER_IDLE, Math.round(keepAlive * 1.5f));

        //Handle will flag
        if (msg.isWillFlag()) {
            QOSType willQos = QOSType.values()[msg.getWillQos()];
            m_messaging.publish(msg.getWillTopic(), msg.getWillMessage().getBytes(),
                    willQos, msg.isWillRetain());
        }

        //handle user authentication
        if (msg.isUserFlag()) {
            String pwd = null;
            if (msg.isPasswordFlag()) {
                pwd = msg.getPassword();
            }
            if (!m_authenticator.checkValid(msg.getUsername(), pwd)) {
                ConnAckMessage okResp = new ConnAckMessage();
                okResp.setReturnCode(ConnAckMessage.BAD_USERNAME_OR_PASSWORD);
                session.write(okResp);
                return;
            }
        }

        //TODO for QoS1 and QoS remember to store all the messages arrived to
        //the dormat subscriptionsonce the QoS1 and QoS2
        //handle clean session flag
        if (msg.isCleanSession()) {
            //remove all prev subscriptions
            //cleanup topic subscriptions
            m_messaging.removeSubscriptions(msg.getClientID());
        }

        ConnAckMessage okResp = new ConnAckMessage();
        okResp.setReturnCode(ConnAckMessage.CONNECTION_ACCEPTED);
        session.write(okResp);
    }

    protected void handleSubscribe(IoSession session, SubscribeMessage msg) {
        LOG.info("registering the subscriptions");
        for (SubscribeMessage.Couple req : msg.subscriptions()) {
            m_messaging.subscribe((String) session.getAttribute(ATTR_CLIENTID),
                    req.getTopic(), AbstractMessage.QOSType.values()[req.getQos()]);
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
            m_messaging.unsubscribe(topic, (String) session.getAttribute(ATTR_CLIENTID));
        }
        //ack the client
        UnsubAckMessage ackMessage = new UnsubAckMessage();
        ackMessage.setMessageID(msg.getMessageID());

        LOG.info("replying with UnsubAct to MSG ID {0}", msg.getMessageID());
        session.write(ackMessage);
    }

    protected void handlePublish(IoSession session, PublishMessage message) {
        m_messaging.publish(message.getTopicName(), message.getPayload(),
                message.getQos(), message.isRetainFlag());
    }

    protected void handleDisconnect(IoSession session, DisconnectMessage disconnectMessage) {
        String clientID = (String) session.getAttribute(ATTR_CLIENTID);
        //remove from clientIDs
        m_clientIDsLock.lock();
        try {
            m_clientIDs.remove(clientID);
        } finally {
            m_clientIDsLock.unlock();
        }
        boolean cleanSession = (Boolean) session.getAttribute("cleanSession");
        if (cleanSession) {
            //cleanup topic subscriptions
            m_messaging.removeSubscriptions(clientID);
        }
        
        //close the TCP connection
        session.close(true);
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {
        session.close(false);
    }

    public void setMessaging(IMessaging messaging) {
        m_messaging = messaging;
    }

    public void setAuthenticator(IAuthenticator authenticator) {
        m_authenticator = authenticator;
    }

    public void notify(String clientId, String topic, QOSType qOSType, byte[] payload, boolean retained) {
        PublishMessage pubMessage = new PublishMessage();
        pubMessage.setRetainFlag(retained);
        pubMessage.setTopicName(topic);
        pubMessage.setQos(qOSType);
        pubMessage.setPayload(payload);
        assert m_clientIDs != null;
        assert m_clientIDs.get(clientId) != null;
        LOG.debug("Session for clientId " + clientId + " is " + m_clientIDs.get(clientId).getSession());
        m_clientIDs.get(clientId).getSession().write(pubMessage);
    }
}
