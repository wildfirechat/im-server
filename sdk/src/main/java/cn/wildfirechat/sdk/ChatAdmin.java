package cn.wildfirechat.sdk;

import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.*;

public class ChatAdmin {
    public static void init(String url, String secret) {
        HttpUtils.init(url, secret);
    }

    public static IMResult<InputOutputUserInfo> getUserByName(String mobile) throws Exception {
        String path = "/admin/user/info";
        InputGetUserInfo getUserInfo = new InputGetUserInfo(null, mobile);
        return HttpUtils.httpJsonPost(path, getUserInfo, InputOutputUserInfo.class);
    }

    public static IMResult<OutputCreateUser> createUser(InputOutputUserInfo user) throws Exception {
        String path = "/admin/user/create";
        return HttpUtils.httpJsonPost(path, user, OutputCreateUser.class);
    }

    public static IMResult<OutputGetIMTokenData> getUserToken(String userId, String clientId) throws Exception {
        String path = "/admin/user/token";
        InputGetToken getToken = new InputGetToken(userId, clientId);
        return HttpUtils.httpJsonPost(path, getToken, OutputGetIMTokenData.class);
    }
}
