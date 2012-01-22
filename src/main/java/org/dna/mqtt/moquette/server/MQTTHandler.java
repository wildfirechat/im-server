package org.dna.mqtt.moquette.server;

import org.dna.mqtt.moquette.messaging.spi.IMessaging;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import static org.dna.mqtt.moquette.proto.messages.AbstractMessage.*;

/**
 *
 * @author andrea
 */
public class MQTTHandler extends IoHandlerAdapter {

    private static final Logger LOG = Logger.getLogger(MQTTHandler.class.getName());
    Map<String, IoSession> m_clientIDs = new HashMap<String, IoSession>();
    private IMessaging m_messaging;
    private IAuthenticator m_authenticator;

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        AbstractMessage msg = (AbstractMessage) message;
        LOG.fine("Received a message of type " + msg.getMessageType());
        switch (msg.getMessageType()) {
            case CONNECT:
                handleConnect(session, (ConnectMessage) msg);
        }
    }

    protected void handleConnect(IoSession session, ConnectMessage msg) {
        if (msg.getProcotolVersion() != 0x03) {
            ConnAckMessage badProto = new ConnAckMessage();
            badProto.setReturnCode(ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION);
            session.write(badProto);
            session.close(false);
            return;
        }

        if (msg.getClientID().length() > 23) {
            ConnAckMessage okResp = new ConnAckMessage();
            okResp.setReturnCode(ConnAckMessage.IDENTIFIER_REJECTED);
            session.write(okResp);
            return;
        }

        //if an old client with the same ID already exists close its session.
        if (m_clientIDs.containsKey(msg.getClientID())) {
            m_clientIDs.get(msg.getClientID()).close(false);
        }
        m_clientIDs.put(msg.getClientID(), session);

        int keepAlive = msg.getKeepAlive();
        session.setAttribute("keepAlive", keepAlive);
        session.setAttribute("cleanSession", msg.isCleanSession());

        session.getConfig().setIdleTime(IdleStatus.READER_IDLE, Math.round(keepAlive * 1.5f));

        //Handle will flag
        if (msg.isWillFlag()) {
            m_messaging.publish(msg.getWillTopic(), msg.getWillMessage(), msg.getWillQos(), msg.isWillRetain());
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

        //TODO handle clean session flag

        ConnAckMessage okResp = new ConnAckMessage();
        okResp.setReturnCode(ConnAckMessage.CONNECTION_ACCEPTED);
        session.write(okResp);
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
}
