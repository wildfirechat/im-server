package cn.wildfirechat.sdk;

import cn.wildfirechat.sdk.model.*;

public class ChatAdmin {
    public static void init(String url, String secret) {
        HttpUtils.init(url, secret);
    }

    public static IMResult<User> getUserByName(String mobile) throws Exception {
        String path = "/admin/user/info";
        UserName name = new UserName(mobile);
        return HttpUtils.httpJsonPost(path, name, User.class);
    }

    public static IMResult<UserId> createUser(User user) throws Exception {
        String path = "/admin/user/create";
        return HttpUtils.httpJsonPost(path, user, UserId.class);
    }

    public static IMResult<Token> getUserToken(String userId, String clientId) throws Exception {
        String path = "/admin/user/token";
        return HttpUtils.httpJsonPost(path, new GetTokenRequest(userId, clientId), Token.class);
    }
}
