package cn.wildfirechat.pojos;

public class ConferenceLeaveEvent {
    public String userId;
    public String roomId;
    public boolean rejoin;
    public boolean kicked;
    public boolean screenSharing;
    public long timestamp;

    public ConferenceLeaveEvent() {
        timestamp = System.currentTimeMillis();
    }
}
