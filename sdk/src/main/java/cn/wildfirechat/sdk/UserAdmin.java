package cn.wildfirechat.sdk;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.IMResult;

import java.util.List;

public class UserAdmin {
    public static IMResult<InputOutputUserInfo> getUserByName(String mobile) throws Exception {
        String path = APIPath.User_Get_Info;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(null, mobile);
        return HttpUtils.httpJsonPost(path, getUserInfo, InputOutputUserInfo.class);
    }

    public static IMResult<OutputCreateUser> createUser(InputOutputUserInfo user) throws Exception {
        String path = APIPath.Create_User;
        return HttpUtils.httpJsonPost(path, user, OutputCreateUser.class);
    }

    public static IMResult<OutputGetIMTokenData> getUserToken(String userId, String clientId) throws Exception {
        String path = APIPath.User_Get_Token;
        InputGetToken getToken = new InputGetToken(userId, clientId);
        return HttpUtils.httpJsonPost(path, getToken, OutputGetIMTokenData.class);
    }

    public static IMResult<Void> updateUserBlockStatus(String userId, int block) throws Exception {
        String path = APIPath.User_Update_Block_Status;
        InputOutputUserBlockStatus blockStatus = new InputOutputUserBlockStatus(userId, block);
        return HttpUtils.httpJsonPost(path, blockStatus, Void.class);
    }

    public static IMResult<OutputUserStatus> checkUserBlockStatus(String userId) throws Exception {
        String path = APIPath.User_Update_Block_Status;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(userId, null);
        return HttpUtils.httpJsonPost(path, getUserInfo, OutputUserStatus.class);
    }

    public static IMResult<OutputUserBlockStatusList> getBlockedList() throws Exception {
        String path = APIPath.User_Get_Blocked_List;
        return HttpUtils.httpJsonPost(path, null, OutputUserBlockStatusList.class);
    }

    //not implement
//    public static IMResult<OutputUserBlockStatusList> getUserOnlineStatus(String userId) throws Exception {
//        String path = APIPath.User_Get_Online_Status;
//        InputGetUserInfo inputGetUserInfo = new InputGetUserInfo(userId, null);
//        return HttpUtils.httpJsonPost(path, null, OutputUserBlockStatusList.class);
//    }




}
