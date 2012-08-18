package org.dna.mqtt.commons;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author andrea
 */
public class MessageIDGenerator {
    private AtomicInteger m_current;
    
    public MessageIDGenerator() {
        //NB we start at -1 because the first incAndGet has to return 0
        m_current = new AtomicInteger(-1);
    }
    
    public int next() {
        return m_current.incrementAndGet();
    }
}
