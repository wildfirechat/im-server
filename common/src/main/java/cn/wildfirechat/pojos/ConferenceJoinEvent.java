package cn.wildfirechat.pojos;

public class ConferenceJoinEvent {
    public String userId;
    public String roomId;
    public boolean screenSharing;
    public long timestamp;

    public ConferenceJoinEvent() {
        timestamp = System.currentTimeMillis();
    }
}
