package org.dna.mqtt.moquette.server.mina;

import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.dna.mqtt.moquette.server.ServerChannel;

/**
 *
 * @author andrea
 */
public class MinaChannel implements ServerChannel {
    
    private IoSession m_session;
    
    public MinaChannel(IoSession session) {
        m_session = session;
    }

    public Object getAttribute(Object key) {
        return m_session.getAttribute(key);
    }
    
    public void setAttribute(Object key, Object value) {
        m_session.setAttribute(key, value);
    }

    public void close(boolean immediately) {
        m_session.close(immediately);
    }

    public void write(Object value) {
        m_session.write(value);
    }

    public void setIdleTime(int idleTime) {
        m_session.getConfig().setIdleTime(IdleStatus.READER_IDLE, idleTime);
    }

    
}
