package cn.wildfirechat.pojos;

public class ConferencePublishEvent {
    public String userId;
    public boolean video;
    public String roomId;
    public boolean screenSharing;
    public long timestamp;

    public ConferencePublishEvent() {
        timestamp = System.currentTimeMillis();
    }
}
