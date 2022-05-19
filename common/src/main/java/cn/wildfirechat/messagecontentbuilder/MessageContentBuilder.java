package cn.wildfirechat.messagecontentbuilder;

import cn.wildfirechat.pojos.MessagePayload;

import java.util.List;

public abstract class MessageContentBuilder {
    private int mentionedType;
    private List<String> mentionedTargets;
    private String extra;
    public MessageContentBuilder mentionedType(int mentionedType) {
        this.mentionedType = mentionedType;
        return this;
    }
    public MessageContentBuilder mentionedTargets(List<String> mentionedTargets) {
        this.mentionedTargets = mentionedTargets;
        return this;
    }

    public MessageContentBuilder extra(String extra) {
        this.extra = extra;
        return this;
    }

    protected MessagePayload encodeBase() {
        MessagePayload payload = new MessagePayload();
        payload.setMentionedType(mentionedType);
        payload.setMentionedTarget(mentionedTargets);
        payload.setExtra(extra);
        return payload;
    }
    abstract public MessagePayload build();
}
