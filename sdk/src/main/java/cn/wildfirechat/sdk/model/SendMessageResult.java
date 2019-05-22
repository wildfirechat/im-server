package cn.wildfirechat.sdk.model;

public class SendMessageResult {
    private long messageUid;
    private long timestamp;

    public SendMessageResult() {
    }

    public SendMessageResult(long messageUid, long timestamp) {
        this.messageUid = messageUid;
        this.timestamp = timestamp;
    }

    public long getMessageUid() {
        return messageUid;
    }

    public void setMessageUid(long messageUid) {
        this.messageUid = messageUid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
