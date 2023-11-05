package cn.wildfirechat.pojos;

public class ConferenceDestroyEvent {
    public String userId;
    public String roomId;
    public long timestamp;

    public ConferenceDestroyEvent() {
        timestamp = System.currentTimeMillis();
    }
}
