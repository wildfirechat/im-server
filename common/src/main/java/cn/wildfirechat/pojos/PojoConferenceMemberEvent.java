package cn.wildfirechat.pojos;

/**
 * 会议成员事件流水。
 *
 * 每次成员状态变化追加一条记录，不做修改。
 */
public class PojoConferenceMemberEvent {
    public static final int EVENT_JOIN = 1;
    public static final int EVENT_LEAVE = 2;
    public static final int EVENT_PUBLISH = 3;
    public static final int EVENT_UNPUBLISH = 4;
    public static final int EVENT_AUDIO_MUTE = 5;
    public static final int EVENT_AUDIO_UNMUTE = 6;
    public static final int EVENT_VIDEO_MUTE = 7;
    public static final int EVENT_VIDEO_UNMUTE = 8;
    public static final int EVENT_SCREEN_SHARE_START = 9;
    public static final int EVENT_SCREEN_SHARE_STOP = 10;
    public static final int EVENT_KICK = 11;
    public static final int EVENT_UPDATE_EXTRA = 12;

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

    public static String nameOf(int eventType) {
        switch (eventType) {
            case EVENT_JOIN:
                return "JOIN";
            case EVENT_LEAVE:
                return "LEAVE";
            case EVENT_PUBLISH:
                return "PUBLISH";
            case EVENT_UNPUBLISH:
                return "UNPUBLISH";
            case EVENT_AUDIO_MUTE:
                return "AUDIO_MUTE";
            case EVENT_AUDIO_UNMUTE:
                return "AUDIO_UNMUTE";
            case EVENT_VIDEO_MUTE:
                return "VIDEO_MUTE";
            case EVENT_VIDEO_UNMUTE:
                return "VIDEO_UNMUTE";
            case EVENT_SCREEN_SHARE_START:
                return "SCREEN_SHARE_START";
            case EVENT_SCREEN_SHARE_STOP:
                return "SCREEN_SHARE_STOP";
            case EVENT_KICK:
                return "KICK";
            case EVENT_UPDATE_EXTRA:
                return "UPDATE_EXTRA";
            default:
                return "UNKNOWN";
        }
    }
}
