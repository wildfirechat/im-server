package cn.wildfirechat.messagecontentbuilder;

import cn.wildfirechat.pojos.MessagePayload;
import cn.wildfirechat.proto.ProtoConstants;
import io.netty.util.internal.StringUtil;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class RichNotificationContentBuilder extends MessageContentBuilder{
    private String title;
    private String desc;
    private String remark;
    private JSONArray datas;
    private String exName;
    private String exPortrait;
    private String exUrl;
    private String appId;

    public static RichNotificationContentBuilder newBuilder(String title, String desc, String exUrl) {
        RichNotificationContentBuilder builder = new RichNotificationContentBuilder();
        builder.title = title;
        builder.desc = desc;
        builder.exUrl = exUrl;
        return builder;
    }

    public RichNotificationContentBuilder remark(String remark) {
        this.remark = remark;
        return this;
    }
    public RichNotificationContentBuilder exName(String exName) {
        this.exName = exName;
        return this;
    }
    public RichNotificationContentBuilder exPortrait(String exPortrait) {
        this.exPortrait = exPortrait;
        return this;
    }
    public RichNotificationContentBuilder appId(String appId) {
        this.appId = appId;
        return this;
    }
    public RichNotificationContentBuilder addItem(String key, String value) {
        return addItem(key, value, null);
    }
    public RichNotificationContentBuilder addItem(String key, String value, String color) {
        if(this.datas == null) {
            this.datas = new JSONArray();
        }
        JSONObject item = new JSONObject();
        item.put("key", key);
        item.put("value", value == null ? "" : value);
        if(!StringUtil.isNullOrEmpty(color)) {
            item.put("color", color);
        }
        this.datas.add(item);
        return this;
    }

    @Override
    public MessagePayload build() {
        MessagePayload payload = encodeBase();
        payload.setType(ProtoConstants.ContentType.Rich_Notification);
        payload.setPersistFlag(ProtoConstants.PersistFlag.Persist_And_Count);
        payload.setPushContent(title);
        payload.setContent(desc);
        JSONObject jsonObject = new JSONObject();
        if(!StringUtil.isNullOrEmpty(remark))
            jsonObject.put("remark", remark);
        if(!StringUtil.isNullOrEmpty(exName))
            jsonObject.put("exName", exName);
        if(!StringUtil.isNullOrEmpty(exPortrait))
            jsonObject.put("exPortrait", exPortrait);
        if(!StringUtil.isNullOrEmpty(exUrl))
            jsonObject.put("exUrl", exUrl);
        if(!StringUtil.isNullOrEmpty(appId))
            jsonObject.put("appId", appId);
        if(datas != null && !datas.isEmpty())
            jsonObject.put("datas", datas);

        payload.setBase64edData(Base64.getEncoder().encodeToString(jsonObject.toJSONString().getBytes(StandardCharsets.UTF_8)));

        return payload;
    }
}
