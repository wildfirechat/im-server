package org.dna.mqtt.moquette;

/**
 *
 * @author andrea
 */
public class SubscribeException extends ConnectionException {

    public SubscribeException(String msg) {
        super(msg);
    }

    public SubscribeException(Throwable e) {
        super(e);
    }
}
