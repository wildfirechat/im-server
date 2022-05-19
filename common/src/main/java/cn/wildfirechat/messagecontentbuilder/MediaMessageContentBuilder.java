package cn.wildfirechat.messagecontentbuilder;

import cn.wildfirechat.pojos.MessagePayload;

abstract public class MediaMessageContentBuilder extends MessageContentBuilder{
    private String remoteMediaUrl;

    public MediaMessageContentBuilder remoteMediaUrl(String remoteMediaUrl) {
        this.remoteMediaUrl = remoteMediaUrl;
        return this;
    }

    @Override
    protected MessagePayload encodeBase() {
        MessagePayload payload = super.encodeBase();
        payload.setRemoteMediaUrl(remoteMediaUrl);
        return payload;
    }
}
