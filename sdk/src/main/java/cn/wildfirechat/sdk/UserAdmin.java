package cn.wildfirechat.sdk;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.sdk.utilities.AdminHttpUtils;

public class UserAdmin {
    public static IMResult<InputOutputUserInfo> getUserByName(String name) throws Exception {
        String path = APIPath.User_Get_Info;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(null, name, null);
        return AdminHttpUtils.httpJsonPost(path, getUserInfo, InputOutputUserInfo.class);
    }

    public static IMResult<InputOutputUserInfo> getUserByUserId(String userId) throws Exception {
        String path = APIPath.User_Get_Info;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(userId, null, null);
        return AdminHttpUtils.httpJsonPost(path, getUserInfo, InputOutputUserInfo.class);
    }

    public static IMResult<InputOutputUserInfo> getUserByMobile(String mobile) throws Exception {
        String path = APIPath.User_Get_Info;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(null, null, mobile);
        return AdminHttpUtils.httpJsonPost(path, getUserInfo, InputOutputUserInfo.class);
    }

    public static IMResult<OutputCreateUser> createUser(InputOutputUserInfo user) throws Exception {
        String path = APIPath.Create_User;
        return AdminHttpUtils.httpJsonPost(path, user, OutputCreateUser.class);
    }

    public static IMResult<Void> updateUserInfo(InputOutputUserInfo user, int/*UpdateUserInfoMask*/ flag) throws Exception {
        String path = APIPath.Update_User;
        InputUpdateUserInfo updateUserInfo = new InputUpdateUserInfo();
        updateUserInfo.flag = flag;
        updateUserInfo.userInfo = user;
        return AdminHttpUtils.httpJsonPost(path, updateUserInfo, Void.class);
    }

    public static IMResult<OutputCreateRobot> createRobot(InputCreateRobot robot) throws Exception {
        String path = APIPath.Create_Robot;
        return AdminHttpUtils.httpJsonPost(path, robot, OutputCreateRobot.class);
    }

    /*
    销毁机器人和销毁用户是同一个接口
     */
    public static IMResult<Void> destroyRobot(String userId) throws Exception {
        String path = APIPath.Destroy_User;
        InputDestroyUser inputDestroyUser = new InputDestroyUser();
        inputDestroyUser.setUserId(userId);
        return AdminHttpUtils.httpJsonPost(path, inputDestroyUser, Void.class);
    }

    public static IMResult<OutputRobot> getRobotInfo(String robotId) throws Exception {
        String path = APIPath.User_Get_Robot_Info;
        InputRobotId getRobotInfo = new InputRobotId();
        getRobotInfo.setRobotId(robotId);
        return AdminHttpUtils.httpJsonPost(path, getRobotInfo, OutputRobot.class);
    }

    public static IMResult<OutputGetIMTokenData> getUserToken(String userId, String clientId, int platform) throws Exception {
        String path = APIPath.User_Get_Token;
        InputGetToken getToken = new InputGetToken(userId, clientId, platform);
        return AdminHttpUtils.httpJsonPost(path, getToken, OutputGetIMTokenData.class);
    }

    public static IMResult<Void> updateUserBlockStatus(String userId, int block) throws Exception {
        String path = APIPath.User_Update_Block_Status;
        InputOutputUserBlockStatus blockStatus = new InputOutputUserBlockStatus(userId, block);
        return AdminHttpUtils.httpJsonPost(path, blockStatus, Void.class);
    }

    public static IMResult<OutputUserStatus> checkUserBlockStatus(String userId) throws Exception {
        String path = APIPath.User_Check_Block_Status;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(userId, null, null);
        return AdminHttpUtils.httpJsonPost(path, getUserInfo, OutputUserStatus.class);
    }

    public static IMResult<OutputUserBlockStatusList> getBlockedList() throws Exception {
        String path = APIPath.User_Get_Blocked_List;
        return AdminHttpUtils.httpJsonPost(path, null, OutputUserBlockStatusList.class);
    }

    public static IMResult<OutputCheckUserOnline> checkUserOnlineStatus(String userId) throws Exception {
        String path = APIPath.User_Get_Online_Status;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(userId, null, null);
        return AdminHttpUtils.httpJsonPost(path, getUserInfo, OutputCheckUserOnline.class);
    }

    /* 强迫用户下线，需要用户重新获取token才能进行连接。
    userId必须有效，clientId可以为空，当为空时，踢下线说有客户端，当不为空时仅踢掉对应客户端。
     */
    public static IMResult<Void> kickoffUserClient(String userId, String clientId) throws Exception {
        String path = APIPath.User_Kickoff_Client;
        StringPairPojo pojo = new StringPairPojo(userId, clientId);
        return AdminHttpUtils.httpJsonPost(path, pojo, Void.class);
    }

    public static IMResult<Void> destroyUser(String userId) throws Exception {
        String path = APIPath.Destroy_User;
        InputDestroyUser inputDestroyUser = new InputDestroyUser();
        inputDestroyUser.setUserId(userId);
        return AdminHttpUtils.httpJsonPost(path, inputDestroyUser, Void.class);
    }

    //仅专业版支持
    public static IMResult<OutputCreateDevice> createOrUpdateDevice(InputCreateDevice device) throws Exception {
        String path = APIPath.CreateOrUpdate_Device;
        return AdminHttpUtils.httpJsonPost(path, device, OutputCreateDevice.class);
    }

    //仅专业版支持
    public static IMResult<OutputDevice> getDevice(String deviceId) throws Exception {
        String path = APIPath.Get_Device;
        InputDeviceId inputDeviceId = new InputDeviceId();
        inputDeviceId.setDeviceId(deviceId);
        return AdminHttpUtils.httpJsonPost(path, inputDeviceId, OutputDevice.class);
    }

    public static IMResult<OutputDeviceList> getUserDevices(String userId) throws Exception {
        String path = APIPath.Get_User_Devices;
        InputUserId inputUserId = new InputUserId();
        inputUserId.setUserId(userId);
        return AdminHttpUtils.httpJsonPost(path, inputUserId, OutputDeviceList.class);
    }

    public static IMResult<GetOnlineUserCountResult> getOnlineUserCount() throws Exception {
        return AdminHttpUtils.httpJsonPost(APIPath.User_Online_Count, null, GetOnlineUserCountResult.class);
    }

    public static IMResult<GetOnlineUserResult> getOnlineUser(int nodeId, int offset, int count) throws Exception {
        GetOnlineUserRequest request = new GetOnlineUserRequest();
        request.nodeId = nodeId;
        request.offset = offset;
        request.count = count;
        return AdminHttpUtils.httpJsonPost(APIPath.User_Online_List, request, GetOnlineUserResult.class);
    }

    public static IMResult<OutputApplicationUserInfo> applicationGetUserInfo(String authCode) throws Exception {
        String path = APIPath.User_Application_Get_UserInfo;
        InputApplicationGetUserInfo input = new InputApplicationGetUserInfo();
        input.setAuthCode(authCode);
        return AdminHttpUtils.httpJsonPost(path, input, OutputApplicationUserInfo.class);
    }
}
