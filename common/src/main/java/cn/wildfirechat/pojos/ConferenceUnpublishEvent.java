package cn.wildfirechat.pojos;

public class ConferenceUnpublishEvent {
    public String userId;
    public String roomId;
    public boolean screenSharing;
    public long timestamp;

    public ConferenceUnpublishEvent() {
        timestamp = System.currentTimeMillis();
    }
}
