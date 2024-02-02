package cn.wildfirechat.pojos;

public class InputConferenceRequest {
    public String userId;
    public String clientId;
    public String request;
    public long sessionId;
    public String roomId;
    public String data;
    public boolean advance;

    public InputConferenceRequest() {
    }

    public InputConferenceRequest(String userId, String clientId, String request, long sessionId, String roomId, String data, boolean advance) {
        this.userId = userId;
        this.clientId = clientId;
        this.request = request;
        this.sessionId = sessionId;
        this.roomId = roomId;
        this.data = data;
        this.advance = advance;
    }
}
