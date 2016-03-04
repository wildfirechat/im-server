package io.moquette.interception.messages;

import io.moquette.parser.proto.messages.PublishMessage;

import java.nio.ByteBuffer;

/**
 * @author Wagner Macedo
 */
public class InterceptPublishMessage extends InterceptAbstractMessage {
    private final PublishMessage msg;
    private final String clientID;
    private final String username;
    
    public InterceptPublishMessage(PublishMessage msg, String clientID, String username) {
        super(msg);
        this.msg = msg;
        this.clientID = clientID;
        this.username = username;
    }

    public String getTopicName() {
        return msg.getTopicName();
    }

    public ByteBuffer getPayload() {
        return msg.getPayload();
    }

    public String getClientID() {
        return clientID;
    }

	public String getUsername() {
		return username;
	}
}
