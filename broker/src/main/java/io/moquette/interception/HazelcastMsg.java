
package io.moquette.interception;

import io.moquette.interception.messages.InterceptPublishMessage;
import java.io.Serializable;
import static io.moquette.spi.impl.Utils.readBytesAndRewind;

/**
 * Created by mackristof on 28/05/2016.
 */
public class HazelcastMsg implements Serializable {

    private static final long serialVersionUID = -1431584750134928273L;
    private final String clientId;
    private final int qos;
    private final byte[] payload;
    private final String topic;

    public HazelcastMsg(InterceptPublishMessage msg) {
        this.clientId = msg.getClientID();
        this.topic = msg.getTopicName();
        this.qos = msg.getQos().value();
        this.payload = readBytesAndRewind(msg.getPayload());
    }

    public String getClientId() {
        return clientId;
    }

    public int getQos() {
        return qos;
    }

    public byte[] getPayload() {
        return payload;
    }

    public String getTopic() {
        return topic;
    }
}
