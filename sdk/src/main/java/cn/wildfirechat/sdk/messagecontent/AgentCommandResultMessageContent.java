/*
 * Copyright (c) 2026 WildFireChat. All rights reserved.
 */

package cn.wildfirechat.sdk.messagecontent;

import cn.wildfirechat.pojos.MessagePayload;
import cn.wildfirechat.proto.ProtoConstants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 指令应答消息（机器人→用户，透明消息）。消息类型 209。
 * 客户端不渲染、不进消息流（persistFlag=Transparent，不落库、不计数）。
 * 作为 207 {@link AgentCommandMessageContent} 的应答通道，当前仅承载 op=dirs
 * （AI 面板工作目录候选按需获取，见 INTERACTION_DESIGN.md §10）。
 * payload.content 为 JSON 字符串：
 * {"ver":1,"op":"dirs","seq":12345,"robotId":"robot_xxx","cwd":"/abs/cwd",
 *  "root":"/abs/root","dirs":["a","b"],"total":194,"truncated":false}
 */
public class AgentCommandResultMessageContent extends MessageContent {
    private final JSONObject json;

    public AgentCommandResultMessageContent() {
        this.json = new JSONObject();
    }

    public AgentCommandResultMessageContent(String op, long seq) {
        this();
        json.put("ver", 1);
        json.put("op", op);
        json.put("seq", seq);
    }

    public AgentCommandResultMessageContent content(String content) { parseInto(content); return this; }

    public AgentCommandResultMessageContent ver(int ver) { json.put("ver", ver); return this; }
    public AgentCommandResultMessageContent op(String op) { json.put("op", op); return this; }
    public AgentCommandResultMessageContent seq(long seq) { json.put("seq", seq); return this; }
    public AgentCommandResultMessageContent robotId(String robotId) { json.put("robotId", robotId); return this; }
    public AgentCommandResultMessageContent cwd(String cwd) { json.put("cwd", cwd); return this; }
    public AgentCommandResultMessageContent root(String root) { json.put("root", root); return this; }
    public AgentCommandResultMessageContent dirs(List<String> dirs) {
        JSONArray array = new JSONArray();
        if (dirs != null) {
            array.addAll(dirs);
        }
        json.put("dirs", array);
        return this;
    }
    public AgentCommandResultMessageContent total(int total) { json.put("total", total); return this; }
    public AgentCommandResultMessageContent truncated(boolean truncated) { json.put("truncated", truncated); return this; }

    public int getVer() { return num("ver").intValue(); }
    public String getOp() { return str("op"); }
    public Long getSeq() { Object v = json.get("seq"); return v == null ? null : (v instanceof Number ? ((Number) v).longValue() : Long.valueOf(String.valueOf(v))); }
    public String getRobotId() { return str("robotId"); }
    public String getCwd() { return str("cwd"); }
    public String getRoot() { return str("root"); }

    /** 目录名列表（非全路径，按名称升序）；非数组/脏数据返回空列表。 */
    public List<String> getDirs() {
        List<String> result = new ArrayList<>();
        Object v = json.get("dirs");
        if (v instanceof JSONArray) {
            for (Object item : (JSONArray) v) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
        }
        return result;
    }

    public int getTotal() { return num("total").intValue(); }

    public boolean isTruncated() { return Boolean.TRUE.equals(json.get("truncated")) || "true".equalsIgnoreCase(str("truncated")); }

    public JSONObject getContentJson() { return json; }

    @Override
    public int getContentType() { return ProtoConstants.ContentType.Agent_Command_Result; }

    @Override
    public int getPersistFlag() { return ProtoConstants.PersistFlag.Transparent; }

    @Override
    public MessagePayload encode() {
        MessagePayload payload = super.encode();
        payload.setContent(json.toString());
        payload.setSearchableContent("");
        return payload;
    }

    @Override
    public void decode(MessagePayload payload) {
        super.decode(payload);
        parseInto(payload.getContent());
    }

    private void parseInto(String content) {
        if (content == null || content.isEmpty()) return;
        try {
            Object o = new JSONParser().parse(content);
            if (o instanceof JSONObject) {
                json.clear();
                json.putAll((JSONObject) o);
            }
        } catch (ParseException ignored) { }
    }

    private String str(String key) { Object v = json.get(key); return v == null ? "" : String.valueOf(v); }

    private Number num(String key) { Object v = json.get(key); return v instanceof Number ? (Number) v : 0; }
}
