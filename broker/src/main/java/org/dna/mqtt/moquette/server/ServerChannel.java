package org.dna.mqtt.moquette.server;

/**
 *
 * @author andrea
 */
public interface ServerChannel {
    
    Object getAttribute(Object key);
    
    void setAttribute(Object key, Object value);
    
    void setIdleTime(int idleTime);
    
    void close(boolean immediately);
    
    void write(Object value);
}
