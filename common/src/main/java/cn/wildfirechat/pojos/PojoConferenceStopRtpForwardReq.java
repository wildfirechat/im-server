package cn.wildfirechat.pojos;

public class PojoConferenceStopRtpForwardReq {
    public String roomId;
    public String publisherId;
    public long streamId;

    public PojoConferenceStopRtpForwardReq() {
    }

    public PojoConferenceStopRtpForwardReq(String roomId, String publisherId, long streamId) {
        this.roomId = roomId;
        this.publisherId = publisherId;
        this.streamId = streamId;
    }
}
