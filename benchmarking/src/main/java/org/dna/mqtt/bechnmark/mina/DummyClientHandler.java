package org.dna.mqtt.bechnmark.mina;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.proto.messages.AbstractMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author andrea
 */
public class DummyClientHandler extends IoHandlerAdapter {
    
     private static final Logger LOG = LoggerFactory.getLogger(DummyClientHandler.class);
    
    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        AbstractMessage msg = (AbstractMessage) message;
        LOG.debug("Received a message of type " + msg.getMessageType());
    }
}

