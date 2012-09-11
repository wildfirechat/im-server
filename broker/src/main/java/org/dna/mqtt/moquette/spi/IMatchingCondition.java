package org.dna.mqtt.moquette.messaging.spi;

/**
 */
public interface IMatchingCondition {
    boolean match(String key);
}

