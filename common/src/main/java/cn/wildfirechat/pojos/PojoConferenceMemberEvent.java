package cn.wildfirechat.pojos;

/**
 * 会议成员事件流水。
 *
 * 每个事件类型表示一个状态维度，oldValue/newValue 表示状态变化：
 * 1 表示开启/加入/静音/开始，0 表示关闭/离开/取消静音/停止。
 */
public class PojoConferenceMemberEvent {
    public static final int EVENT_JOIN_STATE = 1;
    public static final int EVENT_PUBLISH_STATE = 2;
    public static final int EVENT_AUDIO_MUTE_STATE = 3;
    public static final int EVENT_VIDEO_MUTE_STATE = 4;
    public static final int EVENT_SCREEN_SHARE_STATE = 5;
    public static final int EVENT_KICK = 6;
    public static final int EVENT_UPDATE_EXTRA = 7;

    public static final int SOURCE_CLIENT = 1;
    public static final int SOURCE_JANUS = 2;
    public static final int SOURCE_SYSTEM = 3;

    public static final int ROOM_TYPE_CONFERENCE = 1;
    public static final int ROOM_TYPE_CALL = 2;

    public static final int LEAVE_REASON_NORMAL = 0;
    public static final int LEAVE_REASON_KICKED = 1;
    public static final int LEAVE_REASON_TIMEOUT = 2;
    public static final int LEAVE_REASON_CLIENT_OFFLINE = 3;
    public static final int LEAVE_REASON_DESTROYED = 4;
    public static final int LEAVE_REASON_SERVER_DISCONNECTED = 5;
    public static final int LEAVE_REASON_REJOIN_REPLACED = 6;

    public long id;
    public String roomId;
    public int roomType;
    public String userId;
    public String clientId;
    public long sessionId;
    public long handleId;
    public int eventType;
    public String eventName;
    public String oldValue;
    public String newValue;
    public String operator;
    public int source;
    public String data;
    public long dt;
    public long timestamp;

    /**
     * 根据状态维度和新状态生成可读事件名。
     */
    public static String nameOf(int eventType, String newValue) {
        boolean on = "1".equals(newValue);
        switch (eventType) {
            case EVENT_JOIN_STATE:
                return on ? "JOIN" : "LEAVE";
            case EVENT_PUBLISH_STATE:
                return on ? "PUBLISH" : "UNPUBLISH";
            case EVENT_AUDIO_MUTE_STATE:
                return on ? "AUDIO_MUTE" : "AUDIO_UNMUTE";
            case EVENT_VIDEO_MUTE_STATE:
                return on ? "VIDEO_MUTE" : "VIDEO_UNMUTE";
            case EVENT_SCREEN_SHARE_STATE:
                return on ? "SCREEN_SHARE_START" : "SCREEN_SHARE_STOP";
            case EVENT_KICK:
                return "KICK";
            case EVENT_UPDATE_EXTRA:
                return "UPDATE_EXTRA";
            default:
                return "UNKNOWN";
        }
    }
}
