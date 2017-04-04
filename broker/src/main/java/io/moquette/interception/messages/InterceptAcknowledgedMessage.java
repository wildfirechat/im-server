
package io.moquette.interception.messages;

import static io.moquette.spi.IMessagesStore.StoredMessage;

public class InterceptAcknowledgedMessage implements InterceptMessage {

    final private StoredMessage msg;
    private final String username;
    private final String topic;
    private final int packetID;

    public InterceptAcknowledgedMessage(StoredMessage msg, String topic, String username, int packetID) {
        this.msg = msg;
        this.username = username;
        this.topic = topic;
        this.packetID = packetID;
    }

    public StoredMessage getMsg() {
        return msg;
    }

    public String getUsername() {
        return username;
    }

    public String getTopic() {
        return topic;
    }

    public int getPacketID() {
        return packetID;
    }
}
