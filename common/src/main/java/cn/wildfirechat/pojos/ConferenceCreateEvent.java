package cn.wildfirechat.pojos;

public class ConferenceCreateEvent {
    public String userId;
    public String roomId;
    public String serverId;
    public String description;
    public String pin;
    public int max_publishers;
    public int bitrate;
    public boolean advance;
    public boolean recording;
    public long timestamp;

    public ConferenceCreateEvent() {
        timestamp = System.currentTimeMillis();
    }
}
