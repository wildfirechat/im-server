/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.pojos;

import cn.wildfirechat.proto.WFCMessage;
import com.google.protobuf.ByteString;
import com.hazelcast.util.StringUtil;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class MessagePayload {
    private int type;
    private String searchableContent;
    private String pushContent;
    private String content;
    private String base64edData;
    private int mediaType;
    private String remoteMediaUrl;
    private int persistFlag;
    private int expireDuration;
    private int mentionedType;
    private List<String> mentionedTarget;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getSearchableContent() {
        return searchableContent;
    }

    public void setSearchableContent(String searchableContent) {
        this.searchableContent = searchableContent;
    }

    public String getPushContent() {
        return pushContent;
    }

    public void setPushContent(String pushContent) {
        this.pushContent = pushContent;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getBase64edData() {
        return base64edData;
    }

    public void setBase64edData(String base64edData) {
        this.base64edData = base64edData;
    }

    public int getMediaType() {
        return mediaType;
    }

    public void setMediaType(int mediaType) {
        this.mediaType = mediaType;
    }

    public String getRemoteMediaUrl() {
        return remoteMediaUrl;
    }

    public void setRemoteMediaUrl(String remoteMediaUrl) {
        this.remoteMediaUrl = remoteMediaUrl;
    }

    public int getPersistFlag() {
        return persistFlag;
    }

    public void setPersistFlag(int persistFlag) {
        this.persistFlag = persistFlag;
    }

    public int getExpireDuration() {
        return expireDuration;
    }

    public void setExpireDuration(int expireDuration) {
        this.expireDuration = expireDuration;
    }

    public int getMentionedType() {
        return mentionedType;
    }

    public void setMentionedType(int mentionedType) {
        this.mentionedType = mentionedType;
    }

    public List<String> getMentionedTarget() {
        return mentionedTarget;
    }

    public void setMentionedTarget(List<String> mentionedTarget) {
        this.mentionedTarget = mentionedTarget;
    }

    public WFCMessage.MessageContent toProtoMessageContent() {
        WFCMessage.MessageContent.Builder builder = WFCMessage.MessageContent.newBuilder()
            .setType(type)
            .setMediaType(mediaType)
            .setPersistFlag(persistFlag)
            .setExpireDuration(expireDuration)
            .setMentionedType(mentionedType);

        if (!StringUtil.isNullOrEmpty(searchableContent))
            builder.setSearchableContent(searchableContent);
        if (!StringUtil.isNullOrEmpty(pushContent))
            builder.setPushContent(pushContent);
        if (!StringUtil.isNullOrEmpty(searchableContent))
            builder.setContent(searchableContent);
        if (!StringUtil.isNullOrEmpty(base64edData))
            builder.setData(ByteString.copyFrom(Base64.getDecoder().decode(base64edData)));
        if (!StringUtil.isNullOrEmpty(remoteMediaUrl))
            builder.setRemoteMediaUrl(remoteMediaUrl);
        if (mentionedTarget != null && mentionedTarget.size() > 0)
            builder.addAllMentionedTarget(mentionedTarget);

        return builder.build();
    }

    public static MessagePayload fromProtoMessageContent(WFCMessage.MessageContent protoContent) {
        if (protoContent == null)
            return null;

        MessagePayload payload = new MessagePayload();
        payload.type = protoContent.getType();
        payload.searchableContent = protoContent.getSearchableContent();
        payload.pushContent = protoContent.getPushContent();
        payload.content = protoContent.getContent();
        if (protoContent.getData() != null && protoContent.getData().size() > 0)
            payload.base64edData = Base64.getEncoder().encodeToString(protoContent.getData().toByteArray());
        payload.mediaType = protoContent.getMediaType();
        payload.remoteMediaUrl = protoContent.getRemoteMediaUrl();
        payload.persistFlag = protoContent.getPersistFlag();
        payload.expireDuration = protoContent.getExpireDuration();
        payload.mentionedType = protoContent.getMentionedType();
        payload.mentionedTarget = new ArrayList<>();
        payload.mentionedTarget.addAll(protoContent.getMentionedTargetList());
        return payload;
    }
}
