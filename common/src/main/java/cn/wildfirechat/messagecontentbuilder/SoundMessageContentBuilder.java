package cn.wildfirechat.messagecontentbuilder;

import cn.wildfirechat.pojos.MessagePayload;
import cn.wildfirechat.proto.ProtoConstants;
import org.json.simple.JSONObject;

public class SoundMessageContentBuilder extends MediaMessageContentBuilder {
    private int duration;
    public static SoundMessageContentBuilder newBuilder(int duration) {
        SoundMessageContentBuilder builder = new SoundMessageContentBuilder();
        builder.duration = duration;
        return builder;
    }
    @Override
    public MessagePayload build() {
        MessagePayload payload = encodeBase();
        payload.setType(ProtoConstants.ContentType.Voice);
        payload.setMediaType(ProtoConstants.MessageMediaType.VOICE);
        payload.setPersistFlag(ProtoConstants.PersistFlag.Persist_And_Count);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("duration", duration);
        payload.setContent(jsonObject.toJSONString());
        return payload;
    }
}
