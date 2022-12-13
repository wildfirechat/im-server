package cn.wildfirechat.pojos;

public class PojoConferenceRtpForwardReq {
    public String roomId;
    public String publisherId;
    public String host;
    public int audioPort;
    public int videoPort;

    public PojoConferenceRtpForwardReq() {
    }

    public PojoConferenceRtpForwardReq(String roomId, String publisherId, String host, int audioPort, int videoPort) {
        this.roomId = roomId;
        this.publisherId = publisherId;
        this.host = host;
        this.audioPort = audioPort;
        this.videoPort = videoPort;
    }
}
