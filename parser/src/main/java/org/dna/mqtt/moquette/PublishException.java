package org.dna.mqtt.moquette;

/**
 *
 * @author andrea
 */
public class PublishException extends ConnectionException {

    public PublishException(String msg) {
        super(msg);
    }

    public PublishException(Throwable e) {
        super(e);
    }
}
