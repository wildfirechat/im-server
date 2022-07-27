/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;

import cn.wildfirechat.proto.WFCMessage;
import io.netty.util.internal.StringUtil;

import java.util.List;

public class OutputMessageData {
    private long messageId;
    private String sender;
    private Conversation conv;
    private MessagePayload payload;
    private List<String> toUsers;
    private long timestamp;
    private OutputClient client;

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public Conversation getConv() {
        return conv;
    }

    public void setConv(Conversation conv) {
        this.conv = conv;
    }

    public MessagePayload getPayload() {
        return payload;
    }

    public void setPayload(MessagePayload payload) {
        this.payload = payload;
    }

    public List<String> getToUsers() {
        return toUsers;
    }

    public void setToUsers(List<String> toUsers) {
        this.toUsers = toUsers;
    }

    public long getMessageId() {
        return messageId;
    }

    public void setMessageId(long messageId) {
        this.messageId = messageId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public OutputClient getClient() {
        return client;
    }

    public void setClient(OutputClient client) {
        this.client = client;
    }

    public static OutputMessageData fromProtoMessage(WFCMessage.Message protoMessage) {
        return fromProtoMessage(protoMessage, null);
    }

    public static OutputMessageData fromProtoMessage(WFCMessage.Message protoMessage, OutputClient fromClient) {
        OutputMessageData data = new OutputMessageData();
        data.messageId = protoMessage.getMessageId();
        data.sender = protoMessage.getFromUser();
        data.conv = new Conversation();
        data.conv.setTarget(protoMessage.getConversation().getTarget());
        data.conv.setType(protoMessage.getConversation().getType());
        data.conv.setLine(protoMessage.getConversation().getLine());
        data.payload = MessagePayload.fromProtoMessageContent(protoMessage.getContent());
        data.timestamp = protoMessage.getServerTimestamp();
        data.toUsers = protoMessage.getToList();
        data.client = fromClient;
        return data;
    }
}
