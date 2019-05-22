package cn.wildfirechat.sdk;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.IMResult;

public class UserAdmin {
    public static IMResult<InputOutputUserInfo> getUserByName(String mobile) throws Exception {
        String path = APIPath.Get_User_Info;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(null, mobile);
        return HttpUtils.httpJsonPost(path, getUserInfo, InputOutputUserInfo.class);
    }

    public static IMResult<OutputCreateUser> createUser(InputOutputUserInfo user) throws Exception {
        String path = APIPath.Create_User;
        return HttpUtils.httpJsonPost(path, user, OutputCreateUser.class);
    }

    public static IMResult<OutputGetIMTokenData> getUserToken(String userId, String clientId) throws Exception {
        String path = APIPath.Get_User_Token;
        InputGetToken getToken = new InputGetToken(userId, clientId);
        return HttpUtils.httpJsonPost(path, getToken, OutputGetIMTokenData.class);
    }
}
