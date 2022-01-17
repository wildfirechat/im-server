package cn.wildfirechat.pojos;

import java.util.ArrayList;

public class GetOnlineUserResult {
    public static class UserClient {
        public String userId;
        public String clientId;
        public int platform;
    }

    ArrayList<GetOnlineUserResult.UserClient> userClients;
    public int totalCount;
    public int offset;
}
