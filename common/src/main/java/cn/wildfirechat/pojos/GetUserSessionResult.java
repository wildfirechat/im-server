package cn.wildfirechat.pojos;

import java.util.ArrayList;
import java.util.List;

public class GetUserSessionResult {
    public static class UserSession {
        public String userId;
        public String clientId;
        public int platform;
        public int pushType;
        public String deviceToken;
        public String deviceVoipToken;
        boolean isOnline;

        public UserSession() {
        }

        public UserSession(String userId, String clientId, int platform, int pushType, String deviceToken, String deviceVoipToken, boolean isOnline) {
            this.userId = userId;
            this.clientId = clientId;
            this.platform = platform;
            this.pushType = pushType;
            this.deviceToken = deviceToken;
            this.deviceVoipToken = deviceVoipToken;
            this.isOnline = isOnline;
        }
    }

    public List<UserSession> userSessions;

    public GetUserSessionResult() {
        userSessions = new ArrayList<>();
    }
}
