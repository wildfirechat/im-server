
package io.moquette.interception.messages;

import static io.moquette.spi.IMessagesStore.StoredMessage;

public class InterceptAcknowledgedMessage implements InterceptMessage {

    final private StoredMessage msg;
    private final String username;
    private final String topic;

    public InterceptAcknowledgedMessage(final StoredMessage msg, final String topic, final String username) {
        this.msg = msg;
        this.username = username;
        this.topic = topic;
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
}
