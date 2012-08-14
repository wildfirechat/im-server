package org.dna.mqtt.moquette.client;

/**
 *
 * @author andrea
 */
public interface IPublishCallback {

    void published(String topic, byte[] message/*, boolean retained*/);
}
