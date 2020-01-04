package cn.wildfirechat.pojos;

public class UserOnlineStatus {
    public static final int ONLINE = 0;
    public static final int OFFLINE = 1;
    public static final int LOGOUT = -1;

    public String userId;
    public String clientId;
    public int platform;
    public int status;
    public long timestamp;

    public UserOnlineStatus(String userId, String clientId, int platform, int status) {
        this.userId = userId;
        this.clientId = clientId;
        this.platform = platform;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }
}
