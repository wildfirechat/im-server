package io.moquette.interception.messages;

import static io.moquette.spi.IMessagesStore.StoredMessage;

public class InterceptConsumedMessage {
	final private StoredMessage msg;
	
	public InterceptConsumedMessage( final StoredMessage msg) {
		this.msg = msg;
	}
	
	public StoredMessage getMsg() {
		return msg;
	}
}
