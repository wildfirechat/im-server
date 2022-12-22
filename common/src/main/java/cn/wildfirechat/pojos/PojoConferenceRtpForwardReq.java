package cn.wildfirechat.pojos;

public class PojoConferenceRtpForwardReq {
    public String roomId;
    public String publisherId;
    public String host;
    public int audioPort;
    public int audioPt;
    public long audioSSRC;
    public int videoPort;
    public int videoPt;
    public long videoSSRC;

    public PojoConferenceRtpForwardReq() {
    }

    public PojoConferenceRtpForwardReq(String roomId, String publisherId, String host, int audioPort, int audioPt, long audioSSRC, int videoPort, int videoPt, long videoSSRC) {
        this.roomId = roomId;
        this.publisherId = publisherId;
        this.host = host;
        this.audioPort = audioPort;
        this.audioPt = audioPt;
        this.audioSSRC = audioSSRC;
        this.videoPort = videoPort;
        this.videoPt = videoPt;
        this.videoSSRC = videoSSRC;
    }
}
