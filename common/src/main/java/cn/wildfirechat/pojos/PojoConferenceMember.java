package cn.wildfirechat.pojos;

/**
 * 会议成员快照。
 *
 * 每个 (roomId, userId, clientId) 一条记录，保存当前/最后一次参会状态。
 */
public class PojoConferenceMember {
    public long id;
    public String roomId;
    public int roomType;
    public String userId;
    public String clientId;
    public long sessionId;
    public long handleId;
    public long screensharingHandleId;
    public String serverId;
    public boolean advance;
    public boolean robot;
    public boolean publishing;
    public boolean audioMuted;
    public boolean videoMuted;
    public boolean screensharing;
    public boolean online;
    public int joinCount;
    public long firstJoinDt;
    public long lastJoinDt;
    public long leaveDt;
    public int leaveReason;
    public long duration;
    public String extra;
    public long createDt;
    public long updateDt;
}
