package cn.wildfirechat.sdk;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.pojos.mesh.PojoSearchUserRes;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.sdk.utilities.AdminHttpUtils;

import java.util.List;

/**
 * 用户管理类
 * <p>
 * 提供用户管理相关的功能，包括：
 * <ul>
 * <li>用户信息的获取、创建、更新、销毁</li>
 * <li>按用户名称搜索用户</li>
 * <li>机器人管理</li>
 * <li>用户封禁状态管理</li>
 * <li>用户在线状态查询</li>
 * <li>用户设备管理</li>
 * <li>用户Token管理</li>
 * </ul>
 * </p>
 */
public class UserAdmin {
    /**
     * 根据用户名获取用户信息（不包含已删除用户）
     * @param name 用户名
     * @return 用户信息
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<InputOutputUserInfo> getUserByName(String name) throws Exception {
        return getUserByName(name, false);
    }

    /**
     * 根据用户名获取用户信息
     * @param name 用户名
     * @param includeDeleted 是否包含已删除的用户
     * @return 用户信息
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<InputOutputUserInfo> getUserByName(String name, boolean includeDeleted) throws Exception {
        String path = APIPath.User_Get_Info;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(null, name, null, includeDeleted);
        return AdminHttpUtils.httpJsonPost(path, getUserInfo, InputOutputUserInfo.class);
    }

    /**
     * 根据用户ID获取用户信息（不包含已删除用户）
     * @param userId 用户ID
     * @return 用户信息
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<InputOutputUserInfo> getUserByUserId(String userId) throws Exception {
        return getUserByUserId(userId, false);
    }

    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @param includeDeleted 是否包含已删除的用户
     * @return 用户信息
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<InputOutputUserInfo> getUserByUserId(String userId, boolean includeDeleted) throws Exception {
        String path = APIPath.User_Get_Info;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(userId, null, null, includeDeleted);
        return AdminHttpUtils.httpJsonPost(path, getUserInfo, InputOutputUserInfo.class);
    }

    /**
     * 根据用户名称搜索用户（第一页，20条）
     * <p>对用户名和昵称进行模糊匹配，仅返回未删除的普通用户和机器人。</p>
     * @param keyword 用户名称关键词
     * @return 搜索结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<PojoSearchUserRes> searchUserByName(String keyword) throws Exception {
        return searchUserByName(keyword, 0);
    }

    /**
     * 根据用户名称搜索用户
     * <p>对用户名和昵称进行模糊匹配，仅返回未删除的普通用户和机器人。每页默认20条记录。</p>
     * @param keyword 用户名称关键词
     * @param page 页码，从0开始
     * @return 搜索结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<PojoSearchUserRes> searchUserByName(String keyword, int page) throws Exception {
        return searchUserByName(keyword, page, 20);
    }

    /**
     * 根据用户名称搜索用户
     * <p>对用户名和昵称进行模糊匹配，仅返回未删除的普通用户和机器人。</p>
     * @param keyword 用户名称关键词
     * @param page 页码，从0开始
     * @param count 每页数量，为0时服务端使用默认值20，最大100
     * @return 搜索结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<PojoSearchUserRes> searchUserByName(String keyword, int page, int count) throws Exception {
        String path = APIPath.User_Search_By_Name;
        PojoSearchUserByNameReq req = new PojoSearchUserByNameReq();
        req.keyword = keyword;
        req.page = page;
        req.count = count;
        return AdminHttpUtils.httpJsonPost(path, req, PojoSearchUserRes.class);
    }

    /**
     * 根据手机号获取用户信息（不包含已删除用户）
     * @deprecated 用户手机号码不要求唯一，正常应该业务系统保证电话号码唯一。如果业务系统不限制电话号码唯一，可能一个号码有多个用户，此方法只能返回一个，此时建议使用 {@link #getUsersByMobile(String mobile, boolean includeDeleted)}
     * @param mobile 手机号
     * @return 用户信息
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<InputOutputUserInfo> getUserByMobile(String mobile) throws Exception {
        return getUserByMobile(mobile, false);
    }

    /**
     * 根据手机号获取用户信息，
     * @deprecated 用户手机号码不要求唯一，正常应该业务系统保证电话号码唯一。如果业务系统不限制电话号码唯一，可能一个号码有多个用户，此方法只能返回一个，此时建议使用 {@link #getUsersByMobile(String mobile, boolean includeDeleted)}
     * @param mobile 手机号
     * @param includeDeleted 是否包含已删除的用户
     * @return 用户信息
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<InputOutputUserInfo> getUserByMobile(String mobile, boolean includeDeleted) throws Exception {
        String path = APIPath.User_Get_Info;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(null, null, mobile, includeDeleted);
        return AdminHttpUtils.httpJsonPost(path, getUserInfo, InputOutputUserInfo.class);
    }

    /**
     * 根据电话号码获取用户信息列表
     * @param mobile 邮箱地址
     * @param includeDeleted 是否包含已删除的用户
     * @return 用户信息列表
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputUserInfoList> getUsersByMobile(String mobile, boolean includeDeleted) throws Exception {
        String path = APIPath.User_Get_By_Mobile;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(null, null, mobile, includeDeleted);
        return AdminHttpUtils.httpJsonPost(path, getUserInfo, OutputUserInfoList.class);
    }

    /**
     * 根据邮箱获取用户信息列表
     * @param email 邮箱地址
     * @return 用户信息列表
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputUserInfoList> getUserByEmail(String email) throws Exception {
        String path = APIPath.User_Get_Email_Info;
        return AdminHttpUtils.httpJsonPost(path, email, OutputUserInfoList.class);
    }

    /**
     * 获取所有用户列表（分页）
     * @param count 每页数量
     * @param offset 偏移量
     * @return 用户列表
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputGetUserList> getAllUsers(int count, int offset) throws Exception {
        String path = APIPath.User_Get_All;
        InputGetUserList input = new InputGetUserList();
        input.count = count;
        input.offset = offset;
        return AdminHttpUtils.httpJsonPost(path, input, OutputGetUserList.class);
    }

    /**
     * 获取所有机器人列表（分页）
     * @param count 每页数量
     * @param offset 偏移量
     * @return 机器人列表
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputGetRobotList> getAllRobots(int count, int offset) throws Exception {
        String path = APIPath.User_Get_All_Robots;
        InputGetUserList input = new InputGetUserList();
        input.count = count;
        input.offset = offset;
        return AdminHttpUtils.httpJsonPost(path, input, OutputGetRobotList.class);
    }

    /**
     * 批量获取用户信息
     * @param userIds 用户ID列表
     * @return 用户信息列表
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputUserInfoList> getBatchUsers(List<String> userIds) throws Exception {
        String path = APIPath.User_Batch_Get_Infos;
        InputStringList input = new InputStringList();
        input.setList(userIds);
        return AdminHttpUtils.httpJsonPost(path, input, OutputUserInfoList.class);
    }

    /**
     * 创建用户
     * @param user 用户信息
     * @return 创建结果，包含用户ID
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputCreateUser> createUser(InputOutputUserInfo user) throws Exception {
        String path = APIPath.Create_User;
        return AdminHttpUtils.httpJsonPost(path, user, OutputCreateUser.class);
    }

    /**
     * 更新用户信息
     * @param user 用户信息
     * @param flag 更新标志位，指定要更新的字段（UpdateUserInfoMask）
     * @return 更新结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<Void> updateUserInfo(InputOutputUserInfo user, int/*UpdateUserInfoMask*/ flag) throws Exception {
        String path = APIPath.Update_User;
        InputUpdateUserInfo updateUserInfo = new InputUpdateUserInfo();
        updateUserInfo.flag = flag;
        updateUserInfo.userInfo = user;
        return AdminHttpUtils.httpJsonPost(path, updateUserInfo, Void.class);
    }

    /**
     * 创建机器人
     * @param robot 机器人信息
     * @return 创建结果，包含机器人ID和Token
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputCreateRobot> createRobot(InputCreateRobot robot) throws Exception {
        String path = APIPath.Create_Robot;
        return AdminHttpUtils.httpJsonPost(path, robot, OutputCreateRobot.class);
    }

    /**
     * 销毁机器人
     * <p>销毁机器人和销毁用户使用同一个接口</p>
     * @param userId 机器人用户ID
     * @return 销毁结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<Void> destroyRobot(String userId) throws Exception {
        String path = APIPath.Destroy_User;
        InputDestroyUser inputDestroyUser = new InputDestroyUser();
        inputDestroyUser.setUserId(userId);
        return AdminHttpUtils.httpJsonPost(path, inputDestroyUser, Void.class);
    }

    /**
     * 获取机器人信息
     * @param robotId 机器人ID
     * @return 机器人信息
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputRobot> getRobotInfo(String robotId) throws Exception {
        String path = APIPath.User_Get_Robot_Info;
        InputRobotId getRobotInfo = new InputRobotId();
        getRobotInfo.setRobotId(robotId);
        return AdminHttpUtils.httpJsonPost(path, getRobotInfo, OutputRobot.class);
    }

    /**
     * 获取用户的机器人列表
     * @param userId 用户ID
     * @return 机器人ID列表
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputStringList> getUserRobots(String userId) throws Exception {
        String path = APIPath.User_Get_User_Robots;
        InputUserId getRobotInfo = new InputUserId();
        getRobotInfo.setUserId(userId);
        return AdminHttpUtils.httpJsonPost(path, getRobotInfo, OutputStringList.class);
    }

    /**
     * 获取用户的IM Token
     * @param userId 用户ID
     * @param clientId 客户端ID
     * @param platform 平台类型
     * @return Token信息
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputGetIMTokenData> getUserToken(String userId, String clientId, int platform) throws Exception {
        String path = APIPath.User_Get_Token;
        InputGetToken getToken = new InputGetToken(userId, clientId, platform);
        return AdminHttpUtils.httpJsonPost(path, getToken, OutputGetIMTokenData.class);
    }

    /**
     * 更新用户封禁状态
     * @param userId 用户ID
     * @param block 封禁状态：0-正常，1-封禁
     * @return 更新结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<Void> updateUserBlockStatus(String userId, int block) throws Exception {
        String path = APIPath.User_Update_Block_Status;
        InputOutputUserBlockStatus blockStatus = new InputOutputUserBlockStatus(userId, block);
        return AdminHttpUtils.httpJsonPost(path, blockStatus, Void.class);
    }

    /**
     * 检查用户封禁状态
     * @param userId 用户ID
     * @return 用户封禁状态
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputUserStatus> checkUserBlockStatus(String userId) throws Exception {
        String path = APIPath.User_Check_Block_Status;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(userId, null, null);
        return AdminHttpUtils.httpJsonPost(path, getUserInfo, OutputUserStatus.class);
    }

    /**
     * 获取被封禁用户列表
     * @return 被封禁用户列表
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputUserBlockStatusList> getBlockedList() throws Exception {
        String path = APIPath.User_Get_Blocked_List;
        return AdminHttpUtils.httpJsonPost(path, null, OutputUserBlockStatusList.class);
    }

    /**
     * 检查用户在线状态
     * @param userId 用户ID
     * @return 用户在线状态
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputCheckUserOnline> checkUserOnlineStatus(String userId) throws Exception {
        String path = APIPath.User_Get_Online_Status;
        InputGetUserInfo getUserInfo = new InputGetUserInfo(userId, null, null);
        return AdminHttpUtils.httpJsonPost(path, getUserInfo, OutputCheckUserOnline.class);
    }

    /**
     * 强迫用户下线
     * <p>
     * 强迫用户下线后，用户需要重新获取token才能进行连接。
     * userId必须有效，clientId可以为空。
     * 当clientId为空时，踢下线所有客户端；当不为空时仅踢掉对应客户端。
     * </p>
     * @param userId 用户ID
     * @param clientId 客户端ID，为空时踢下线所有客户端
     * @return 下线结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<Void> kickoffUserClient(String userId, String clientId) throws Exception {
        String path = APIPath.User_Kickoff_Client;
        StringPairPojo pojo = new StringPairPojo(userId, clientId);
        return AdminHttpUtils.httpJsonPost(path, pojo, Void.class);
    }

    /**
     * 销毁用户
     * @param userId 用户ID
     * @return 销毁结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<Void> destroyUser(String userId) throws Exception {
        String path = APIPath.Destroy_User;
        InputDestroyUser inputDestroyUser = new InputDestroyUser();
        inputDestroyUser.setUserId(userId);
        return AdminHttpUtils.httpJsonPost(path, inputDestroyUser, Void.class);
    }

    /**
     * 创建或更新设备信息（仅专业版支持）
     * @param device 设备信息
     * @return 创建或更新结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputCreateDevice> createOrUpdateDevice(InputCreateDevice device) throws Exception {
        String path = APIPath.CreateOrUpdate_Device;
        return AdminHttpUtils.httpJsonPost(path, device, OutputCreateDevice.class);
    }

    /**
     * 获取设备信息（仅专业版支持）
     * @param deviceId 设备ID
     * @return 设备信息
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputDevice> getDevice(String deviceId) throws Exception {
        String path = APIPath.Get_Device;
        InputDeviceId inputDeviceId = new InputDeviceId();
        inputDeviceId.setDeviceId(deviceId);
        return AdminHttpUtils.httpJsonPost(path, inputDeviceId, OutputDevice.class);
    }

    /**
     * 获取用户的设备列表
     * @param userId 用户ID
     * @return 用户设备列表
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputDeviceList> getUserDevices(String userId) throws Exception {
        String path = APIPath.Get_User_Devices;
        InputUserId inputUserId = new InputUserId();
        inputUserId.setUserId(userId);
        return AdminHttpUtils.httpJsonPost(path, inputUserId, OutputDeviceList.class);
    }

    /**
     * 获取在线用户数量
     * @return 在线用户数量统计结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<GetOnlineUserCountResult> getOnlineUserCount() throws Exception {
        return AdminHttpUtils.httpJsonPost(APIPath.User_Online_Count, null, GetOnlineUserCountResult.class);
    }

    /**
     * 获取在线用户列表（分页）
     * @param nodeId 节点ID，用于分布式部署场景
     * @param offset 偏移量
     * @param count 每页数量
     * @return 在线用户列表
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<GetOnlineUserResult> getOnlineUser(int nodeId, int offset, int count) throws Exception {
        GetOnlineUserRequest request = new GetOnlineUserRequest();
        request.nodeId = nodeId;
        request.offset = offset;
        request.count = count;
        return AdminHttpUtils.httpJsonPost(APIPath.User_Online_List, request, GetOnlineUserResult.class);
    }

    /**
     * 通过应用授权码获取用户信息
     * @param authCode 应用授权码
     * @return 用户信息
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputApplicationUserInfo> applicationGetUserInfo(String authCode) throws Exception {
        String path = APIPath.User_Application_Get_UserInfo;
        InputApplicationGetUserInfo input = new InputApplicationGetUserInfo();
        input.setAuthCode(authCode);
        return AdminHttpUtils.httpJsonPost(path, input, OutputApplicationUserInfo.class);
    }

    /**
     * 获取用户会话信息
     * @param userId 用户ID
     * @return 用户会话信息
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<GetUserSessionResult> getUserSession(String userId) throws Exception {
        InputUserId inputUserId = new InputUserId(userId);
        return AdminHttpUtils.httpJsonPost(APIPath.User_Session_List, inputUserId, GetUserSessionResult.class);
    }

    /**
     * 获取用户的未读数，只有专业版IM服务支持，未读数不是很准确，只能表示一个大概。
     * @param userId
     * @return 用户的未读数，包括未接收消息和客户端上报的未读数。
     * @throws Exception
     */
    public static IMResult<OutputUserUnreadCount> getUserUnreadCount(String userId) throws Exception {
        InputUserId inputUserId = new InputUserId(userId);
        return AdminHttpUtils.httpJsonPost(APIPath.User_Unread_count, inputUserId, OutputUserUnreadCount.class);
    }
}
