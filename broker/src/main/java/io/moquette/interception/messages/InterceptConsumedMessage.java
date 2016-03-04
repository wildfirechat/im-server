package io.moquette.interception.messages;

import static io.moquette.spi.IMessagesStore.StoredMessage;

public class InterceptConsumedMessage {
	final private StoredMessage msg;
	 private final String username;
	
	public InterceptConsumedMessage( final StoredMessage msg, String username) {
		this.msg = msg;
		this.username = username;
	}
	
	public StoredMessage getMsg() {
		return msg;
	}

	public String getUsername() {
		return username;
	}
}
