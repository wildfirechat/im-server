package cn.wildfirechat.messagecontentbuilder;

import cn.wildfirechat.pojos.MessagePayload;
import cn.wildfirechat.proto.ProtoConstants;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class StreamingTextMessageContentBuilder extends MessageContentBuilder{
    private String text;
    private String streamId;
    private boolean generating;
    public static StreamingTextMessageContentBuilder newBuilder(String streamId) {
        StreamingTextMessageContentBuilder builder = new StreamingTextMessageContentBuilder();
        builder.generating = false;
        builder.streamId = streamId;
        return builder;
    }
    public StreamingTextMessageContentBuilder text(String text) {
        this.text = text;
        return this;
    }

    public StreamingTextMessageContentBuilder generating(boolean generating) {
        this.generating = generating;
        return this;
    }

    @Override
    public MessagePayload build() {
        MessagePayload payload = encodeBase();
        payload.setSearchableContent(text);
        payload.setContent(streamId);
        if(generating) {
            //正在生成消息的类型是14，存储标记位为透传为4。
            payload.setType(14);
            payload.setPersistFlag(4);
        } else {
            //已经生成消息的类型是15，存储标记位为透传为3。
            payload.setType(15);
            payload.setPersistFlag(3);
        }
        return payload;
    }
}
