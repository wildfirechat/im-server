package cn.wildfirechat.messagecontentbuilder;

import cn.wildfirechat.pojos.MessagePayload;
import cn.wildfirechat.proto.ProtoConstants;

public class TextMessageContentBuilder extends MessageContentBuilder{
    private String text;
    public static TextMessageContentBuilder newBuilder(String text) {
        TextMessageContentBuilder builder = new TextMessageContentBuilder();
        builder.text = text;
        return builder;
    }
    @Override
    public MessagePayload build() {
        MessagePayload payload = encodeBase();
        payload.setType(ProtoConstants.ContentType.Text);
        payload.setPersistFlag(ProtoConstants.PersistFlag.Persist_And_Count);
        payload.setSearchableContent(text);
        return payload;
    }
}
