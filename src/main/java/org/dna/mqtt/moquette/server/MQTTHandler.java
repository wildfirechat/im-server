package org.dna.mqtt.moquette.server;

import java.util.HashMap;
import java.util.Map;
import org.dna.mqtt.moquette.proto.messages.ConnectMessage;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.dna.mqtt.moquette.proto.messages.ConnAckMessage;
import static org.dna.mqtt.moquette.proto.messages.AbstractMessage.*;

/**
 *
 * @author andrea
 */
public class MQTTHandler extends IoHandlerAdapter {
    
    Map<String, IoSession> m_clientIDs = new HashMap<String, IoSession>();

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        AbstractMessage msg = (AbstractMessage) message;
        switch (msg.getMessageType()) {
            case CONNECT:
                handleConnect(session, (ConnectMessage) msg);
        }
    }

    protected void handleConnect(IoSession session, ConnectMessage connectMessage) {
        if (connectMessage.getProcotolVersion() != 0x03) {
            ConnAckMessage badProto = new ConnAckMessage();
            badProto.setReturnCode(ConnAckMessage.UNNACEPTABLE_PROTOCOL_VERSION);
            session.write(badProto);
            session.close(false);
            return;
        }
        
        //if an old client with the same ID already exists close its session.s
        if (m_clientIDs.containsKey(connectMessage.getClientID())) {
            m_clientIDs.get(connectMessage.getClientID()).close(false);
        }
        m_clientIDs.put(connectMessage.getClientID(), session);
        
        session.setAttribute("keepAlive", connectMessage.getKeepAlive());
        session.setAttribute("cleanSession", connectMessage.isCleanSession());
        
        //TODO Handle will flag
        //TODO handle user authentication
        
        ConnAckMessage okResp = new ConnAckMessage();
        okResp.setReturnCode(ConnAckMessage.CONNECTION_ACCEPTED);
        session.write(okResp);
    }
}
