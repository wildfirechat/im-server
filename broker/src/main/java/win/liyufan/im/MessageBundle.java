/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package win.liyufan.im;

import cn.wildfirechat.proto.WFCMessage;

import java.io.Serializable;


public class MessageBundle implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8959293027687263752L;
	private String fromUser;
	private String fromClientId;
	private int type;
	private int line;
	private String targetId;
	private WFCMessage.Message message;
	private long messageId;

    public MessageBundle() {
    }

    public MessageBundle(long messageId, String fromUser, String fromClientId, WFCMessage.Message message) {
		super();
		this.fromUser = fromUser;
		this.fromClientId = fromClientId;
		this.type = message.getConversation().getType();
		this.targetId = message.getConversation().getTarget();
		this.line = message.getConversation().getLine();
		this.message = message;
		this.messageId = messageId;
	}
	
	public int getLine() {
		return line;
	}

	public long getMessageId() {
		return messageId;
	}

	public String getFromUser() {
		return fromUser;
	}
	public String getFromClientId() {
		return fromClientId;
	}
	public int getType() {
		return type;
	}
	public String getTargetId() {
		return targetId;
	}
	public WFCMessage.Message getMessage() {
		return message;
	}

    public void setFromClientId(String fromClientId) {
        this.fromClientId = fromClientId;
    }

    public void setMessage(WFCMessage.Message message) {
        this.fromUser = message.getFromUser();
        this.type = message.getConversation().getType();
        this.targetId = message.getConversation().getTarget();
        this.line = message.getConversation().getLine();
        this.message = message;
        this.messageId = message.getMessageId();
    }
}
