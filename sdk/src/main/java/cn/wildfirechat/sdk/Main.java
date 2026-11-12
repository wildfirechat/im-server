package cn.wildfirechat.sdk;

import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.mesh.PojoDomainPingResponse;
import cn.wildfirechat.pojos.mesh.PojoSearchUserRes;
import cn.wildfirechat.pojos.mesh.PojoUserConferenceResponse;
import cn.wildfirechat.pojos.moments.CommentPojo;
import cn.wildfirechat.pojos.moments.FeedPojo;
import cn.wildfirechat.pojos.moments.FeedsPojo;
import cn.wildfirechat.pojos.moments.MomentProfilePojo;
import cn.wildfirechat.proto.WFCMessage;
import cn.wildfirechat.sdk.messagecontent.*;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.sdk.model.IMResult;
import com.google.gson.GsonBuilder;
import io.netty.util.internal.StringUtil;


import java.io.File;
import java.time.LocalDate;
import java.util.*;

import static cn.wildfirechat.pojos.MyInfoType.Modify_DisplayName;
import static cn.wildfirechat.proto.ProtoConstants.ChannelState.*;
import static cn.wildfirechat.proto.ProtoConstants.SystemSettingType.Group_Max_Member_Count;

/**
 * 野火IM Server SDK示例程序主类
 * <p>
 * 提供SDK功能演示的示例代码，包括：
 * <ul>
 * <li>用户管理操作</li>
 * <li>群组管理操作</li>
 * <li>消息发送操作</li>
 * <li>好友关系管理</li>
 * <li>朋友圈功能</li>
 * <li>机器人功能</li>
 * </ul>
 * </p>
 */
public class Main {
    /** 是否为商业版服务器 */
    private static boolean commercialServer = false;
    /** 是否启用高级音视频功能 */
    private static boolean advanceVoip = false;
    /** 是否启用机器人朋友圈功能 */
    private static boolean robotMomentsEnabled = false;
    /** 是否启用朋友圈功能 */
    private static boolean momentsEnabled = false;
    /** 管理端口是18080 */
    private static String AdminUrl = "http://localhost:18080";
    /** 管理员密钥 */
    private static String AdminSecret = "123456";

    /** 机器人和频道使用IM服务的公开端口80，注意不是18080 */
    private static String IMUrl = "http://localhost";


    /**
     * 程序主入口
     * @param args 命令行参数，包括：adminUrl、adminSecret、imUrl、commercialServer、advanceVoip
     * @throws Exception 当测试过程中发生错误时抛出异常
     */
    public static void main(String[] args) throws Exception {
        // 解析命令行参数
        if (args.length == 5) {
            // 从命令行参数获取配置信息
            AdminUrl = args[0];
            AdminSecret = args[1];
            IMUrl = args[2];
            commercialServer = Boolean.parseBoolean(args[3]);
            advanceVoip = Boolean.parseBoolean(args[4]);
        } else {
            // 显示帮助信息或使用默认值
            if(args.length == 1 && (args[0].equals("-h") || args[0].equals("--help") || args[0].equals("-help"))) {
                System.out.println("Usage: java -jar checker.jar adminUrl adminSecret imUrl commercialServer advanceVoip \n      e.g. java -jar checker.jar http://192.168.1.80:18080 123456 http://192.168.1.80 false false");
                return;
            }
            System.out.println("Usage: java -jar checker.jar adminUrl adminSecret imUrl commercialServer advanceVoip \n      e.g. java -jar checker.jar http://192.168.1.80:18080 123456 http://192.168.1.80 false false");
            System.out.println();
            System.out.println();
            System.out.println("Use default value: java -jar checker.jar http://127.0.0.1:18080 123456 http://127.0.0.1 false false");
        }


        //admin使用的是18080端口，超级管理接口，理论上不能对外开放端口，也不能让非内部服务知悉密钥。
        // 执行管理员API测试
        testAdmin();

        //测试解析数据库中消息内容
        testReadMessageContentFromDB();

        //计算消息分表
        testMessageSharding();

        //Robot和Channel都是使用的80端口，第三方可以创建或者为第三方创建，第三方可以使用robot或者channel与IM系统进行对接。
        // 测试机器人功能
        testRobot();
        // 测试频道功能
        testChannel();

        //测试Mesh相关API，用于分布式IM。Mesh相关测试依赖环境，不具备测绘条件，注释掉。
        //testMesh();
    }


    /**
     * 管理员API测试总入口
     * <p>
     * 测试所有管理员功能，包括：
     * <ul>
     * <li>用户管理</li>
     * <li>用户关系管理</li>
     * <li>群组管理</li>
     * <li>聊天室管理</li>
     * <li>消息管理</li>
     * <li>消息内容测试</li>
     * <li>频道API测试</li>
     * <li>通用API测试</li>
     * <li>敏感词API测试</li>
     * <li>设备管理测试(仅商业版)</li>
     * <li>会议功能测试(仅高级音视频版)</li>
     * <li>朋友圈功能测试(仅启用时)</li>
     * </ul>
     * </p>
     * @throws Exception 当测试过程中发生错误时抛出异常
     */
    static void testAdmin() throws Exception {
        //初始化服务API，使用管理员URL和密钥
        AdminConfig.initAdmin(AdminUrl, AdminSecret);

        // 执行各类管理员API测试
        testUser();           // 用户管理测试
        testUserRelation();   // 用户关系测试
        testGroup();          // 群组管理测试
        testChatroom();       // 聊天室测试
        testMessage();        // 消息发送测试
        testMessageContent(); // 消息内容编码测试
        testChannelApi();     // 频道API测试
        testGeneralApi();     // 通用API测试
        testSensitiveApi();   // 敏感词API测试

        // 商业版功能测试
        if (commercialServer) {
            testDevice();     // 设备测试(仅商业版)
        }

        // 高级音视频功能测试
        if(advanceVoip) {
            testConference(); // 会议功能测试(仅高级音视频版)
        }

        // 朋友圈功能测试
        if (momentsEnabled) {
            testMomentsApi(); // 朋友圈API测试
        }

        // 商业版功能测试
        if (commercialServer) {
            testImportMessages();     // 导入消息测试(仅商业版)
        }


        System.out.println("Congratulation, all admin test case passed!!!!!!!");
    }

    //***********************************************
    //****  用户相关的API
    //***********************************************
    /**
     * 用户管理API测试
     * <p>
     * 测试以下功能：
     * <ul>
     * <li>创建普通用户</li>
     * <li>创建和删除机器人</li>
     * <li>获取用户信息（按用户名、手机号、用户ID、邮箱）</li>
     * <li>批量获取用户信息</li>
     * <li>更新用户信息</li>
     * <li>获取用户IM Token</li>
     * <li>用户封禁/解封</li>
     * <li>检查用户在线状态</li>
     * <li>销毁用户（慎用）</li>
     * <li>获取所有用户列表</li>
     * <li>获取在线用户数量和列表（仅商业版）</li>
     * <li>获取用户会话信息（仅商业版）</li>
     * </ul>
     * </p>
     * @throws Exception 当测试过程中发生错误时抛出异常
     */
    static void testUser() throws Exception {
        // 创建用户信息对象
        InputOutputUserInfo userInfo = new InputOutputUserInfo();
        //用户ID，必须保证唯一性
        userInfo.setUserId("userId1");
        //用户名，一般是用户登录帐号，也必须保证唯一性。也就是说所有用户的userId必须不能重复，所有用户的name必须不能重复，但可以同一个用户的userId和name是同一个，一般建议userId使用一个uuid，name是"微信号"且可以修改，
        userInfo.setName("user1");
        userInfo.setMobile("13900000001");
        userInfo.setDisplayName("user 1");

        // 调用SDK创建用户
        IMResult<OutputCreateUser> resultCreateUser = UserAdmin.createUser(userInfo);
        if (resultCreateUser != null && resultCreateUser.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Create user " + resultCreateUser.getResult().getName() + " success");
        } else {
            System.out.println("Create user failure");
            System.exit(-1);
        }

        // 创建机器人信息对象
        InputCreateRobot createRobot = new InputCreateRobot();
        createRobot.setUserId("robot1");
        createRobot.setName("robot1");
        createRobot.setDisplayName("机器人");
        createRobot.setOwner("userId1");
        createRobot.setSecret("123456");
        createRobot.setCallback("http://127.0.0.1:8883/robot/recvmsg");
        // 调用SDK创建机器人
        IMResult<OutputCreateRobot> resultCreateRobot = UserAdmin.createRobot(createRobot);
        if (resultCreateRobot != null && resultCreateRobot.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Create robot " + resultCreateRobot.getResult().getUserId() + " success");
        } else {
            System.out.println("Create robot failure");
            System.exit(-1);
        }

        // 获取机器人信息
        IMResult<OutputRobot> outputRobotIMResult = UserAdmin.getRobotInfo("robot1");
        if(outputRobotIMResult != null && outputRobotIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Get robot success");
        } else {
            System.out.println("Get robot failure");
            System.exit(-1);
        }

        // 销毁机器人
        IMResult<Void> destroyResult = UserAdmin.destroyRobot("robot1");
        if(destroyResult != null && destroyResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("success");
        } else {
            System.out.println("destroy user failure");
            System.exit(-1);
        }

        // 通过用户名获取用户信息
        IMResult<InputOutputUserInfo> resultGetUserInfo1 = UserAdmin.getUserByName(userInfo.getName());
        if (resultGetUserInfo1 != null && resultGetUserInfo1.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            if (userInfo.getUserId().equals(resultGetUserInfo1.getResult().getUserId())
                && userInfo.getName().equals(resultGetUserInfo1.getResult().getName())
                && userInfo.getMobile().equals(resultGetUserInfo1.getResult().getMobile())
                && userInfo.getDisplayName().equals(resultGetUserInfo1.getResult().getDisplayName())) {
                System.out.println("get user info success");
            } else {
                System.out.println("get user info by name failure");
                System.exit(-1);
            }
        } else {
            System.out.println("get user info by name failure");
            System.exit(-1);
        }

        // 通过手机号获取用户信息
        IMResult<InputOutputUserInfo> resultGetUserInfo2 = UserAdmin.getUserByMobile(userInfo.getMobile());
        if (resultGetUserInfo2 != null && resultGetUserInfo2.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            if (userInfo.getUserId().equals(resultGetUserInfo2.getResult().getUserId())
                && userInfo.getName().equals(resultGetUserInfo2.getResult().getName())
                && userInfo.getMobile().equals(resultGetUserInfo2.getResult().getMobile())
                && userInfo.getDisplayName().equals(resultGetUserInfo2.getResult().getDisplayName())) {
                System.out.println("get user info success");
            } else {
                System.out.println("get user info by mobile failure");
//                System.exit(-1);
            }
        } else {
            System.out.println("get user info by mobile failure");
            System.exit(-1);
        }

        // 通过手机号获取用户信息，如果一个手机号码属于多个用户，这里都会返回。
        IMResult<OutputUserInfoList> resultGetUsers = UserAdmin.getUsersByMobile("13900000001", false);
        if (resultGetUsers != null && resultGetUsers.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Get user success");
        } else {
            System.out.println("Create user failure");
            System.exit(-1);
        }

        // 通过用户ID获取用户信息
        IMResult<InputOutputUserInfo> resultGetUserInfo3 = UserAdmin.getUserByUserId(userInfo.getUserId());
        if (resultGetUserInfo3 != null && resultGetUserInfo3.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            if (userInfo.getUserId().equals(resultGetUserInfo3.getResult().getUserId())
                && userInfo.getName().equals(resultGetUserInfo3.getResult().getName())
                && userInfo.getMobile().equals(resultGetUserInfo3.getResult().getMobile())
                && userInfo.getDisplayName().equals(resultGetUserInfo3.getResult().getDisplayName())) {
                System.out.println("get user info success");
            } else {
                System.out.println("get user info by userId failure");
                System.exit(-1);
            }
        } else {
            System.out.println("get user info by userId failure");
            System.exit(-1);
        }

        // 通过邮箱获取用户信息（允许用户不存在）
        IMResult<OutputUserInfoList> userInfoListIMResult = UserAdmin.getUserByEmail("13900000001@139.com");
        if(userInfoListIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS || userInfoListIMResult.getErrorCode() == ErrorCode.ERROR_CODE_NOT_EXIST) {
            System.out.println("getUserByEmail success");
        } else {
            System.out.println("getUserByEmail failure");
            System.exit(-1);
        }

        // 批量获取用户信息
        IMResult<OutputUserInfoList> batchGetUsers = UserAdmin.getBatchUsers(Arrays.asList("userId1", "admin", "FireRobot", "TestUser"));
        if (batchGetUsers.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get batch user success");
        } else {
            System.out.println("get batch user failure");
            System.exit(-1);
        }

        IMResult<PojoSearchUserRes> searchUserResIMResult = UserAdmin.searchUserByName("user 1");
        if (searchUserResIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get batch user success");
        } else {
            System.out.println("get batch user failure");
            System.exit(-1);
        }

        // 准备更新用户信息（先测试不存在的用户ID）
        InputOutputUserInfo updateUserInfo = new InputOutputUserInfo();
        updateUserInfo.setUserId(System.currentTimeMillis()+"");
        updateUserInfo.setDisplayName("updatedUserName");
        updateUserInfo.setPortrait("updatedUserPortrait");
        // 设置更新标志：更新显示名称和头像
        int updateUserFlag = ProtoConstants.UpdateUserInfoMask.Update_User_DisplayName | ProtoConstants.UpdateUserInfoMask.Update_User_Portrait;
        IMResult<Void> result = UserAdmin.updateUserInfo(updateUserInfo, updateUserFlag);
        if(result != null && result.getErrorCode() == ErrorCode.ERROR_CODE_NOT_EXIST) {
            System.out.println("updateUserInfo success");
        } else {
            System.out.println("updateUserInfo failure");
            System.exit(-1);
        }

        // 更新存在的用户信息
        updateUserInfo.setUserId(userInfo.getUserId());
        result = UserAdmin.updateUserInfo(updateUserInfo, updateUserFlag);
        if(result != null && result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("updateUserInfo success");
        } else {
            System.out.println("updateUserInfo failure");
            System.exit(-1);
        }

        // 验证用户信息是否更新成功
        IMResult<InputOutputUserInfo> resultGetUserInfo4 = UserAdmin.getUserByUserId(userInfo.getUserId());
        if (resultGetUserInfo4 != null && resultGetUserInfo4.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            if (userInfo.getUserId().equals(resultGetUserInfo4.getResult().getUserId())
                && updateUserInfo.getDisplayName().equals(resultGetUserInfo4.getResult().getDisplayName())
                && updateUserInfo.getPortrait().equals(resultGetUserInfo4.getResult().getPortrait())) {
                System.out.println("get user info success");
            } else {
                System.out.println("get user info by userId failure");
                System.exit(-1);
            }
        } else {
            System.out.println("get user info by userId failure");
            System.exit(-1);
        }

        // 获取用户的IM Token，用于客户端登录
        IMResult<OutputGetIMTokenData> resultGetToken = UserAdmin.getUserToken(userInfo.getUserId(), "client111", ProtoConstants.Platform.Platform_Android);
        if (resultGetToken != null && resultGetToken.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get token success: " + resultGetToken.getResult().getToken());
        } else {
            System.out.println("get user token failure");
            System.exit(-1);
        }

        // 封禁用户（状态码：2表示封禁）
        IMResult<Void> resultVoid =UserAdmin.updateUserBlockStatus(userInfo.getUserId(), 2);
        if (resultVoid != null && resultVoid.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("block user done");
        } else {
            System.out.println("block user failure");
            System.exit(-1);
        }

        // 检查用户封禁状态
        IMResult<OutputUserStatus> resultCheckUserStatus = UserAdmin.checkUserBlockStatus(userInfo.getUserId());
        if (resultCheckUserStatus != null && resultCheckUserStatus.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            if (resultCheckUserStatus.getResult().getStatus() == 2) {
                System.out.println("check user status success");
            } else {
                System.out.println("user status not correct");
                System.exit(-1);
            }
        } else {
            System.out.println("block user failure");
            System.exit(-1);
        }

        // 获取所有被封禁用户列表
        IMResult<OutputUserBlockStatusList> resultBlockStatusList = UserAdmin.getBlockedList();
        if (resultBlockStatusList != null && resultBlockStatusList.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            boolean success = false;
            for (InputOutputUserBlockStatus blockStatus : resultBlockStatusList.getResult().getStatusList()) {
                if (blockStatus.getUserId().equals(userInfo.getUserId()) && blockStatus.getStatus() == 2) {
                    System.out.println("get block list done");
                    success = true;
                    break;
                }
            }
            if (!success) {
                System.out.println("block user status is not expected");
                System.exit(-1);
            }
        } else {
            System.out.println("block user failure");
            System.exit(-1);
        }

        // 解封用户（状态码：0表示正常）
        resultVoid =UserAdmin.updateUserBlockStatus(userInfo.getUserId(), 0);
        if (resultVoid != null && resultVoid.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("block user done");
        } else {
            System.out.println("block user failure");
            System.exit(-1);
        }

        // 再次检查用户状态，确认已解封
        resultCheckUserStatus = UserAdmin.checkUserBlockStatus(userInfo.getUserId());
        if (resultCheckUserStatus != null && resultCheckUserStatus.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            if (resultCheckUserStatus.getResult().getStatus() == 0) {
                System.out.println("check user status success");
            } else {
                System.out.println("user status not correct");
                System.exit(-1);
            }
        } else {
            System.out.println("block user failure");
            System.exit(-1);
        }

        // 检查用户在线状态
        IMResult<OutputCheckUserOnline> outputCheckUserOnline = UserAdmin.checkUserOnlineStatus(userInfo.getUserId());
        if (outputCheckUserOnline != null && outputCheckUserOnline.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("check user online status success:" + outputCheckUserOnline.getResult().getSessions().size());
        } else {
            System.out.println("block user online failure");
            System.exit(-1);
        }

        //测试踢下线用户客户端
        IMResult<Void> kickoffResult = UserAdmin.kickoffUserClient(userInfo.getUserId(), null);
        if (kickoffResult != null && kickoffResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("kickoff user client success");
        } else {
            System.out.println("kickoff user client failure");
            System.exit(-1);
        }

        //慎用，这个方法可能功能不完全，如果用户不在需要，建议使用block功能屏蔽用户
        // 销毁用户（物理删除，不可恢复）
        IMResult<Void> voidIMResult = UserAdmin.destroyUser("user11");
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("destroy user success");
        } else {
            System.out.println("destroy user failure");
            System.exit(-1);
        }

        // 获取所有用户列表（分页：每页100条，第0页）
        IMResult<OutputGetUserList> getUserListIMResult = UserAdmin.getAllUsers(100, 0);
        if (getUserListIMResult != null && getUserListIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("getUserListIMResult success");
        } else {
            System.out.println("getUserListIMResult failure");
            System.exit(-1);
        }

        // 获取所有机器人列表（分页：每页100条，第0页）
        IMResult<OutputGetRobotList> getRobotListIMResult = UserAdmin.getAllRobots(100, 0);
        if (getRobotListIMResult != null && getRobotListIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("getRobotListIMResult success");
        } else {
            System.out.println("getRobotListIMResult failure");
            System.exit(-1);
        }

        // 商业版专属功能
        if (commercialServer) {
            // 获取用户的未读情况
            IMResult<OutputUserUnreadCount> userUnreadCountIMResult = UserAdmin.getUserUnreadCount("user1");
            if (userUnreadCountIMResult != null && userUnreadCountIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("get user unread count success");
            } else {
                System.out.println("get user unread count failure");
                System.exit(-1);
            }

            // 获取在线用户总数
            IMResult<GetOnlineUserCountResult> getOnlineUserCountResultIMResult = UserAdmin.getOnlineUserCount();
            if (getOnlineUserCountResultIMResult != null && getOnlineUserCountResultIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("get user online count success");
            } else {
                System.out.println("get user online count failure");
                System.exit(-1);
            }

            // 获取在线用户列表（分页：第1页，从0开始，每页100条）
            IMResult<GetOnlineUserResult> getOnlineUserResultIMResult = UserAdmin.getOnlineUser(1, 0, 100);
            if (getOnlineUserResultIMResult != null && getOnlineUserResultIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("get user online success");
            } else {
                System.out.println("get user online failure");
                System.exit(-1);
            }

            // 获取指定用户的会话信息
            IMResult<GetUserSessionResult> getUserSessionResultIMResult = UserAdmin.getUserSession("hygqmws2k");
            if (getUserSessionResultIMResult != null && getUserSessionResultIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("get user session success");
            } else {
                System.out.println("get user session failure");
                System.exit(-1);
            }
        }
    }

    //***********************************************
    //****  用户关系相关的API
    //***********************************************
    /**
     * 用户关系管理API测试
     * <p>
     * 测试以下功能：
     * <ul>
     * <li>发送好友请求</li>
     * <li>设置好友关系</li>
     * <li>获取好友列表</li>
     * <li>解除好友关系</li>
     * <li>黑名单管理</li>
     * <li>好友备注（别名）管理</li>
     * <li>好友额外信息管理</li>
     * <li>获取好友关系详情</li>
     * </ul>
     * </p>
     * @throws Exception 当测试过程中发生错误时抛出异常
     */
    static void testUserRelation() throws Exception {

        //先创建2个用户用于测试好友关系
        InputOutputUserInfo userInfo = new InputOutputUserInfo();
        userInfo.setUserId("ff1");
        userInfo.setName("ff1");
        userInfo.setMobile("13800000000");
        userInfo.setDisplayName("ff1");

        // 调用SDK创建第一个用户
        IMResult<OutputCreateUser> resultCreateUser = UserAdmin.createUser(userInfo);
        if (resultCreateUser != null && resultCreateUser.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Create user " + resultCreateUser.getResult().getName() + " success");
        } else {
            System.out.println("Create user failure");
            System.exit(-1);
        }

        // 创建第二个用户
        userInfo = new InputOutputUserInfo();
        userInfo.setUserId("ff2");
        userInfo.setName("ff2");
        userInfo.setMobile("13800000001");
        userInfo.setDisplayName("ff2");

        // 调用SDK创建第二个用户
        resultCreateUser = UserAdmin.createUser(userInfo);
        if (resultCreateUser != null && resultCreateUser.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Create user " + resultCreateUser.getResult().getName() + " success");
        } else {
            System.out.println("Create user failure");
            System.exit(-1);
        }

        // 发送好友请求：ff1向ff2发送好友请求，附带问候语"hello"，直接设为好友
        IMResult<Void> result = RelationAdmin.sendFriendRequest("ff1", "ff2", "hello", true);
        if (result != null && (result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS || result.getErrorCode() == ErrorCode.ERROR_CODE_ALREADY_FRIENDS)) {
            System.out.println("send friend request success");
        } else {
            System.out.println("failure");
            System.exit(-1);
        }

        // 设置好友关系：ff1把ff2设为好友，并附带额外信息
        IMResult<Void> updateFriendStatusResult = RelationAdmin.setUserFriend("ff1", "ff2", true, "{\"from\":1}");
        if (updateFriendStatusResult != null && updateFriendStatusResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("update friend status success");
        } else {
            System.out.println("update friend status failure");
            System.exit(-1);
        }

        // 获取ff1的好友列表
        IMResult<OutputStringList> resultGetFriendList = RelationAdmin.getFriendList("ff1");
        if (resultGetFriendList != null && resultGetFriendList.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && resultGetFriendList.getResult().getList().contains("ff2")) {
            System.out.println("get friend status success");
        } else {
            System.out.println("get friend status failure");
            System.exit(-1);
        }

        // 解除好友关系：ff1删除ff2
        updateFriendStatusResult = RelationAdmin.setUserFriend("ff1", "ff2", false, null);
        if (updateFriendStatusResult != null && updateFriendStatusResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("update friend status success");
        } else {
            System.out.println("update friend status failure");
            System.exit(-1);
        }

        // 再次获取好友列表，确认ff2已被删除
        resultGetFriendList = RelationAdmin.getFriendList("ff1");
        if (resultGetFriendList != null && resultGetFriendList.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && !resultGetFriendList.getResult().getList().contains("ff2")) {
            System.out.println("get friend status success");
        } else {
            System.out.println("get friend status failure");
            System.exit(-1);
        }


        // 将ff2加入黑名单：ff1把ff2加入黑名单
        IMResult<Void> updateBlacklistStatusResult = RelationAdmin.setUserBlacklist("ff1", "ff2", true);
        if (updateBlacklistStatusResult != null && updateBlacklistStatusResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("update blacklist status success");
        } else {
            System.out.println("update blacklist status failure");
            System.exit(-1);
        }

        // 获取黑名单列表
        resultGetFriendList = RelationAdmin.getUserBlacklist("ff1");
        if (resultGetFriendList != null && resultGetFriendList.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && resultGetFriendList.getResult().getList().contains("ff2")) {
            System.out.println("get blacklist status success");
        } else {
            System.out.println("get blacklist status failure");
            System.exit(-1);
        }

        // 设置好友备注（别名）
        String alias = "hello" + System.currentTimeMillis();
        IMResult<Void> updateFriendAlias = RelationAdmin.updateFriendAlias("ff1", "ff2", alias);
        if (updateFriendAlias != null && updateFriendAlias.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("update friend alias success");
        } else {
            System.out.println("update friend alias failure");
            System.exit(-1);
        }

        // 获取好友备注（别名）
        IMResult<OutputGetAlias> getFriendAlias = RelationAdmin.getFriendAlias("ff1", "ff2");
        if (getFriendAlias != null && getFriendAlias.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && getFriendAlias.getResult().getAlias().equals(alias)) {
            System.out.println("get friend alias success");
        } else {
            System.out.println("get friend alias failure");
            System.exit(-1);
        }

        // 设置好友额外信息（可以存储自定义数据）
        String friendExtra = "hello friend extra";
        IMResult<Void> setExtraResult = RelationAdmin.updateFriendExtra("ff1", "ff2", friendExtra);
        if (setExtraResult != null && setExtraResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("set friend extra success");
        } else {
            System.out.println("set friend extra failure");
            System.exit(-1);
        }

        // 获取好友关系详情（包括额外信息等）
        IMResult<RelationPojo> getRelation = RelationAdmin.getRelation("ff1", "ff2");
        if (getRelation != null && getRelation.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get friend relation success");
        } else {
            System.out.println("get friend relation failure");
            System.exit(-1);
        }

        // 验证好友额外信息是否正确
        if(!friendExtra.equals(getRelation.getResult().extra)) {
            System.out.println("set friend extra failure");
            System.exit(-1);
        }

        //测试获取用户的机器人列表
        IMResult<OutputStringList> userRobotsResult = UserAdmin.getUserRobots("ff1");
        if (userRobotsResult != null && userRobotsResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get user robots success");
        } else {
            System.out.println("get user robots failure");
            System.exit(-1);
        }
    }
    //***********************************************
    //****  群组相关功能
    //***********************************************
    /**
     * 群组管理API测试
     * <p>
     * 测试以下功能：
     * <ul>
     * <li>创建群组</li>
     * <li>获取群组信息</li>
     * <li>转移群主</li>
     * <li>修改群组信息（名称、扩展字段）</li>
     * <li>获取群成员列表</li>
     * <li>添加群成员</li>
     * <li>踢出群成员</li>
     * <li>设置群成员别名和扩展信息</li>
     * <li>设置/取消群管理员（仅商业版）</li>
     * <li>退出群组</li>
     * <li>获取用户的群组列表</li>
     * <li>获取共同群组</li>
     * <li>群成员禁言/白名单管理（仅商业版）</li>
     * </ul>
     * </p>
     * @throws Exception 当测试过程中发生错误时抛出异常
     */
    static void testGroup() throws Exception {

        // 先解散可能存在的测试群组
        IMResult<Void> voidIMResult1 = GroupAdmin.dismissGroup("user1", "groupId1", null, null);

        PojoGroupInfo groupInfo = new PojoGroupInfo();
        groupInfo.setTarget_id("groupId1");
        groupInfo.setOwner("user1");
        groupInfo.setName("test_group");
        groupInfo.setExtra("hello extra");
        groupInfo.setType(2);
        groupInfo.setPortrait("http://portrait");
        List<PojoGroupMember> members = new ArrayList<>();
        PojoGroupMember member1 = new PojoGroupMember();
        member1.setMember_id(groupInfo.getOwner());
        members.add(member1);

        PojoGroupMember member2 = new PojoGroupMember();
        member2.setMember_id("user2");
        members.add(member2);

        PojoGroupMember member3 = new PojoGroupMember();
        member3.setMember_id("user3");
        members.add(member3);

        IMResult<OutputCreateGroupResult> resultCreateGroup = GroupAdmin.createGroup(groupInfo.getOwner(), groupInfo, members, null, null, null);
        if (resultCreateGroup != null && resultCreateGroup.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("create group success");
        } else {
            System.out.println("create group failure");
            System.exit(-1);
        }

        IMResult<PojoGroupInfo> resultGetGroupInfo = GroupAdmin.getGroupInfo(groupInfo.getTarget_id());
        if (resultGetGroupInfo != null && resultGetGroupInfo.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            if (groupInfo.getExtra().equals(resultGetGroupInfo.getResult().getExtra())
                && groupInfo.getName().equals(resultGetGroupInfo.getResult().getName())
                && groupInfo.getOwner().equals(resultGetGroupInfo.getResult().getOwner())) {
                System.out.println("get group success");
            } else {
                System.out.println("group info is not expected");
                System.exit(-1);
            }
        } else {
            System.out.println("create group failure");
            System.exit(-1);
        }

        IMResult<PojoSearchGroupRes> searchGroupResIMResult = GroupAdmin.searchGroupByName("test_group");
        if (searchGroupResIMResult != null && searchGroupResIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("transfer success");
        } else {
            System.out.println("create group failure");
            System.exit(-1);
        }

        IMResult<Void> voidIMResult = GroupAdmin.transferGroup(groupInfo.getOwner(), groupInfo.getTarget_id(), "user2", null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("transfer success");
        } else {
            System.out.println("create group failure");
            System.exit(-1);
        }

        voidIMResult = GroupAdmin.modifyGroupInfo(groupInfo.getOwner(), groupInfo.getTarget_id(), ProtoConstants.ModifyGroupInfoType.Modify_Group_Name,"HelloWorld", null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("transfer success");
        } else {
            System.out.println("create group failure");
            System.exit(-1);
        }

        voidIMResult = GroupAdmin.modifyGroupInfo(groupInfo.getOwner(), groupInfo.getTarget_id(), ProtoConstants.ModifyGroupInfoType.Modify_Group_Extra,"HelloWorld2", null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("modify group extra success");
        } else {
            System.out.println("modify group extra failure");
            System.exit(-1);
        }

        resultGetGroupInfo = GroupAdmin.getGroupInfo(groupInfo.getTarget_id());
        if (resultGetGroupInfo != null && resultGetGroupInfo.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            if ("user2".equals(resultGetGroupInfo.getResult().getOwner())) {
                groupInfo.setOwner("user2");
            } else {
                System.out.println("group info is not expected");
                System.exit(-1);
            }
        } else {
            System.out.println("create group failure");
            System.exit(-1);
        }

        IMResult<OutputGroupMemberList> resultGetMembers = GroupAdmin.getGroupMembers(groupInfo.getTarget_id());
        if (resultGetMembers != null && resultGetMembers.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get group member success");
        } else {
            System.out.println("create group failure");
            System.exit(-1);
        }

        PojoGroupMember m = new PojoGroupMember();
        m.setMember_id("user1");
        m.setAlias("hello user1");

        voidIMResult = GroupAdmin.addGroupMembers("user1", groupInfo.getTarget_id(), Arrays.asList(m), null, null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("add group member success");
        } else {
            System.out.println("add group member failure");
            System.exit(-1);
        }

        IMResult<PojoGroupMember> groupMemberIMResult = GroupAdmin.getGroupMember(groupInfo.getTarget_id(), "user1");
        if (groupMemberIMResult != null && groupMemberIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get group member success");
        } else {
            System.out.println("get group member failure");
            System.exit(-1);
        }

        voidIMResult = GroupAdmin.kickoffGroupMembers("user1", groupInfo.getTarget_id(), Arrays.asList("user3"), null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("kickoff group member success");
        } else {
            System.out.println("kickoff group member failure");
            System.exit(-1);
        }

        voidIMResult = GroupAdmin.setGroupMemberAlias("user1", groupInfo.getTarget_id(), "user2", "test user2", null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("set group member alias success");
        } else {
            System.out.println("set group member alias failure");
            System.exit(-1);
        }

        voidIMResult = GroupAdmin.setGroupMemberExtra(groupInfo.getOwner(), groupInfo.getTarget_id(), groupInfo.getOwner(), "hello member extra2", null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("set group member extra success");
        } else {
            System.out.println("set group member extra failure");
            System.exit(-1);
        }

        if(commercialServer) {
            PojoGroupMember m4 = new PojoGroupMember();
            m4.setMember_id("user4");
            m4.setAlias("hello user4");

            PojoGroupMember m5 = new PojoGroupMember();
            m5.setMember_id("user5");
            m5.setAlias("hello user5");

            voidIMResult = GroupAdmin.addGroupMembers("user1", groupInfo.getTarget_id(), Arrays.asList(m4, m5), null, null, null);
            if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("add group member success");
            } else {
                System.out.println("add group member failure");
                System.exit(-1);
            }

            voidIMResult = GroupAdmin.setGroupManager("user1", groupInfo.getTarget_id(), Arrays.asList("user4", "user5"), true, null, null);
            if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("set group manager success");
            } else {
                System.out.println("set group manager failure");
                System.exit(-1);
            }

            voidIMResult = GroupAdmin.setGroupManager("user1", groupInfo.getTarget_id(), Arrays.asList("user4", "user5"), false, null, null);
            if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("cancel group manager success");
            } else {
                System.out.println("cancel group manager failure");
                System.exit(-1);
            }
        }

        voidIMResult = GroupAdmin.quitGroup("user4", groupInfo.getTarget_id(), null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("quit group success");
        } else {
            System.out.println("quit group failure");
            System.exit(-1);
        }

        IMResult<OutputGroupIds> groupIdsIMResult = GroupAdmin.getUserGroups("user1");
        if (groupIdsIMResult != null && groupIdsIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            if (groupIdsIMResult.getResult().getGroupIds().contains(groupInfo.getTarget_id())) {
                System.out.println("get user groups success");
            } else {
                System.out.println("get user groups failure");
                System.exit(-1);
            }
        } else {
            System.out.println("get user groups failure");
            System.exit(-1);
        }

        groupIdsIMResult = GroupAdmin.getUserGroupsByType("user2", Arrays.asList(ProtoConstants.GroupMemberType.GroupMemberType_Manager, ProtoConstants.GroupMemberType.GroupMemberType_Owner));
        if (groupIdsIMResult != null && groupIdsIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get user groups by type success");
        } else {
            System.out.println("get user groups by type failure");
            System.exit(-1);
        }

        groupIdsIMResult = GroupAdmin.getCommonGroups("user1", "user2");
        if (groupIdsIMResult != null && groupIdsIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get user common groups success");
        } else {
            System.out.println("get user common groups failure");
            System.exit(-1);
        }

        //测试批量获取群组信息
        IMResult<PojoGroupInfoList> batchGroupInfoResult = GroupAdmin.batchGroupInfos(Arrays.asList(groupInfo.getTarget_id()));
        if (batchGroupInfoResult != null && batchGroupInfoResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("batch get group info success");
        } else {
            System.out.println("batch get group info failure");
            System.exit(-1);
        }

        //测试群备注功能
        String groupRemark = "test group remark";
        IMResult<Void> setGroupRemarkResult = GroupAdmin.setGroupRemark("user1", groupInfo.getTarget_id(), groupRemark);
        if (setGroupRemarkResult != null && setGroupRemarkResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("set group remark success");
        } else {
            System.out.println("set group remark failure");
            System.exit(-1);
        }

        Thread.sleep(1000);
        IMResult<String> getGroupRemarkResult = GroupAdmin.getGroupRemark("user1", groupInfo.getTarget_id());
        if (getGroupRemarkResult != null && getGroupRemarkResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && groupRemark.equals(getGroupRemarkResult.getResult())) {
            System.out.println("get group remark success");
        } else {
            System.out.println("get group remark failure");
            System.exit(-1);
        }

        //测试收藏群功能
        IMResult<Void> setFavGroupResult = GroupAdmin.setFavGroup("user1", groupInfo.getTarget_id(), true);
        if (setFavGroupResult != null && setFavGroupResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("set fav group success");
        } else {
            System.out.println("set fav group failure");
            System.exit(-1);
        }

        Thread.sleep(1000);
        IMResult<Boolean> isFavGroupResult = GroupAdmin.isFavGroup("user1", groupInfo.getTarget_id());
        if (isFavGroupResult != null && isFavGroupResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && isFavGroupResult.getResult()) {
            System.out.println("is fav group success");
        } else {
            System.out.println("is fav group failure");
            System.exit(-1);
        }

        //取消收藏
        setFavGroupResult = GroupAdmin.setFavGroup("user1", groupInfo.getTarget_id(), false);
        if (setFavGroupResult != null && setFavGroupResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("cancel fav group success");
        } else {
            System.out.println("cancel fav group failure");
            System.exit(-1);
        }

        //仅专业版支持
        if (commercialServer) {
            //开启群成员禁言
            voidIMResult = GroupAdmin.muteGroupMemeber("user1", groupInfo.getTarget_id(), Arrays.asList("user5"), true, null, null);
            if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("mute group member success");
            } else {
                System.out.println("mute group member failure");
                System.exit(-1);
            }
            //关闭群成员禁言
            voidIMResult = GroupAdmin.muteGroupMemeber("user1", groupInfo.getTarget_id(), Arrays.asList("user5"), false, null, null);
            if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("unmute group member success");
            } else {
                System.out.println("unmute group member failure");
                System.exit(-1);
            }

            //开启群成员白名单，当群全局禁言时，白名单用户可以发言
            voidIMResult = GroupAdmin.allowGroupMemeber("user1", groupInfo.getTarget_id(), Arrays.asList("user5"), true, null, null);
            if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("allow group member success");
            } else {
                System.out.println("allow group member failure");
                System.exit(-1);
            }

            //关闭群成员白名单
            voidIMResult = GroupAdmin.allowGroupMemeber("user1", groupInfo.getTarget_id(), Arrays.asList("user5"), false, null, null);
            if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("unallow group member success");
            } else {
                System.out.println("unallow group member failure");
                System.exit(-1);
            }
        }

        //测试解散群组
        IMResult<Void> dismissGroupResult = GroupAdmin.dismissGroup("user2", groupInfo.getTarget_id(), null, null);
        if (dismissGroupResult != null && dismissGroupResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("dismiss group success");
        } else {
            System.out.println("dismiss group failure");
            System.exit(-1);
        }

        //验证群组已被解散
        resultGetGroupInfo = GroupAdmin.getGroupInfo(groupInfo.getTarget_id());
        if (resultGetGroupInfo != null && (resultGetGroupInfo.getErrorCode() == ErrorCode.ERROR_CODE_NOT_EXIST || resultGetGroupInfo.result.isDeleted())) {
            System.out.println("group dismissed verified");
        } else {
            System.out.println("group dismiss verify failure");
            System.exit(-1);
        }
    }

    //***********************************************
    //****  消息相关功能
    //***********************************************
    /**
     * 消息管理API测试
     * <p>
     * 测试以下功能：
     * <ul>
     * <li>发送单聊消息</li>
     * <li>获取消息详情</li>
     * <li>撤回消息</li>
     * <li>删除消息（仅商业版）</li>
     * <li>更新消息内容（仅商业版）</li>
     * <li>广播消息（仅商业版）</li>
     * <li>撤回广播消息（仅商业版）</li>
     * <li>获取会话已读时间戳（仅商业版）</li>
     * <li>获取消息投递状态（仅商业版）</li>
     * <li>清除会话（仅商业版）</li>
     * <li>群发消息</li>
     * <li>撤回群发消息</li>
     * </ul>
     * </p>
     * @throws Exception 当测试过程中发生错误时抛出异常
     */
    static void testMessage() throws Exception {
        // 创建会话对象：目标用户ff2，会话类型为私聊
        Conversation conversation = new Conversation();
        conversation.setTarget("ff2");
        conversation.setType(ProtoConstants.ConversationType.ConversationType_Private);
        // 创建文本消息内容并编码为MessagePayload
        TextMessageContent textMessageContent = new TextMessageContent("Hello world");
        MessagePayload payload = textMessageContent.encode();

        IMResult<SendMessageResult> resultSendMessage = MessageAdmin.sendMessage("ff1", conversation, payload, null);
        if (resultSendMessage != null && resultSendMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("send message success");
        } else {
            System.out.println("send message failure");
            System.exit(-1);
        }

        IMResult<OutputMessageData> outputMessageDataIMResult = MessageAdmin.getMessage(resultSendMessage.result.getMessageUid());
        if(outputMessageDataIMResult != null && outputMessageDataIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && outputMessageDataIMResult.getResult().getMessageId() == resultSendMessage.getResult().getMessageUid()) {
            System.out.println("get message success");
        } else {
            System.out.println("get message failure");
            System.exit(-1);
        }

        IMResult<String> stringIMResult = MessageAdmin.recallMessage("user1", resultSendMessage.getResult().getMessageUid());
        if (stringIMResult != null && stringIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("recall message success");
        } else {
            System.out.println("recall message failure");
            System.exit(-1);
        }

        if (commercialServer) {
            IMResult<Void> voidIMResult = MessageAdmin.deleteMessage(resultSendMessage.getResult().getMessageUid());
            if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("delete message success");
            } else {
                System.out.println("delete message failure");
                System.exit(-1);
            }

            payload.setSearchableContent("hello world2");
            resultSendMessage = MessageAdmin.sendMessage("user1", conversation, payload, null);
            if (resultSendMessage != null && resultSendMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("send message success");
            } else {
                System.out.println("send message failure");
                System.exit(-1);
            }

            payload.setSearchableContent("hello world3");
            voidIMResult = MessageAdmin.updateMessageContent("user1", resultSendMessage.getResult().getMessageUid(), payload, true);
            if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("update message success");
            } else {
                System.out.println("update message failure");
                System.exit(-1);
            }

            IMResult<BroadMessageResult> resultBroadcastMessage = MessageAdmin.broadcastMessage("user1", 0, payload);
            if (resultBroadcastMessage != null && resultBroadcastMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("broad message success, send message to " + resultBroadcastMessage.getResult().getCount() + " users");
            } else {
                System.out.println("broad message failure");
                System.exit(-1);
            }

            voidIMResult = MessageAdmin.recallBroadCastMessage("user1", resultBroadcastMessage.result.getMessageUid());
            if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("Success");
            } else {
                System.out.println("failure");
            }

            //测试删除广播消息
            IMResult<Void> deleteBroadCastResult = MessageAdmin.deleteBroadCastMessage("user1", resultBroadcastMessage.result.getMessageUid());
            if (deleteBroadCastResult != null && deleteBroadCastResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("delete broadcast message success");
            } else {
                System.out.println("delete broadcast message failure");
            }

            IMResult<OutputTimestamp> timestampResult = MessageAdmin.getConversationReadTimestamp("57gqmws2k", new Conversation(0, "admin", 0));
            if(timestampResult != null && timestampResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("Get conversation read time success");
            } else {
                System.out.println("Get conversation read time failure");
            }

            timestampResult = MessageAdmin.getMessageDelivery("57gqmws2k");
            if(timestampResult != null && timestampResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("Get message delivery success");
            } else {
                System.out.println("Get message delivery failure");
            }

            conversation = new Conversation();
            conversation.setTarget("user1");
            conversation.setType(ProtoConstants.ConversationType.ConversationType_Private);
            conversation.setLine(0);
            IMResult<Void> clearConversationResult = MessageAdmin.clearConversation("user2", conversation);
            if (clearConversationResult != null && clearConversationResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("clear conversation success");
            } else {
                System.out.println("clear conversation failure");
            }

            //测试清空用户消息
            IMResult<Void> clearUserMessagesResult = MessageAdmin.clearUserMessages("user1", conversation, 0, System.currentTimeMillis());
            if (clearUserMessagesResult != null && clearUserMessagesResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("clear user messages success");
            } else {
                System.out.println("clear user messages failure");
            }
        }

        List<String> multicastReceivers = Arrays.asList("user2", "user3", "user4");
        IMResult<MultiMessageResult> resultMulticastMessage = MessageAdmin.multicastMessage("user1", multicastReceivers, 0, payload);
        if (resultMulticastMessage != null && resultMulticastMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("multi message success, messageid is " + resultMulticastMessage.getResult().getMessageUid());
        } else {
            System.out.println("multi message failure");
            System.exit(-1);
        }

        IMResult<Void> voidIMResult = MessageAdmin.recallMultiCastMessage("user1", resultMulticastMessage.result.getMessageUid(), multicastReceivers);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Success");
        } else {
            System.out.println("failure");
        }

        if (commercialServer) {
            //测试删除多播消息
            IMResult<Void> deleteMultiCastResult = MessageAdmin.deleteMultiCastMessage("user1", resultMulticastMessage.result.getMessageUid(), multicastReceivers);
            if (deleteMultiCastResult != null && deleteMultiCastResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("delete multicast message success");
            } else {
                System.out.println("delete multicast message failure");
            }
        }
    }

    //***********************************************
    //****  发送各种消息。
    //***********************************************
    /**
     * 消息内容编码测试
     * <p>
     * 测试各种类型消息的编码和发送：
     * <ul>
     * <li>文本消息</li>
     * <li>语音消息</li>
     * <li>图片消息</li>
     * <li>视频消息</li>
     * <li>位置消息</li>
     * <li>文件消息</li>
     * <li>动态表情消息</li>
     * <li>链接消息</li>
     * <li>名片消息</li>
     * <li>提醒消息</li>
     * <li>富通知消息</li>
     * <li>流式文本消息</li>
     * </ul>
     * </p>
     * @throws Exception 当测试过程中发生错误时抛出异常
     */
    static void testMessageContent() throws Exception {
        // 设置发送者
        String sender = "userId2";
        // 创建会话对象
        Conversation conversation = new Conversation();
        conversation.setTarget("3ygqmws2k");
        conversation.setType(ProtoConstants.ConversationType.ConversationType_Private);

        //测试发送文本消息
        TextMessageContent textMessageContent = new TextMessageContent("测试文本消息");
        //消息转成Payload并发送
        MessagePayload payload = textMessageContent.encode();
        IMResult<SendMessageResult> resultSendMessage = MessageAdmin.sendMessage(sender, conversation, payload, null);
        checkSendMessageResult(resultSendMessage);

        //测试发送语音消息
        SoundMessageContent soundMessageContent = new SoundMessageContent();
        //语音文件时长，单位秒
        soundMessageContent.setDuration(7);
        //语音文件格式为amr或者mp3，需要先上传到对象存储服务。
        soundMessageContent.setRemoteMediaUrl("https://media.wfcoss.cn/firechat/voice_message_sample.amr");
        //消息转成Payload并发送
        payload = soundMessageContent.encode();
        resultSendMessage = MessageAdmin.sendMessage(sender, conversation, payload, null);
        checkSendMessageResult(resultSendMessage);

        //测试发送图片消息
        ImageMessageContent imageMessageContent = new ImageMessageContent();
        //base64edData为图片的缩略图。缩略图的生成规则是，把图片压缩到120X120大小的方框内，45%的质量压缩为JPG格式，再把二进制做base64编码得到字符串。
        String thumbnailBase64edData = "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";
        imageMessageContent.setThumbnailBytes(Base64.getDecoder().decode(thumbnailBase64edData));
        //图片地址，发送前需要先上传到对象存储服务。
        imageMessageContent.setRemoteMediaUrl("https://media.wfcoss.cn/firechat/image_message_sample.jpg");
        //消息转成Payload并发送
        payload = imageMessageContent.encode();
        resultSendMessage = MessageAdmin.sendMessage(sender, conversation, payload, null);
        checkSendMessageResult(resultSendMessage);

        //测试发送视频消息
        VideoMessageContent videoMessageContent = new VideoMessageContent();
        //base64edData为视频的首帧的缩略图。缩略图的生成规则是，把图片压缩到120X120大小的方框内，45%的质量压缩为JPG格式，再把二进制做base64编码得到字符串。
        thumbnailBase64edData = "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";
        videoMessageContent.setThumbnailBytes(Base64.getDecoder().decode(thumbnailBase64edData));
        //视频时长，单位秒
        videoMessageContent.setDuration(3);
        //视频地址，发送前需要先上传到对象存储服务。
        videoMessageContent.setRemoteMediaUrl("https://media.wfcoss.cn/firechat/video_message_sample.mov");
        //消息转成Payload并发送
        payload = videoMessageContent.encode();
        resultSendMessage = MessageAdmin.sendMessage(sender, conversation, payload, null);
        checkSendMessageResult(resultSendMessage);

        //测试发送位置消息
        LocationMessageContent locationMessageContent = new LocationMessageContent();
        //base64edData为位置图片的缩略图。缩略图的生成规则是，把图片压缩到120X120大小的方框内，45%的质量压缩为JPG格式，再把二进制做base64编码得到字符串。
        thumbnailBase64edData = "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";
        locationMessageContent.setThumbnailByte(Base64.getDecoder().decode(thumbnailBase64edData));
        locationMessageContent.setTitle("中国北京市海淀区阜成路北二街131号");
        //设置经纬度
        locationMessageContent.setLatitude(39.926537312885166);
        locationMessageContent.setLongitude(116.32154022158235);
        //消息转成Payload并发送
        payload = locationMessageContent.encode();
        resultSendMessage = MessageAdmin.sendMessage(sender, conversation, payload, null);
        checkSendMessageResult(resultSendMessage);

        //测试发送文件消息
        FileMessageContent fileMessageContent = new FileMessageContent();
        //设置文件名
        fileMessageContent.setName("野火产品简介.pptx");
        //设置文件大小
        fileMessageContent.setSize(38394);
        //设置文件链接
        fileMessageContent.setRemoteMediaUrl("https://media.wfcoss.cn/firechat/file_message_sample.pptx");
        //消息转成Payload并发送
        payload = fileMessageContent.encode();
        resultSendMessage = MessageAdmin.sendMessage(sender, conversation, payload, null);
        checkSendMessageResult(resultSendMessage);

        //测试动态表情消息
        StickerMessageContent stickerMessageContent = new StickerMessageContent();
        stickerMessageContent.setWidth(753);
        stickerMessageContent.setHeight(960);
        stickerMessageContent.setRemoteMediaUrl("https://media.wfcoss.cn/firechat/sticker_message_sample.jpg");
        //消息转成Payload并发送
        payload = stickerMessageContent.encode();
        resultSendMessage = MessageAdmin.sendMessage(sender, conversation, payload, null);
        checkSendMessageResult(resultSendMessage);

        //测试链接消息
        LinkMessageContent linkMessageContent = new LinkMessageContent();
        linkMessageContent.setTitle("野火IM开发手册");
        linkMessageContent.setUrl("https://docs.wildfirechat.cn");
        linkMessageContent.setThumbnailUrl("https://docs.wildfirechat.cn/favicon.ico");
        linkMessageContent.setContentDigest("野火IM开发手册，关于野火的所有知识都在这里！");
        //消息转成Payload并发送
        payload = linkMessageContent.encode();
        resultSendMessage = MessageAdmin.sendMessage(sender, conversation, payload, null);
        checkSendMessageResult(resultSendMessage);

        //测试名片消息
        CardMessageContent cardMessageContent = new CardMessageContent();
        //类型：0，用户；1，群组；2，聊天室；3，频道
        cardMessageContent.setType(0);
        cardMessageContent.setTarget("FireRobot");
        cardMessageContent.setName("FireRobot");
        cardMessageContent.setPortrait("https://cdn2.wildfirechat.net/robot.png");
        cardMessageContent.setDisplayName("小火");
        cardMessageContent.setFrom(sender);
        //消息转成Payload并发送
        payload = cardMessageContent.encode();
        resultSendMessage = MessageAdmin.sendMessage(sender, conversation, payload, null);
        checkSendMessageResult(resultSendMessage);

        //测试提醒消息
        TipNotificationMessageContent tipNotificationMessageContent = new TipNotificationMessageContent();
        tipNotificationMessageContent.setTip("这是一个提醒小灰条消息");
        //消息转成Payload并发送
        payload = tipNotificationMessageContent.encode();
        resultSendMessage = MessageAdmin.sendMessage(sender, conversation, payload, null);
        checkSendMessageResult(resultSendMessage);

        //测试富通知消息
        RichNotificationMessageContent richNotificationMessageContent = new RichNotificationMessageContent("产品审核通知", "您好，您的SSL证书以审核通过并成功办理，请关注", "https://www.wildfirechat.cn")
            .remark("谢谢惠顾")
            .exName("证书小助手")
            .appId("1234567890")
            .addItem("登陆账户", "野火IM", "#173177")
            .addItem("产品名称", "域名wildifrechat.cn申请的免费SSL证书", "#173177")
            .addItem("审核通过", "通过", "#173177")
            .addItem("说明", "请登陆账户查看处理", "#173177");
        //消息转成Payload并发送
        payload = richNotificationMessageContent.encode();
        resultSendMessage = MessageAdmin.sendMessage(sender, conversation, payload, null);
        checkSendMessageResult(resultSendMessage);

        //测试流式文本消息
        testStreamingText(sender, conversation);
    }

    //消息相关表分为2类，分表是:t_messages_x和t_user_messages_y，并且是分表存储的。表内存储数据和分表规则请参考 https://docs.wildfirechat.cn/faq/server.html 问题2
    /**
     * 消息分表计算测试
     * <p>
     * 演示如何计算消息存储的分表名称：
     * <ul>
     * <li>用户消息表：t_user_messages_x (x为用户ID哈希值%128)</li>
     * <li>消息表：t_messages_y (y为年份*12+月份)</li>
     * </ul>
     * </p>
     */
    static void testMessageSharding() {
        String userId = "user1";
        // 计算用户消息表：使用用户ID的哈希值对128取模
        int hashId = Math.abs(userId.hashCode())%128;
        String userMessageTable = "t_user_messages_" + hashId;
        System.out.println("user:" + userId + " user messages table is " + userMessageTable);

        // 计算消息表：使用年份和月份
        Calendar calendar = Calendar.getInstance();
        Date date = new Date();
        calendar.setTime(date);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        year %= 3;
        String messageTable = "t_messages_" + (year * 12 + month);
        System.out.println("This month save message to table " + messageTable);
    }

    /**
     * 从数据库读取消息内容测试
     * <p>
     * 演示如何从数据库的二进制数据解析消息内容：
     * <ul>
     * <li>从数据库读取_messageContent字段</li>
     * <li>解析为协议栈MessageContent对象</li>
     * <li>使用MessageContentFactory解码为具体消息类型</li>
     * <li>处理不同类型的消息（文本、图片、语音等）</li>
     * </ul>
     * </p>
     * @throws Exception 当解析过程中发生错误时抛出异常
     */
    static void testReadMessageContentFromDB() throws Exception {
        //从数据库t_messages_x表中读取到消息内容字段_data的二进制数据为
        byte[] data = {8,1,18,5,72,101,108,108,111,64,3};
        //1. 先把二进制数据转化为协议栈消息内容。
        WFCMessage.MessageContent protoContent = WFCMessage.MessageContent.parseFrom(data);
        //2. 调用MessageContentFactory接口解析为消息内容。
        MessageContent messageContent = MessageContentFactory.decodeMessageContent(protoContent);
        //3. 检查是哪种消息，如果没有定义，会回落到UnknownMessageContent。自定义消息看本函数最后的注释。
        if(messageContent instanceof TextMessageContent) {
            TextMessageContent txt = (TextMessageContent)messageContent;
            System.out.println("读取到的是文本消息，内容为：" + txt.getText());
        } else if(messageContent instanceof ImageMessageContent) {
            ImageMessageContent img = (ImageMessageContent) messageContent;
            System.out.println("读取到的是图片消息，图片链接为：" + img.getRemoteMediaUrl());
        } else if(messageContent instanceof SoundMessageContent) {
            SoundMessageContent sound = (SoundMessageContent) messageContent;
            System.out.println("读取到的是声音消息，声音链接为：" + sound.getRemoteMediaUrl());
        }

        byte[] data2 = {8,3,18,8,91,-27,-101,-66,-25,-119,-121,93,42,-106,33,-1,-40,-1,-32,0,16,74,70,73,70,0,1,1,0,0,72,0,72,0,0,-1,-31,0,-128,69,120,105,102,0,0,77,77,0,42,0,0,0,8,0,5,1,18,0,3,0,0,0,1,0,1,0,0,1,26,0,5,0,0,0,1,0,0,0,74,1,27,0,5,0,0,0,1,0,0,0,82,1,40,0,3,0,0,0,1,0,2,0,0,-121,105,0,4,0,0,0,1,0,0,0,90,0,0,0,0,0,0,0,72,0,0,0,1,0,0,0,72,0,0,0,1,0,2,-96,2,0,4,0,0,0,1,0,0,0,120,-96,3,0,4,0,0,0,1,0,0,0,90,0,0,0,0,-1,-19,0,56,80,104,111,116,111,115,104,111,112,32,51,46,48,0,56,66,73,77,4,4,0,0,0,0,0,0,56,66,73,77,4,37,0,0,0,0,0,16,-44,29,-116,-39,-113,0,-78,4,-23,-128,9,-104,-20,-8,66,126,-1,-64,0,17,8,0,90,0,120,3,1,34,0,2,17,1,3,17,1,-1,-60,0,31,0,0,1,5,1,1,1,1,1,1,0,0,0,0,0,0,0,0,1,2,3,4,5,6,7,8,9,10,11,-1,-60,0,-75,16,0,2,1,3,3,2,4,3,5,5,4,4,0,0,1,125,1,2,3,0,4,17,5,18,33,49,65,6,19,81,97,7,34,113,20,50,-127,-111,-95,8,35,66,-79,-63,21,82,-47,-16,36,51,98,114,-126,9,10,22,23,24,25,26,37,38,39,40,41,42,52,53,54,55,56,57,58,67,68,69,70,71,72,73,74,83,84,85,86,87,88,89,90,99,100,101,102,103,104,105,106,115,116,117,118,119,120,121,122,-125,-124,-123,-122,-121,-120,-119,-118,-110,-109,-108,-107,-106,-105,-104,-103,-102,-94,-93,-92,-91,-90,-89,-88,-87,-86,-78,-77,-76,-75,-74,-73,-72,-71,-70,-62,-61,-60,-59,-58,-57,-56,-55,-54,-46,-45,-44,-43,-42,-41,-40,-39,-38,-31,-30,-29,-28,-27,-26,-25,-24,-23,-22,-15,-14,-13,-12,-11,-10,-9,-8,-7,-6,-1,-60,0,31,1,0,3,1,1,1,1,1,1,1,1,1,0,0,0,0,0,0,1,2,3,4,5,6,7,8,9,10,11,-1,-60,0,-75,17,0,2,1,2,4,4,3,4,7,5,4,4,0,1,2,119,0,1,2,3,17,4,5,33,49,6,18,65,81,7,97,113,19,34,50,-127,8,20,66,-111,-95,-79,-63,9,35,51,82,-16,21,98,114,-47,10,22,36,52,-31,37,-15,23,24,25,26,38,39,40,41,42,53,54,55,56,57,58,67,68,69,70,71,72,73,74,83,84,85,86,87,88,89,90,99,100,101,102,103,104,105,106,115,116,117,118,119,120,121,122,-126,-125,-124,-123,-122,-121,-120,-119,-118,-110,-109,-108,-107,-106,-105,-104,-103,-102,-94,-93,-92,-91,-90,-89,-88,-87,-86,-78,-77,-76,-75,-74,-73,-72,-71,-70,-62,-61,-60,-59,-58,-57,-56,-55,-54,-46,-45,-44,-43,-42,-41,-40,-39,-38,-30,-29,-28,-27,-26,-25,-24,-23,-22,-14,-13,-12,-11,-10,-9,-8,-7,-6,-1,-37,0,67,0,7,7,7,7,7,7,12,7,7,12,17,12,12,12,17,23,17,17,17,17,23,30,23,23,23,23,23,30,36,30,30,30,30,30,30,36,36,36,36,36,36,36,36,43,43,43,43,43,43,50,50,50,50,50,56,56,56,56,56,56,56,56,56,56,-1,-37,0,67,1,9,9,9,14,13,14,25,13,13,25,59,40,33,40,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,59,-1,-35,0,4,0,8,-1,-38,0,12,3,1,0,2,17,3,17,0,63,0,-10,-105,36,113,72,-120,-60,-28,-43,67,116,17,-128,110,51,84,39,-15,5,-67,-76,-51,4,-118,119,41,80,49,-126,14,-29,-113,-49,-38,-67,7,-95,-58,107,94,95,-38,105,-42,-19,61,-53,-31,87,0,-128,50,121,-10,21,-25,45,-29,24,83,82,-70,-108,40,72,-99,48,27,119,-33,-37,-128,15,-79,-58,113,92,55,-118,-4,69,117,37,-52,-78,70,75,64,73,-17,-110,51,-39,-79,-45,25,3,-81,21,-64,92,11,-15,8,126,112,79,126,120,-4,51,92,115,-100,-37,-9,122,29,52,-24,74,105,-72,-90,-19,-65,-111,-35,-66,-69,61,-68,-53,58,42,-74,27,40,115,-100,1,-33,7,-127,-57,-91,83,58,-91,-68,-118,-17,104,29,100,35,-126,15,0,-6,-12,-19,-19,92,4,-113,118,-15,-94,-88,99,-22,15,92,-109,-40,119,-85,80,-36,62,37,-106,33,-60,100,29,-93,-48,-25,56,29,-64,-17,-51,115,52,-57,99,-83,77,98,-2,11,127,32,-50,-78,4,108,-126,-92,-110,119,117,-4,61,106,57,-17,93,-26,73,-29,-111,67,63,-16,-89,0,-12,-55,36,86,108,50,92,-56,-56,-118,-83,16,94,51,-73,-8,122,-125,-109,-127,-118,-93,20,-47,-68,-98,77,-72,43,-55,63,54,122,-46,72,71,71,113,114,-56,-65,56,42,-52,119,21,-58,1,-4,77,85,121,-30,114,55,124,-121,24,76,-13,-100,-5,-42,125,-36,-26,84,38,104,-39,-101,3,110,50,112,123,-3,69,101,-61,-26,93,72,-85,24,36,-81,60,123,125,105,-88,-36,-92,-101,-47,29,11,106,119,118,-73,94,76,39,115,47,-54,-85,-100,-125,-63,-12,-21,89,-15,60,-87,116,34,-108,5,12,-92,-85,12,17,-45,-7,118,53,89,-95,-119,51,113,43,48,-105,35,29,0,30,-60,99,53,-87,110,-10,-122,53,86,-61,73,-27,-107,100,28,6,-7,-117,103,-111,-12,-4,-86,-29,22,-12,71,68,48,-107,102,-38,81,45,44,75,4,108,-77,51,0,112,-7,92,-98,61,57,-29,62,-11,12,119,112,-63,112,-86,-79,-110,-96,-113,-108,31,76,-9,-21,89,119,87,119,22,-13,-20,0,5,-112,100,41,-29,0,-3,106,-60,55,114,-35,-121,72,16,-123,3,-25,96,6,6,61,-22,26,125,76,26,107,70,109,127,104,91,121,-93,-51,82,-117,-53,15,44,101,-78,121,-28,-6,-9,-87,63,-75,108,-1,0,-65,117,-7,-113,-2,38,-78,5,-12,-117,43,66,2,-56,-82,49,-114,-52,13,46,-47,-1,0,62,107,-2,127,26,66,63,-1,-48,-56,95,22,107,-47,76,100,-98,77,-15,18,119,70,-40,110,15,108,-29,-75,96,-99,73,-98,-19,-81,-28,33,-92,57,-7,-113,31,120,99,-74,51,-114,-43,-115,-2,-115,55,49,57,-114,83,-50,1,57,31,90,-123,-32,-77,-126,48,-9,-82,-46,-69,119,28,126,-107,-33,38,-102,62,-49,-22,-72,123,93,-46,-115,-97,94,-97,-25,-8,124,-55,-18,-60,74,-54,-81,-72,-122,31,46,14,20,-100,114,115,75,106,-126,62,-124,-111,-35,123,84,105,61,-100,80,18,-71,117,99,-64,39,63,-50,-102,99,-45,20,3,-27,21,99,-3,-46,113,-4,-21,37,8,-89,116,-48,-29,70,-115,39,120,114,-81,-101,-4,-6,-36,-110,107,56,102,64,-69,8,92,-16,65,-30,-95,58,109,-76,24,49,35,-79,-11,-49,122,122,36,24,-38,37,32,30,10,-25,-113,-64,-29,53,105,4,44,-83,22,91,110,49,77,66,45,90,-58,-79,-62,81,113,-65,-77,95,-121,-11,-9,-107,-30,104,-115,-69,70,11,68,-93,42,64,60,-29,61,50,122,85,120,-51,-91,-118,-77,8,-104,-18,60,100,-126,71,29,51,82,-91,-79,15,-78,89,126,78,78,11,-127,-102,-122,-14,-54,51,-123,15,-27,-10,1,-71,31,-104,-88,-27,-108,85,-30,-114,9,-48,-28,94,-46,-107,53,117,-67,-19,-7,108,53,-11,-105,-115,-119,94,-11,98,-33,83,-5,88,109,-31,35,10,57,36,-11,-51,103,-86,-39,-37,-87,-118,100,-13,14,70,73,-28,115,-23,-116,98,-81,75,13,-100,104,-47,-85,70,-86,-36,-116,113,-45,-90,105,-62,83,-21,35,76,36,-15,11,87,53,-90,-24,-74,-46,51,-57,-70,-44,-119,7,117,-64,-21,-8,-43,41,55,-78,-106,96,-84,-60,-29,10,14,127,17,82,91,52,83,-88,107,121,76,110,123,117,6,-97,63,-37,35,6,70,35,-53,-58,95,-41,-118,-87,67,-98,23,102,-72,-72,70,-67,46,119,-73,-106,-65,-16,73,13,-78,69,-120,-26,64,-27,120,59,-80,74,-25,-73,53,98,11,117,-74,-120,-61,110,-82,67,28,-100,100,-32,-12,-19,89,63,104,18,66,22,70,-37,-98,-125,-1,0,-78,-22,104,2,-30,-43,-116,-112,74,118,-16,91,60,-12,-10,-87,-25,-117,-39,19,46,88,-59,78,52,-44,-83,-41,77,63,95,-21,-95,-90,-17,-28,-87,38,108,-20,24,37,78,118,-9,-23,85,-65,-76,-109,-2,126,-37,-2,-7,-84,-24,110,-110,-32,52,-77,-56,16,-55,-14,-99,-96,127,42,119,-39,-20,-1,0,-25,-69,127,-33,20,70,73,-22,-1,0,-81,-60,-62,21,121,-30,-91,-93,-11,-73,-7,-97,-1,-47,-13,86,-79,-75,40,35,-73,115,-26,-89,44,-64,-11,-49,-46,-85,125,-102,-44,41,75,-103,76,-104,28,110,-32,-127,-11,-17,90,87,58,78,-105,115,23,-103,-89,-7,-16,-52,-68,50,-19,56,31,82,79,-14,-84,-56,116,-55,96,50,111,38,87,59,72,35,-8,122,-11,-21,-57,76,-41,76,-30,-87,62,68,-109,126,90,-2,7,-46,42,-12,-88,105,24,94,-2,119,-4,2,59,61,-118,69,-71,-115,-29,63,-34,31,49,63,94,-108,-65,-39,42,-101,101,46,-15,-106,28,-90,3,17,-97,66,15,79,-62,-84,105,-106,-42,8,-81,45,-35,-39,-118,80,-60,-58,0,-56,-29,-66,49,-36,-43,-58,-110,24,80,92,92,124,-15,0,72,116,31,51,30,-36,103,-16,53,-83,58,92,-16,114,106,-1,0,61,-65,83,120,60,53,106,124,-50,59,108,-81,-2,90,-94,-100,86,-10,-63,68,23,50,9,7,77,-92,108,97,-114,-107,24,123,13,61,-116,46,-122,76,-1,0,19,124,-57,-114,-39,-88,90,43,77,78,63,38,22,-39,34,100,-128,-4,16,62,-67,106,-76,122,92,-40,-112,76,1,25,-2,30,77,39,25,69,39,21,-13,-36,-47,86,-108,-26,-91,70,23,125,-9,94,-102,-105,62,-47,-89,92,98,55,80,57,-21,-45,21,19,105,-79,92,-60,69,-84,-67,121,33,-85,5,1,12,-5,65,96,50,-96,30,13,89,-77,55,48,-36,-84,123,-68,-73,110,-57,-80,-12,53,-50,-86,115,59,77,28,75,25,10,-14,-28,-81,78,-9,-46,-21,66,91,88,-34,-58,115,37,-64,84,32,-29,97,-50,72,-1,0,103,21,110,105,-19,100,119,-14,-32,50,116,-49,35,2,-75,-38,-40,-54,86,89,73,125,-71,-57,-29,89,-14,91,91,-93,-19,102,-7,67,18,-96,13,-68,-97,82,43,89,-63,-63,90,59,29,-33,83,-83,66,42,-99,36,-100,111,-42,-41,-11,-41,79,45,-118,70,-22,101,-128,45,-88,109,-96,-13,-14,-12,-4,106,-11,-117,106,18,6,102,37,83,56,33,-121,62,-72,-59,18,78,-52,60,-72,48,-101,57,-32,-16,5,53,-11,6,-56,-114,80,-24,-39,36,49,-23,-116,86,119,-77,-67,-54,-70,-93,37,41,77,-39,105,110,-97,-103,45,-59,-67,-75,-55,-40,96,5,-67,65,34,-85,65,113,119,106,-37,38,-121,100,99,-115,-67,-66,-72,21,104,-33,9,35,-61,-71,-34,-68,115,-42,-103,34,77,119,15,-109,-26,109,61,65,61,127,-3,85,-77,-9,-75,-114,-26,-14,81,-109,-10,-108,-2,47,43,107,-22,85,-69,-75,-114,85,23,69,25,85,-79,-73,-113,-61,53,71,-20,-53,-22,-1,0,-112,-85,34,-62,-11,-94,42,-78,-126,-47,-127,-104,-40,-15,-41,-11,-51,71,-10,13,75,-2,121,67,-7,10,-58,81,-109,119,72,-15,-85,-13,-44,-105,55,-78,-4,63,-32,-97,-1,-46,-13,-53,13,114,91,35,-10,75,-112,39,44,8,83,-72,-81,-25,88,-41,87,-9,-46,-53,44,-79,-110,9,-24,0,-57,-89,95,-89,52,-21,84,70,-69,73,-92,108,-128,-69,-79,-22,71,99,91,58,60,58,57,-55,-65,108,-17,-18,50,64,39,-81,0,-13,90,71,17,86,114,80,115,-75,-114,-72,87,-85,87,-106,28,-10,-79,-54,-120,-18,25,21,-101,57,-112,-112,8,25,28,-46,-108,-69,-45,-89,81,38,25,-41,35,25,-56,-28,127,47,122,-24,-18,-82,-30,-114,86,-122,-55,93,81,71,67,-45,32,-111,-59,103,-119,-29,-110,-15,-89,-68,-52,-127,8,5,71,0,-127,-98,-3,115,-8,-12,-84,-107,-31,61,31,-52,-59,94,19,-75,-2,102,91,106,-73,18,-50,110,66,-82,126,-18,20,118,62,-99,-21,98,-40,94,-36,-18,-107,-113,-106,79,56,60,26,-102,-2,-17,69,8,-115,-90,64,-48,48,35,119,-89,35,-97,-54,-85,-51,127,111,18,-119,-93,109,-57,-17,21,35,-97,-91,118,104,-92,-36,-27,115,-33,-53,-86,40,-13,58,-75,46,-105,103,-8,-108,46,26,72,-100,-61,49,8,1,-49,-103,-125,-55,-49,-75,72,-94,21,121,24,-111,113,-69,44,115,-4,38,-85,106,23,-81,-88,58,-92,41,-76,49,-63,81,-17,-45,-82,105,-15,66,-74,50,57,40,74,-80,-37,-13,99,-116,-5,15,-25,92,-13,-102,82,-45,83,25,-30,23,-75,115,-118,-26,-118,-47,62,-33,-89,-34,105,-36,61,-36,-74,-94,104,-2,93,-128,96,47,79,95,-89,-46,-79,30,-26,43,-56,-4,-94,88,73,-41,13,-22,43,-96,-74,-79,-13,-95,-35,110,-50,114,15,76,-112,-92,122,-109,-57,79,122,-82,-42,-47,90,50,-54,-48,-99,-29,-85,41,-56,97,-33,53,80,-124,90,-9,118,71,-93,44,52,-36,82,-125,-76,118,125,127,-90,99,-37,90,52,110,90,92,50,-80,-57,-45,-89,53,-46,13,-77,-62,98,-72,-37,-18,59,-2,29,64,-88,38,-110,-55,-128,84,-119,-103,-92,81,-54,-16,70,115,-40,125,42,33,29,-62,46,75,29,-67,113,-36,123,-43,-62,-47,-42,46,-24,48,-40,106,112,114,-10,111,-103,63,63,-52,-75,45,-51,-78,71,-77,-55,84,35,0,62,54,-25,-14,-88,-25,-127,-25,24,19,-124,100,-25,110,57,31,-115,82,-44,101,96,-79,-62,-49,-69,-52,112,9,35,-97,-62,-90,77,66,-35,34,-14,-68,-123,5,65,37,-13,-116,-116,-16,8,-18,77,85,74,-9,-109,-26,-74,-33,-46,-45,-11,50,-83,-116,-123,42,-114,-116,-74,-5,-65,34,101,-14,102,-37,4,-63,-69,41,101,-5,-39,53,103,-5,34,-57,-42,-21,-4,-1,0,-64,-85,58,-53,80,-118,-9,-27,-72,42,-123,72,0,-14,63,30,57,-83,93,-106,-65,-13,-14,-65,-101,-1,0,-123,77,-29,100,-1,0,-81,-55,-99,48,-115,42,-47,85,18,78,-3,-9,-3,79,-1,-45,-15,-21,23,-14,100,71,-36,-119,-126,71,29,-120,31,-105,62,-107,110,-22,-32,75,127,24,104,-64,117,114,14,-47,-63,-19,-7,-109,-102,-94,44,90,24,93,-104,-126,78,-48,-84,15,66,79,97,-34,-81,70,-41,66,8,-40,-51,-13,-79,101,14,-40,35,35,-96,-84,93,86,-109,-115,-12,99,-10,-110,80,113,79,70,71,-10,-95,21,-56,75,-114,55,2,-64,-29,-98,-71,-29,21,64,-54,-55,51,-112,114,-69,-65,-50,125,-22,-12,-13,93,67,20,65,-45,18,109,-53,62,57,-25,-116,126,85,9,-116,-35,-92,81,-64,-87,-70,71,9,-41,28,-109,-116,-12,-51,17,73,46,102,111,75,11,42,-111,109,110,-107,-19,-35,25,-116,38,-72,-112,-57,110,55,22,60,34,-114,107,-82,-71,-16,70,-89,97,100,117,27,-87,-93,-40,-120,29,-45,56,32,99,-98,-36,-6,87,79,-92,-8,50,-21,68,-65,77,64,-70,92,-128,8,-107,49,-125,-125,-35,79,63,-113,78,43,91,-59,86,23,-6,-83,-113,-40,-20,84,54,79,-52,-96,-10,-19,-109,-11,-81,14,-74,108,-99,104,66,-116,-105,47,86,118,-45,-64,56,83,-108,-22,45,86,-56,-14,81,119,-89,40,-60,112,-116,-102,-71,29,-11,-99,-36,98,63,41,11,3,-55,-50,56,-19,89,-73,122,61,-34,-97,4,-46,93,20,-114,72,24,41,-119,-114,28,-18,-18,7,112,43,30,-37,-9,99,121,60,-26,-66,-122,21,-7,-67,-24,-67,11,-114,109,82,30,-20,-30,-83,-38,-42,61,75,79,-15,89,-48,109,36,-79,-103,68,-119,-68,-108,35,7,4,-16,65,62,-107,-50,-49,126,13,-52,-123,-119,116,-105,59,123,40,-17,-113,74,-29,-26,121,-122,86,99,-50,112,63,15,74,-69,110,-78,-76,-53,110,-89,40,-1,0,120,-111,-114,-39,-3,43,10,88,120,83,-85,42,-76,-107,-100,-65,18,99,-103,-44,117,-81,75,110,-58,-68,122,-108,86,-52,85,99,-55,-24,74,-100,-126,42,97,124,-84,-54,-14,-126,-86,-5,-79,-113,110,59,-43,86,-46,-115,-86,-105,36,-107,117,-56,61,56,-1,0,10,-84,109,36,86,88,-2,-8,-19,-20,43,-90,115,-108,86,-89,125,124,86,50,-124,20,-102,-47,110,-122,94,93,-61,52,-63,8,-7,66,113,-20,77,107,-24,26,109,-107,-19,-67,-44,-6,-119,-35,-28,4,-62,2,67,124,-57,-17,113,-44,83,36,-48,-96,-108,11,-105,114,56,-28,-116,96,-29,-80,-9,-86,109,103,21,-116,-122,-26,-38,86,60,96,-125,-116,-13,88,-42,-90,-22,65,-72,-53,-18,103,11,-61,86,109,-30,-86,37,37,-42,-51,127,90,29,-50,-97,-32,109,34,72,87,81,-73,-68,47,-126,89,21,-64,-64,-10,96,122,-30,-75,63,-31,31,79,-7,-17,103,-1,0,126,-105,-4,107,-125,-99,110,46,32,99,52,-60,22,24,-38,56,-49,29,112,6,49,88,-65,-39,103,-2,123,31,-54,-72,99,-105,-30,-27,119,26,-38,122,35,-70,88,110,95,-126,-110,107,-44,-1,-44,-13,123,93,71,-20,-110,125,-110,24,-64,-114,66,62,87,64,114,6,49,-57,122,126,-96,-70,44,-54,-92,126,-23,-103,-65,-128,-31,65,-9,29,-65,-6,-43,63,-120,-29,-115,52,107,9,81,64,118,39,44,7,39,-123,-22,105,-34,50,-73,-73,-126,59,111,34,53,78,71,-35,0,127,42,-13,37,27,-54,50,78,-41,27,-111,-124,-41,70,24,-102,-40,5,126,-5,-2,-9,-32,61,-115,86,-122,-2,11,55,41,105,0,87,-49,-51,51,13,-51,-97,108,-16,63,42,-91,-109,-104,-1,0,-33,74,77,87,-115,70,112,56,27,-21,-82,75,-38,123,-78,-40,-42,-82,42,-91,75,41,61,-74,59,-117,79,28,95,91,-22,17,-67,-42,-39,-94,-115,112,-22,-68,100,17,-41,-36,-42,-107,-25,-114,-27,18,-57,115,-89,64,-47,69,-56,38,65,-61,125,49,94,88,-3,71,-46,-67,34,-11,87,-2,17,104,78,58,36,88,-3,43,-56,-60,-32,-80,-48,-100,27,-122,-6,29,-104,108,77,89,66,109,-53,101,115,-104,-42,-11,100,-44,-27,121,-27,2,89,92,12,30,-104,-64,-82,84,-87,76,-79,-83,9,6,6,71,-67,67,-128,65,4,127,14,107,-39,-93,77,66,10,49,-40,-13,-28,-36,-101,-109,-22,85,-56,108,-79,25,60,115,67,-51,32,-112,75,25,-6,122,86,-107,-96,31,107,-76,-29,-84,-117,-4,-59,117,94,55,-73,-73,-123,109,-28,-118,53,70,114,119,21,0,19,-64,-21,85,123,73,33,-58,26,115,34,19,-90,106,113,-23,73,-88,-4,-78,-58,-47,9,8,28,-74,-42,28,-28,31,-18,-1,0,-11,-22,27,79,15,-33,93,-92,119,-119,44,113,36,-54,118,-87,39,62,-64,-6,103,-82,107,-48,108,-128,-14,44,-41,-79,-119,65,30,-59,106,-124,0,8,96,3,-96,65,-113,-5,-28,87,-105,87,48,-97,44,-107,-74,53,-85,-118,-100,-96,-95,39,116,114,-42,-102,6,-84,44,-38,-3,-14,-78,-82,-19,-80,115,-69,104,56,63,-113,-73,122,-24,-20,-76,27,75,-115,37,-115,-14,126,-7,73,12,-92,109,101,-29,-127,-17,93,58,-13,51,19,-49,-56,63,-12,26,117,-40,2,-22,5,28,3,-73,-118,-27,-87,-118,-99,69,-55,123,117,-45,-14,46,-98,42,80,-89,-54,-11,87,-40,-15,91,49,-88,36,98,97,19,-51,110,9,80,14,72,61,122,96,-25,-114,-68,85,-49,-75,63,-4,-8,55,-27,47,-1,0,21,94,-77,-128,-77,58,-81,0,71,-48,125,106,58,-19,88,-35,90,-27,58,104,-54,73,114,-33,99,-1,-39,48,1,58,90,104,116,116,112,58,47,47,49,57,50,46,49,54,56,46,49,46,56,49,58,56,48,47,102,115,47,49,47,50,48,50,53,47,48,50,47,49,56,47,48,55,47,51,55,47,52,48,47,77,51,108,110,99,87,49,51,99,122,74,114,45,49,45,49,55,51,57,56,51,53,52,54,48,45,116,66,98,71,82,110,49,90,119,56,111,79,46,106,112,103,64,3};
        protoContent = WFCMessage.MessageContent.parseFrom(data2);
        messageContent = MessageContentFactory.decodeMessageContent(protoContent);
        if(messageContent instanceof TextMessageContent) {
            TextMessageContent txt = (TextMessageContent)messageContent;
            System.out.println("读取到的是文本消息，内容为：" + txt.getText());
        } else if(messageContent instanceof ImageMessageContent) {
            ImageMessageContent img = (ImageMessageContent) messageContent;
            System.out.println("读取到的是图片消息，图片链接为：" + img.getRemoteMediaUrl());
        } else if(messageContent instanceof SoundMessageContent) {
            SoundMessageContent sound = (SoundMessageContent) messageContent;
            System.out.println("读取到的是声音消息，声音链接为：" + sound.getRemoteMediaUrl());
        }

        // 问题：MessageContentFactory是如何找到消息的？
        // 答：MessageContentFactory在启动时会扫描自己的包cn.wildfirechat.sdk.messagecontent下的所有MessageContent子类，记住子类和消息类型的对应关系。
        // 问题：自定义消息如何添加到MessageContentFactory中？
        // 答：首先自定义消息需要继承MessageContent，然后有2个选择，1是放到此SDK的cn.wildfirechat.sdk.messagecontent包下打包SDK；2是调用MessageContentFactory中的registerCustomMessageContent方法手动关联。
        // 比如 MessageContentFactory.registerCustomMessageContent(CustomTextMessageContent.class);
    }

    /**
     * 检查消息发送结果
     * <p>
     * 验证消息是否发送成功，如果失败则退出程序。
     * </p>
     * @param resultSendMessage 消息发送结果
     */
    static void checkSendMessageResult(IMResult<SendMessageResult> resultSendMessage) {
        if (resultSendMessage != null && resultSendMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("send message success");
        } else {
            System.out.println("send message failure");
            System.exit(-1);
        }
    }

    /**
     * 流式文本消息测试
     * <p>
     * 演示如何发送流式文本消息，模拟AI逐字生成效果：
     * <ul>
     * <li>将长文本分段发送</li>
     * <li>使用StreamTextGeneratingMessageContent表示正在生成</li>
     * <li>使用StreamTextGeneratedMessageContent表示生成完成</li>
     * </ul>
     * </p>
     * @param sender 发送者用户ID
     * @param conversation 会话对象
     * @throws Exception 当发送过程中发生错误时抛出异常
     */
    static void testStreamingText(String sender, Conversation conversation) throws Exception {
        // 准备要发送的长文本
        String fullText = "北京野火无限网络科技有限公司是成立于2019年底的一家科技创新企业，公司的主要目标是为广大企业和单位提供优质可控、私有部署的即时通讯和实时音视频能力，为社会信息化水平提高作出自己的贡献。\n" +
            "\n" +
            "野火IM是公司研发一套自主可控的即时通讯组件，具有全部私有化、功能齐全、协议稳定可靠、全平台支持、安全性高和支持国产化等技术特点。客户端分层设计，既可开箱即用，也可与现有系统深度融合。具有完善的服务端API和自定义消息功能，可以任意扩展功能。代码开源率高，方便二次开发和使用。支持多人实时音视频和会议功能，线上沟通更通畅。\n" +
            "\n" +
            "公司致力于开源项目，在Github上开源项目广受好评，其中Server项目有超过7.1K个Star，组织合计Star超过1万个。有大量的技术公司受益于我们的开源，为自己的产品添加了即时通讯能力，这也算是我们公司为社会信息化建设做出的一点点贡献吧。\n" +
            "\n" +
            "公司以即时通讯技术为核心，持续努力优化和完善即时通讯和实时音视频产品，努力为客户提供最优质的即时通讯和实时音视频能力。";
        int i = 0;
        String streamId = UUID.randomUUID().toString();
        while (i < fullText.length()) {
            i+= 15;

            boolean finish = i >= fullText.length();
            String partText = finish?fullText:fullText.substring(0, i);

            MessageContent messageContent;
            if(finish) {
                messageContent = new StreamTextGeneratedMessageContent(partText, streamId);
            } else {
                messageContent = new StreamTextGeneratingMessageContent(partText, streamId);
            }
            //消息转成Payload并发送
            MessagePayload payload = messageContent.encode();

            IMResult<SendMessageResult> resultSendMessage = MessageAdmin.sendMessage(sender, conversation, payload, null);
            if (resultSendMessage != null && resultSendMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("send message success");
            } else {
                System.out.println("send message failure");
                System.exit(-1);
            }

            Thread.sleep(500);
        }
    }


    //***********************************************
    //****  测试频道功能
    //***********************************************
    /**
     * 频道管理API测试
     * <p>
     * 测试以下功能：
     * <ul>
     * <li>创建频道</li>
     * <li>获取频道信息</li>
     * <li>关注频道</li>
     * <li>取消关注频道</li>
     * <li>检查用户关注状态</li>
     * <li>销毁频道（标记为已删除状态）</li>
     * </ul>
     * </p>
     * @throws Exception 当测试过程中发生错误时抛出异常
     */
    static void testChannelApi() throws Exception {
        String channelName = "MyChannel";
        String channelOwner = "user1";
        InputCreateChannel inputCreateChannel = new InputCreateChannel();
        inputCreateChannel.setName(channelName);
        inputCreateChannel.setOwner(channelOwner);
        IMResult<OutputCreateChannel> resultCreateChannel = ChannelAdmin.createChannel(inputCreateChannel);
        if (resultCreateChannel != null && resultCreateChannel.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("success");
            inputCreateChannel.setTargetId(resultCreateChannel.result.getTargetId());
        } else {
            System.out.println("create channel failure");
            System.exit(-1);
        }

        IMResult<OutputGetChannelInfo> resultGetChannel = ChannelAdmin.getChannelInfo(inputCreateChannel.getTargetId());
        if(resultGetChannel != null && resultGetChannel.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS
            && resultGetChannel.getResult().getName().equals(channelName)
            && resultGetChannel.getResult().getOwner().equals(channelOwner)) {
            System.out.println("success");
        } else {
            System.out.println("get channel failure");
            System.exit(-1);
        }

        String subscriber = "aaa";
        IMResult<Void> voidIMResult = ChannelAdmin.subscribeChannel(inputCreateChannel.getTargetId(), subscriber);
        if(voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("subscribeChannel success");
        } else {
            System.out.println("subscriber channel failure");
            System.exit(-1);
        }

        Thread.sleep(100);
        IMResult<OutputBooleanValue> booleanIMResult = ChannelAdmin.isUserSubscribedChannel(subscriber, inputCreateChannel.getTargetId());
        if(booleanIMResult != null && booleanIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && booleanIMResult.getResult().value) {
            System.out.println("subscribe status is correct");
        } else {
            System.out.println("subscribe status is incorrect");
            System.exit(-1);
        }


        voidIMResult = ChannelAdmin.unsubscribeChannel(inputCreateChannel.getTargetId(), subscriber);
        if(voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("unsubscribeChannel success");
        } else {
            System.out.println("unsubscriber channel failure");
            System.exit(-1);
        }
        Thread.sleep(100);

        booleanIMResult = ChannelAdmin.isUserSubscribedChannel(subscriber, inputCreateChannel.getTargetId());
        if(booleanIMResult != null && booleanIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && !booleanIMResult.getResult().value) {
            System.out.println("subscribe status is correct");
        } else {
            System.out.println("subscribe status is incorrect");
            System.exit(-1);
        }

        IMResult<OutputChannelInfoList> channelInfoListIMResult = ChannelAdmin.getChannelInfoList(10, 0);
        if(channelInfoListIMResult != null && channelInfoListIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get channel list success");
        } else {
            System.out.println("get channel list failure");
            System.exit(-1);
        }

        voidIMResult = ChannelAdmin.batchSubscribeChannel(inputCreateChannel.getTargetId(), Arrays.asList("user1", "user2", "user3"));
        if(voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("batch subscribe channel success");
        } else {
            System.out.println("batch subscribe channel failure");
            System.exit(-1);
        }


        IMResult<PojoTotalList> totalListIMResult = ChannelAdmin.getChannelSubscribers(inputCreateChannel.getTargetId(), 10, 1);
        if(totalListIMResult != null && totalListIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get channel subscribe list success");
        } else {
            System.out.println("get channel subscribe list failure");
            System.exit(-1);
        }

        voidIMResult = ChannelAdmin.batchUnsubscribeChannel(inputCreateChannel.getTargetId(), Arrays.asList("user1", "user2", "user3"));
        if(voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("batch unsubscribe channel success");
        } else {
            System.out.println("batch unsubscribe channel failure");
            System.exit(-1);
        }


        voidIMResult = ChannelAdmin.destroyChannel(inputCreateChannel.getTargetId());
        if(voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("success");
        } else {
            System.out.println("destroy channel failure");
            System.exit(-1);
        }

        resultGetChannel = ChannelAdmin.getChannelInfo(inputCreateChannel.getTargetId());
        if(resultGetChannel != null && resultGetChannel.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && (resultGetChannel.getResult().getState() & ProtoConstants.ChannelState.Channel_State_Mask_Deleted) > 0) {
            System.out.println("success");
        } else {
            System.out.println("get channel failure");
            System.exit(-1);
        }
    }

    //***********************************************
    //****  一些其它的功能，比如创建频道，更新用户设置等
    //***********************************************
    /**
     * 通用API测试
     * <p>
     * 测试以下功能：
     * <ul>
     * <li>获取系统设置（如群组最大成员数）</li>
     * <li>修改用户设置</li>
     * <li>发送用户设置</li>
     * <li>获取用户设置</li>
     * </ul>
     * </p>
     * @throws Exception 当测试过程中发生错误时抛出异常
     */
    static void testGeneralApi() throws Exception {
        IMResult<SystemSettingPojo> resultGetSystemSetting  =  GeneralAdmin.getSystemSetting(Group_Max_Member_Count);
        if (resultGetSystemSetting != null && resultGetSystemSetting.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("success");
        } else {
            System.out.println("get system setting failure");
            System.exit(-1);
        }

        IMResult<Void> resultSetSystemSetting = GeneralAdmin.setSystemSetting(Group_Max_Member_Count, "2000", "最大群人数为2000");
        if (resultSetSystemSetting != null && resultSetSystemSetting.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("success");
        } else {
            System.out.println("get system setting failure");
            System.exit(-1);
        }

        resultGetSystemSetting  =  GeneralAdmin.getSystemSetting(Group_Max_Member_Count);
        if (resultGetSystemSetting != null && resultGetSystemSetting.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && resultGetSystemSetting.getResult().value.equals("2000")) {
            System.out.println("success");
        } else {
            System.out.println("get system setting failure");
            System.exit(-1);
        }

        IMResult<HealthCheckResult> health = GeneralAdmin.healthCheck();
        if(health != null && health.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println(health.result);
        } else {
            System.out.println("health check failure");
            System.exit(-1);
        }


        //测试用户设置功能
        IMResult<Void> setUserSettingResult = GeneralAdmin.setUserSetting("user1", 3003, "test_key", "test_value");
        if (setUserSettingResult != null && setUserSettingResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("set user setting success");
        } else {
            System.out.println("set user setting failure");
            System.exit(-1);
        }

        IMResult<UserSettingPojo> getUserSettingResult = GeneralAdmin.getUserSetting("user1", 3003, "test_key");
        if (getUserSettingResult != null && getUserSettingResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && "test_value".equals(getUserSettingResult.getResult().getValue())) {
            System.out.println("get user setting success");
        } else {
            System.out.println("get user setting failure");
            System.exit(-1);
        }

//        IMResult<String> uploadUrlIMResult = GeneralAdmin.uploadFile(new File("/Users/rain/Workspace/server_commercial/sdk/pom.xml"));
//        if (uploadUrlIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
//            System.out.println("update moments blocklist success");
//        } else {
//            System.out.println("update moments blocklist failure:" + uploadUrlIMResult.getErrorCode().code);
//        }

        if (commercialServer) {
            //测试会话置顶功能
            IMResult<Void> setTopResult = GeneralAdmin.setConversationTop("user1", ProtoConstants.ConversationType.ConversationType_Private, "user2", 0, true);
            if (setTopResult != null && setTopResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("set conversation top success");
            } else {
                System.out.println("set conversation top failure");
                System.exit(-1);
            }

            Thread.sleep(1000);
            IMResult<Boolean> getTopResult = GeneralAdmin.getConversationTop("user1", ProtoConstants.ConversationType.ConversationType_Private, "user2", 0);
            if (getTopResult != null && getTopResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && getTopResult.getResult()) {
                System.out.println("get conversation top success");
            } else {
                System.out.println("get conversation top failure");
                System.exit(-1);
            }

            //取消置顶
            setTopResult = GeneralAdmin.setConversationTop("user1", ProtoConstants.ConversationType.ConversationType_Private, "user2", 0, false);
            if (setTopResult != null && setTopResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("cancel conversation top success");
            } else {
                System.out.println("cancel conversation top failure");
                System.exit(-1);
            }

            //测试获取会话文件
            IMResult<FilesPojo> getConversationFilesResult = GeneralAdmin.getConversationFiles(ProtoConstants.ConversationType.ConversationType_Private, "user2", 0, "user1", 0, true, 10);
            if (getConversationFilesResult != null && getConversationFilesResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("get conversation files success");
            } else {
                System.out.println("get conversation files failure");
            }

            //测试获取用户文件
            IMResult<FilesPojo> getUserFilesResult = GeneralAdmin.getUserFiles("user1", 0, true, 10);
            if (getUserFilesResult != null && getUserFilesResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("get user files success");
            } else {
                System.out.println("get user files failure");
            }

            //测试获取单个文件信息
            if (getUserFilesResult.getResult() != null && getUserFilesResult.getResult().files != null && !getUserFilesResult.getResult().files.isEmpty()) {
                long messageId = getUserFilesResult.getResult().files.get(0).messageId;
                IMResult<FilesPojo.FilePojo> getFileResult = GeneralAdmin.getFile(messageId);
                if (getFileResult != null && getFileResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                    System.out.println("get file success");
                } else {
                    System.out.println("get file failure");
                }
            }
        }
    }

    /**
     * 聊天室管理API测试
     * <p>
     * 测试以下功能：
     * <ul>
     * <li>创建聊天室</li>
     * <li>获取聊天室信息</li>
     * <li>获取聊天室成员列表</li>
     * <li>销毁聊天室</li>
     * <li>获取用户的聊天室</li>
     * <li>设置聊天室黑名单（仅商业版）</li>
     * <li>获取聊天室黑名单（仅商业版）</li>
     * <li>发送聊天室消息</li>
     * </ul>
     * </p>
     * @throws Exception 当测试过程中发生错误时抛出异常
     */
    static void testChatroom() throws Exception {
        String chatroomId = "chatroomId1";
        String chatroomTitle = "TESTCHATROM";
        String chatroomDesc = "this is a test chatroom";
        String chatroomPortrait = "http://pic.com/test123.png";
        String chatroomExtra = "{\'managers:[\"user1\",\"user2\"]}";
        IMResult<OutputCreateChatroom> chatroomIMResult = ChatroomAdmin.createChatroom(chatroomId,chatroomTitle, chatroomDesc, chatroomPortrait,chatroomExtra,ProtoConstants.ChatroomState.Chatroom_State_Normal);
        if (chatroomIMResult != null && chatroomIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && chatroomIMResult.getResult().getChatroomId().equals(chatroomId)) {
            System.out.println("create chatroom success");
        } else {
            System.out.println("create chatroom failure");
            System.exit(-1);
        }

        IMResult<OutputGetChatroomInfo> getChatroomInfoIMResult = ChatroomAdmin.getChatroomInfo(chatroomId);
        if (getChatroomInfoIMResult != null && getChatroomInfoIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            if (!getChatroomInfoIMResult.getResult().getChatroomId().equals(chatroomId)
                || !getChatroomInfoIMResult.getResult().getTitle().equals(chatroomTitle)
                || !getChatroomInfoIMResult.getResult().getDesc().equals(chatroomDesc)
                || !getChatroomInfoIMResult.getResult().getPortrait().equals(chatroomPortrait)
                || !getChatroomInfoIMResult.getResult().getExtra().equals(chatroomExtra)
                || getChatroomInfoIMResult.getResult().getState() != ProtoConstants.ChatroomState.Chatroom_State_Normal) {
                System.out.println("chatroom info incorrect");
                System.exit(-1);
            } else {
                System.out.println("chatroom info correct");
            }
        } else {
            System.out.println("get chatroom info failure");
            System.exit(-1);
        }

        IMResult<OutputStringList> memberList = ChatroomAdmin.getChatroomMembers(chatroomId);
        if (memberList != null && memberList.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get chatroom member success");
        } else {
            System.out.println("get chatroom member failure: " + memberList.getErrorCode().msg);
        }

        IMResult<Void> voidIMResult = ChatroomAdmin.destroyChatroom(chatroomId);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("destroy chatroom done!");
        } else {
            System.out.println("destroy chatroom failure");
            System.exit(-1);
        }

        Thread.sleep(1000);
        getChatroomInfoIMResult = ChatroomAdmin.getChatroomInfo(chatroomId);
        if (getChatroomInfoIMResult != null && getChatroomInfoIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && getChatroomInfoIMResult.getResult().getState() == ProtoConstants.ChatroomState.Chatroom_State_End) {
            System.out.println("chatroom destroyed!");
        } else {
            System.out.println("chatroom not destroyed!");
            System.exit(-1);
        }

        IMResult<OutputUserChatroom>  userChatroomIMResult = ChatroomAdmin.getUserChatroom("uygqmws2k");
        if(userChatroomIMResult != null && (userChatroomIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS || userChatroomIMResult.getErrorCode() == ErrorCode.ERROR_CODE_NOT_EXIST)) {
            System.out.println("get user chatroom success");
        } else {
            System.out.println("get user chatroom failure");
            System.exit(-1);
        }

        //测试设置聊天室全局禁言
        IMResult<Void> setChatroomMuteResult = ChatroomAdmin.setChatroomMute(chatroomId, true);
        if (setChatroomMuteResult != null && setChatroomMuteResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("set chatroom mute success");
        } else {
            System.out.println("set chatroom mute failure");
            System.exit(-1);
        }

        //取消聊天室全局禁言
        setChatroomMuteResult = ChatroomAdmin.setChatroomMute(chatroomId, false);
        if (setChatroomMuteResult != null && setChatroomMuteResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("cancel chatroom mute success");
        } else {
            System.out.println("cancel chatroom mute failure");
            System.exit(-1);
        }

        //下面仅专业版支持
        if(commercialServer) {
            //设置用户聊天室黑名单。0正常；1禁言；2禁止加入。
            IMResult<Void> voidIMResult1 = ChatroomAdmin.setChatroomBlacklist("chatroom1", "oto9o9__", 1);
            if (voidIMResult1 != null && voidIMResult1.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("add chatroom black success");
            } else {
                System.out.println("add chatroom black failure");
                System.exit(-1);
            }

            //获取聊天室黑名单
            IMResult<OutputChatroomBlackInfos> blackInfos = ChatroomAdmin.getChatroomBlacklist("chatroom1");
            if (blackInfos != null && blackInfos.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && !blackInfos.getResult().infos.isEmpty()) {
                boolean success = false;
                for (OutputChatroomBlackInfos.OutputChatroomBlackInfo info : blackInfos.getResult().infos) {
                    if (info.userId.equals("oto9o9__")) {
                        success = true;
                        break;
                    }
                }
                if (success) {
                    System.out.println("add chatroom black success");
                } else {
                    System.out.println("add chatroom black failure");
                    System.exit(-1);
                }
            } else {
                System.out.println("add chatroom black failure");
                System.exit(-1);
            }

            //取消用户聊天室黑名单。0正常；1禁言；2禁止加入。
            voidIMResult1 = ChatroomAdmin.setChatroomBlacklist("chatroom1", "oto9o9__", 0);
            if (voidIMResult1 != null && voidIMResult1.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("remove chatroom black success");
            } else {
                System.out.println("remove chatroom black failure");
                System.exit(-1);
            }

            //获取聊天室黑名单
            blackInfos = ChatroomAdmin.getChatroomBlacklist("chatroom1");
            if (blackInfos != null && blackInfos.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                boolean success = true;
                for (OutputChatroomBlackInfos.OutputChatroomBlackInfo info : blackInfos.getResult().infos) {
                    if (info.userId.equals("oto9o9__")) {
                        success = false;
                        break;
                    }
                }
                if (success) {
                    System.out.println("remove chatroom black success");
                } else {
                    System.out.println("remove chatroom black failure");
                    System.exit(-1);
                }
            } else {
                System.out.println("remove chatroom black failure");
                System.exit(-1);
            }

            //设置聊天室管理员
            IMResult<Void> voidIMResult2 = ChatroomAdmin.setChatroomManager("chatroom1", "UserId1", 1);
            if (voidIMResult2 != null && voidIMResult2.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("add chatroom manager success");
            } else {
                System.out.println("add chatroom black failure");
                System.exit(-1);
            }

            //获取聊天室管理员
            IMResult<OutputStringList> managers = ChatroomAdmin.getChatroomManagerList("chatroom1");
            if (managers != null && managers.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && !managers.getResult().getList().isEmpty() && managers.getResult().getList().contains("UserId1")) {
                System.out.println("add chatroom black success");
            } else {
                System.out.println("add chatroom black failure");
                System.exit(-1);
            }

            //取消聊天室管理员
            IMResult<Void> voidIMResult3 = ChatroomAdmin.setChatroomManager("chatroom1", "UserId1", 0);
            if (voidIMResult3 != null && voidIMResult3.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("add chatroom manager success");
            } else {
                System.out.println("add chatroom black failure");
                System.exit(-1);
            }

            //获取聊天室管理员
            IMResult<OutputStringList> managers2 = ChatroomAdmin.getChatroomManagerList("chatroom1");
            if (managers2 != null && managers2.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && !managers2.getResult().getList().contains("UserId1")) {
                System.out.println("add chatroom black success");
            } else {
                System.out.println("add chatroom black failure");
                System.exit(-1);
            }
        }
    }

    /**
     * 机器人功能测试
     * <p>
     * 测试以下功能：
     * <ul>
     * <li>创建机器人</li>
     * <li>初始化RobotService（使用IM端口80）</li>
     * <li>获取机器人信息</li>
     * <li>发送消息</li>
     * <li>撤回消息（仅商业版）</li>
     * <li>更新消息（仅商业版）</li>
     * <li>获取用户信息</li>
     * <li>创建群组</li>
     * <li>发送群组消息</li>
     * <li>获取群组信息</li>
     * <li>获取群组成员</li>
     * <li>修改群组信息</li>
     * <li>添加群成员</li>
     * <li>踢出群成员</li>
     * <li>退出群组</li>
     * <li>解散群组</li>
     * <li>销毁机器人</li>
     * </ul>
     * </p>
     * @throws Exception 当测试过程中发生错误时抛出异常
     */
    static void testRobot() throws Exception {
        String robotId = "robot1";
        String robotSecret = "123456";
        //初始化服务API
        AdminConfig.initAdmin(AdminUrl, AdminSecret);
        //创建机器人
        InputCreateRobot createRobot = new InputCreateRobot();
        createRobot.setUserId(robotId);
        createRobot.setName(robotId);
        createRobot.setDisplayName("机器人");
        createRobot.setOwner("userId1");
        createRobot.setEmail("123@wildfirechat.cn");
        createRobot.setSecret(robotSecret);
        createRobot.setCallback("http://127.0.0.1:8883/robot/recvmsg");
        IMResult<OutputCreateRobot> resultCreateRobot = UserAdmin.createRobot(createRobot);
        if (resultCreateRobot != null && resultCreateRobot.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Create robot " + resultCreateRobot.getResult().getUserId() + " success");
        } else {
            System.out.println("Create robot failure");
            System.exit(-1);
        }

        //使用完需要释放
        RobotService robotService = new RobotService(IMUrl, robotId, robotSecret);

        //***********************************************
        //****  机器人API
        //***********************************************

        IMResult<OutputRobot> robotProfileIMResult = robotService.getProfile();
        if(robotProfileIMResult != null && robotProfileIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get profile success");
        } else {
            System.out.println("get profile failure");
            System.exit(-1);
        }

        String displayName = "testrobot"+System.currentTimeMillis();
        IMResult<Void> voidIMResult1 = robotService.updateProfile(Modify_DisplayName, displayName);
        if(voidIMResult1 != null && voidIMResult1.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("modify profile success");
        } else {
            System.out.println("modify profile failure");
            System.exit(-1);
        }
        robotProfileIMResult = robotService.getProfile();
        if(robotProfileIMResult != null && robotProfileIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && displayName.equals(robotProfileIMResult.getResult().getDisplayName())) {
            System.out.println("get profile success");
        } else {
            System.out.println("get profile failure");
            System.exit(-1);
        }

        String robotCallback = "http://hellow123";
        voidIMResult1 = robotService.setCallback(robotCallback);
        if(voidIMResult1 != null && voidIMResult1.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("set callback success");
        } else {
            System.out.println("set callback failure");
            System.exit(-1);
        }

        IMResult<RobotCallbackPojo> callbackPojoIMResult = robotService.getCallback();
        if(callbackPojoIMResult != null && callbackPojoIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && robotCallback.equals(callbackPojoIMResult.getResult().getUrl())) {
            System.out.println("get callback success");
        } else {
            System.out.println("get callback failure");
            System.exit(-1);
        }

        //测试删除机器人回调地址
        voidIMResult1 = robotService.deleteCallback();
        if(voidIMResult1 != null && voidIMResult1.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("delete callback success");
        } else {
            System.out.println("delete callback failure");
            System.exit(-1);
        }

        //验证回调地址已删除
        callbackPojoIMResult = robotService.getCallback();
        if(callbackPojoIMResult != null && callbackPojoIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && StringUtil.isNullOrEmpty(callbackPojoIMResult.getResult().getUrl())) {
            System.out.println("get callback success");
        } else {
            System.out.println("get callback failure");
            System.exit(-1);
        }

        //创建会话对象，设置目标用户和会话类型
        Conversation conversation = new Conversation();
        conversation.setTarget("user2");
        conversation.setType(ProtoConstants.ConversationType.ConversationType_Private);
        
        //创建消息payload，设置消息类型和内容
        MessagePayload payload = new MessagePayload();
        payload.setType(1);
        payload.setSearchableContent("hello world");

        //测试机器人发送消息
        IMResult<SendMessageResult> resultRobotSendMessage = robotService.sendMessage(robotService.getRobotId(), conversation, payload);
        if (resultRobotSendMessage != null && resultRobotSendMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("robot send message success");
        } else {
            System.out.println("robot send message failure");
            System.exit(-1);
        }

        Thread.sleep(1000);
        IMResult<OutputMessageData> outputMessageDataIMResult = robotService.getMessage(resultRobotSendMessage.result.getMessageUid());
        if (outputMessageDataIMResult != null && outputMessageDataIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("robot get message success");
        } else {
            System.out.println("robot get message failure");
            System.exit(-1);
        }

        //测试机器人回复消息
        IMResult<SendMessageResult> replyMessageResult = robotService.replyMessage(resultRobotSendMessage.getResult().getMessageUid(), payload, false);
        if (replyMessageResult != null && replyMessageResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("robot reply message success");
        } else {
            System.out.println("robot reply message failure");
        }

        if(commercialServer) {
            Thread.sleep(1000);
            IMResult<String> recallMessageResult = robotService.recallMessage(resultRobotSendMessage.result.getMessageUid());
            if (recallMessageResult != null && recallMessageResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("robot recall message success");
            } else {
                System.out.println("robot recall message failure");
                System.exit(-1);
            }

            payload.setSearchableContent("hello world, hello world");
            resultRobotSendMessage = robotService.sendMessage(robotService.getRobotId(), conversation, payload);
            if (resultRobotSendMessage != null && resultRobotSendMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("robot send message success");
            } else {
                System.out.println("robot send message failure");
                System.exit(-1);
            }

            Thread.sleep(1000);
            payload.setSearchableContent("hello world, updated message content");
            IMResult<Void> updateMessageResult = robotService.updateMessage(resultRobotSendMessage.result.getMessageUid(), payload);
            if (updateMessageResult != null && updateMessageResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("robot update message success");
            } else {
                System.out.println("robot update message failure");
                System.exit(-1);
            }

            IMResult<Void> imResult = robotService.setOnline(ProtoConstants.Platform.Platform_LINUX, true);
            if (imResult != null && imResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("robot set online success");
            } else {
                System.out.println("robot set online failure");
                System.exit(-1);
            }
        }


        //测试机器人通过用户ID获取用户信息
        IMResult<InputOutputUserInfo> resultRobotGetUserInfo = robotService.getUserInfo("userId1");
        if (resultRobotGetUserInfo != null && resultRobotGetUserInfo.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("robot get user info success");
        } else {
            System.out.println("robot get user info by userId failure");
            System.exit(-1);
        }

        //测试机器人通过手机号获取用户信息
        IMResult<InputOutputUserInfo> resultRobotGetUserInfoByMobile = robotService.getUserInfoByMobile("13900000000");
        if (resultRobotGetUserInfoByMobile != null && resultRobotGetUserInfoByMobile.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("robot get user info by mobile success");
        } else {
            System.out.println("robot get user info by mobile failure");
        }

        //测试机器人通过用户名获取用户信息
        IMResult<InputOutputUserInfo> resultRobotGetUserInfoByName = robotService.getUserInfoByName("user1");
        if (resultRobotGetUserInfoByName != null && resultRobotGetUserInfoByName.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("robot get user info by name success");
        } else {
            System.out.println("robot get user info by name failure");
        }

        //测试机器人通过邮箱获取用户信息
        IMResult<OutputUserInfoList> resultRobotGetUserInfoByEmail = robotService.getUserInfoByEmail("123@wildfirechat.cn");
        if (resultRobotGetUserInfoByEmail != null && resultRobotGetUserInfoByEmail.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("robot get user info by email success");
        } else {
            System.out.println("robot get user info by email failure");
        }

        IMResult<OutputUserInfoList> userInfoListIMResult = robotService.getBatchUsers(Arrays.asList("userId1", "userId2"));
        if (userInfoListIMResult != null && userInfoListIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("robot get batch user info by email success");
        } else {
            System.out.println("robot get batch user info by email failure");
        }

        //创建群组信息，用于测试机器人群组管理功能
        String groupId = "robot_group" + System.currentTimeMillis();
        PojoGroupInfo groupInfo = new PojoGroupInfo();
        groupInfo.setTarget_id(groupId);
        groupInfo.setName("test_group");
        groupInfo.setType(2);
        groupInfo.setExtra("hello extra");
        groupInfo.setPortrait("http://portrait");
        List<PojoGroupMember> members = new ArrayList<>();

        PojoGroupMember member1 = new PojoGroupMember();
        member1.setMember_id("user1");
        members.add(member1);

        PojoGroupMember member2 = new PojoGroupMember();
        member2.setMember_id("user2");
        members.add(member2);

        PojoGroupMember member3 = new PojoGroupMember();
        member3.setMember_id("user3");
        members.add(member3);

        //测试机器人创建群组
        IMResult<OutputCreateGroupResult> resultCreateGroup = robotService.createGroup(groupInfo, members, null, null, null);
        if (resultCreateGroup != null && resultCreateGroup.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("create group success");
        } else {
            System.out.println("create group failure");
            System.exit(-1);
        }

        IMResult<PojoGroupInfo> resultGetGroupInfo = robotService.getGroupInfo(groupInfo.getTarget_id());
        if (resultGetGroupInfo != null && resultGetGroupInfo.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            if (groupInfo.getExtra().equals(resultGetGroupInfo.getResult().getExtra())
                && groupInfo.getName().equals(resultGetGroupInfo.getResult().getName())
                && robotId.equals(resultGetGroupInfo.getResult().getOwner())) {
                System.out.println("get group success");
            } else {
                System.out.println("group info is not expected");
                System.exit(-1);
            }
        } else {
            System.out.println("create group failure");
            System.exit(-1);
        }

        IMResult<Void> voidIMResult = robotService.modifyGroupInfo(groupInfo.getTarget_id(), ProtoConstants.ModifyGroupInfoType.Modify_Group_Name,"HelloWorld", null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("modify group success");
        } else {
            System.out.println("modify group failure");
            System.exit(-1);
        }


        voidIMResult = robotService.updateConversationUserSetting(new Conversation(ProtoConstants.ConversationType.ConversationType_Group, groupInfo.getTarget_id(), 0), 0, "hello test conversation user settings");
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("update conversation user setting success");
        } else {
            System.out.println("update conversation user setting failure");
            System.exit(-1);
        }

        IMResult<OutputGroupMemberList> resultGetMembers = robotService.getGroupMembers(groupInfo.getTarget_id());
        if (resultGetMembers != null && resultGetMembers.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get group member success");
        } else {
            System.out.println("get group member failure");
            System.exit(-1);
        }

        PojoGroupMember m = new PojoGroupMember();
        m.setMember_id("user0");
        m.setAlias("hello user0");

        voidIMResult = robotService.addGroupMembers(groupInfo.getTarget_id(), Arrays.asList(m), null, null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("add group member success");
        } else {
            System.out.println("add group member failure");
            System.exit(-1);
        }

        IMResult<PojoGroupMember> groupMemberIMResult = robotService.getGroupMember(groupInfo.getTarget_id(), "user0");
        if (groupMemberIMResult != null && groupMemberIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get group member success");
        } else {
            System.out.println("get group member failure");
            System.exit(-1);
        }

        voidIMResult = robotService.kickoffGroupMembers(groupInfo.getTarget_id(), Arrays.asList("user3"), null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("kickoff group member success");
        } else {
            System.out.println("kickoff group member failure");
            System.exit(-1);
        }

        voidIMResult = robotService.setGroupMemberAlias(groupInfo.getTarget_id(), "user2", "test user2", null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("set group member alias success");
        } else {
            System.out.println("set group member alias failure");
            System.exit(-1);
        }

        //测试设置群成员extra
        voidIMResult = robotService.setGroupMemberExtra(groupInfo.getTarget_id(), "user2", "robot member extra", null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("set group member extra success");
        } else {
            System.out.println("set group member extra failure");
            System.exit(-1);
        }

        if(commercialServer) {
            PojoGroupMember m4 = new PojoGroupMember();
            m4.setMember_id("user4");
            m4.setAlias("hello user4");

            PojoGroupMember m5 = new PojoGroupMember();
            m5.setMember_id("user5");
            m5.setAlias("hello user5");

            voidIMResult = robotService.addGroupMembers(groupInfo.getTarget_id(), Arrays.asList(m4, m5), null, null, null);
            if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("add group member success");
            } else {
                System.out.println("add group member failure");
                System.exit(-1);
            }

            voidIMResult = robotService.setGroupManager(groupInfo.getTarget_id(), Arrays.asList("user4", "user5"), true, null, null);
            if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("set group manager success");
            } else {
                System.out.println("set group manager failure");
                System.exit(-1);
            }

            voidIMResult = robotService.setGroupManager(groupInfo.getTarget_id(), Arrays.asList("user4", "user5"), false, null, null);
            if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("cancel group manager success");
            } else {
                System.out.println("cancel group manager failure");
                System.exit(-1);
            }


            //测试机器人应用获取用户信息。authCode应该是从客户端SDK里获取到的，这里不具备条件，跳过测试
//            IMResult<OutputApplicationUserInfo> appGetUserInfoResult = robotService.applicationGetUserInfo("test_auth_code");
//            if (appGetUserInfoResult != null && (appGetUserInfoResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS || appGetUserInfoResult.getErrorCode() == ErrorCode.ERROR_CODE_INVALID_DATA)) {
//                System.out.println("robot application get user info success");
//            } else {
//                System.out.println("robot application get user info failure");
//            }

            //测试机器人发送会议请求。不具备测试条件，注释掉。
//            IMResult<String> sendConferenceRequestResult = robotService.sendConferenceRequest(robotId, "client1", "test_request", 1, "room1", "test_data", false);
//            if (sendConferenceRequestResult != null && (sendConferenceRequestResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS || sendConferenceRequestResult.getErrorCode() == ErrorCode.ERROR_CODE_INVALID_DATA)) {
//                System.out.println("robot send conference request success");
//            } else {
//                System.out.println("robot send conference request failure");
//            }

            OutputApplicationConfigData config = robotService.getApplicationSignature();
            System.out.println(config);
        }

        //仅专业版支持
        if (commercialServer) {
            //开启群成员禁言
            voidIMResult = robotService.muteGroupMember(groupInfo.getTarget_id(), Arrays.asList("user5"), true, null, null);
            if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("mute group member success");
            } else {
                System.out.println("mute group member failure");
                System.exit(-1);
            }
            //关闭群成员禁言
            voidIMResult = robotService.muteGroupMember(groupInfo.getTarget_id(), Arrays.asList("user5"), false, null, null);
            if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("unmute group member success");
            } else {
                System.out.println("unmute group member failure");
                System.exit(-1);
            }

            //开启群成员白名单，当群全局禁言时，白名单用户可以发言
            voidIMResult = robotService.allowGroupMember(groupInfo.getTarget_id(), Arrays.asList("user5"), true, null, null);
            if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("allow group member success");
            } else {
                System.out.println("allow group member failure");
                System.exit(-1);
            }

            //关闭群成员白名单
            voidIMResult = robotService.allowGroupMember(groupInfo.getTarget_id(), Arrays.asList("user5"), false, null, null);
            if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("unallow group member success");
            } else {
                System.out.println("unallow group member failure");
                System.exit(-1);
            }
        }
        //测试机器人转让群组给user2
        voidIMResult = robotService.transferGroup(groupInfo.getTarget_id(), "user2", null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("transfer success");
        } else {
            System.out.println("create group failure");
            System.exit(-1);
        }

        //验证群组转让成功，检查新群主是否为user2
        resultGetGroupInfo = robotService.getGroupInfo(groupInfo.getTarget_id());
        if (resultGetGroupInfo != null && resultGetGroupInfo.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            if ("user2".equals(resultGetGroupInfo.getResult().getOwner())) {
                groupInfo.setOwner("user2");
            } else {
                System.out.println("group info is not expected");
                System.exit(-1);
            }
        } else {
            System.out.println("create group failure");
            System.exit(-1);
        }

        //测试机器人退出群组
        voidIMResult = robotService.quitGroup(groupInfo.getTarget_id(), null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("quit group success");
        } else {
            System.out.println("quit group failure");
            System.exit(-1);
        }

        //仅专业版支持，且服务器端支持朋友圈
        if (robotMomentsEnabled) {
            IMResult<FeedPojo> postResult = robotService.postMomentsFeed(ProtoConstants.MomentsContentType.Moments_Content_Text_Type, "hello from robot", null, null, null, null, "hello_extra");
            if (postResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("post moments feed success");
            } else {
                System.out.println("post moments feed failure:" + postResult.getErrorCode().code);
                System.exit(-1);
            }

            IMResult<FeedsPojo> pullResult = robotService.getMomentsFeeds(0, 10, null);
            if (pullResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("pull moments feeds success");
            } else {
                System.out.println("pull moments feeds failure:" + pullResult.getErrorCode().code);
                System.exit(-1);
            }

            IMResult<FeedPojo> feedResult = robotService.getMomentsFeed(postResult.getResult().feedId);
            if (feedResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("pull moments one feed success");
            } else {
                System.out.println("pull moments one feed failure:" + feedResult.getErrorCode().code);
                System.exit(-1);
            }

            IMResult<CommentPojo> commentThumbUpResult = robotService.postMomentsComment(postResult.result.feedId, 0, ProtoConstants.MomentsCommentType.Moments_Comment_Thumbup_Type, null, null, null);
            if (commentThumbUpResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("post moments thumb up success");
            } else {
                System.out.println("post moments thumb up failure:" + commentThumbUpResult.getErrorCode().code);
                System.exit(-1);
            }

            IMResult<CommentPojo> commentTextResult = robotService.postMomentsComment(postResult.result.feedId, 0, ProtoConstants.MomentsCommentType.Moments_Comment_Text_Type, "comment hello", null, null);
            if (commentTextResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("post moments comment text success");
            } else {
                System.out.println("post moments comment text failure:" + commentTextResult.getErrorCode().code);
                System.exit(-1);
            }

            IMResult<Void> updateResult = robotService.updateMomentsFeed(postResult.getResult().feedId, ProtoConstants.MomentsContentType.Moments_Content_Text_Type, "hello from robot2", null, null, null, null, "hello_extra2");
            if (updateResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("update moments feed success");
            } else {
                System.out.println("update moments feed failure:" + updateResult.getErrorCode().code);
                System.exit(-1);
            }

            IMResult<Void> deleteCommentThumbUpResult = robotService.deleteMomentsComment(postResult.result.feedId, commentThumbUpResult.result.commentId);
            if (deleteCommentThumbUpResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("delete moments thumb up success");
            } else {
                System.out.println("delete moments thumb up failure:" + deleteCommentThumbUpResult.getErrorCode().code);
                System.exit(-1);
            }

            IMResult<Void> deleteCommentTextResult = robotService.deleteMomentsComment(postResult.result.feedId, commentTextResult.result.commentId);
            if (deleteCommentTextResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("delete moments comment text success");
            } else {
                System.out.println("delete moments comment text failure:" + deleteCommentTextResult.getErrorCode().code);
                System.exit(-1);
            }

            IMResult<Void> deleteFeedResult = robotService.deleteMomentsFeed(postResult.result.feedId);
            if (deleteFeedResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("delete moments feed success");
            } else {
                System.out.println("delete moments feed failure:" + deleteFeedResult.getErrorCode().code);
                System.exit(-1);
            }

            IMResult<MomentProfilePojo> profileResult = robotService.getUserMomentsProfile(robotId);
            if (profileResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("get user moments profile success");
            } else {
                System.out.println("get user moments profile failure:" + profileResult.getErrorCode().code);
                System.exit(-1);
            }

            //测试更新朋友圈设置
            IMResult<Void> updateBgResult = robotService.updateMomentsBackgroundUrl("https://example.com/bg.jpg");
            if (updateBgResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("update moments background success");
            } else {
                System.out.println("update moments background failure:" + updateBgResult.getErrorCode().code);
            }

            IMResult<Void> updateVisibleCountResult = robotService.updateMomentsStrangerVisibleCount(10);
            if (updateVisibleCountResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("update moments visible count success");
            } else {
                System.out.println("update moments visible count failure:" + updateVisibleCountResult.getErrorCode().code);
            }

            IMResult<Void> updateVisibleScopeResult = robotService.updateMomentsVisibleScope(0);
            if (updateVisibleScopeResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("update moments visible scope success");
            } else {
                System.out.println("update moments visible scope failure:" + updateVisibleScopeResult.getErrorCode().code);
            }

            IMResult<Void> updateBlackListResult = robotService.updateMomentsBlackList(Arrays.asList("user1"), null);
            if (updateBlackListResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("update moments blacklist success");
            } else {
                System.out.println("update moments blacklist failure:" + updateBlackListResult.getErrorCode().code);
            }

            IMResult<Void> updateBlockListResult = robotService.updateMomentsBlockList(Arrays.asList("user2"), null);
            if (updateBlockListResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("update moments blocklist success");
            } else {
                System.out.println("update moments blocklist failure:" + updateBlockListResult.getErrorCode().code);
            }
        }

//        IMResult<String> uploadUrlIMResult = robotService.uploadFile(new File("/Users/rain/Workspace/server_commercial/sdk/pom.xml"));
//        if (uploadUrlIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
//            System.out.println("update moments blocklist success");
//        } else {
//            System.out.println("update moments blocklist failure:" + uploadUrlIMResult.getErrorCode().code);
//        }

        //测试获取机器人owner好友列表
        System.out.println("测试获取机器人owner好友列表...");
        IMResult<OutputGetFriendList> friendListResult = robotService.getOwnerFriendList();
        if (friendListResult != null && friendListResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get owner friend list success, friends: " + friendListResult.getResult().getFriends());
        } else {
            System.out.println("get owner friend list failure");
        }

        //测试根据昵称搜索用户
        System.out.println("测试根据昵称搜索用户...");
        IMResult<PojoSearchUserRes> searchResult = robotService.searchUserByDisplayName("user");
        if (searchResult != null && searchResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("search user by display name success, count: " + (searchResult.getResult().getUserInfos() != null ? searchResult.getResult().getUserInfos().size() : 0));
        } else {
            System.out.println("search user by display name failure");
        }

        //测试获取指定用户的机器人列表
        System.out.println("测试获取指定用户的机器人列表...");
        IMResult<OutputGetRobotList> robotListResult = robotService.getUserRobots("userId1");
        if (robotListResult != null && robotListResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get user robots success, count: " + (robotListResult.getResult().robotInfoList != null ? robotListResult.getResult().robotInfoList.size() : 0));
        } else {
            System.out.println("get user robots failure");
        }

        //释放机器人服务资源
        robotService.close();
    }

    //***测试频道API功能，仅专业版支持***
    /**
     * 频道功能测试（仅专业版支持）
     * <p>
     * 测试以下功能：
     * <ul>
     * <li>创建频道</li>
     * <li>获取频道信息</li>
     * <li>修改频道信息</li>
     * <li>获取频道列表</li>
     * <li>关注/取消关注频道</li>
     * <li>获取频道关注者</li>
     * <li>设置频道菜单</li>
     * <li>获取频道菜单</li>
     * <li>发送频道消息</li>
     * <li>获取频道历史消息</li>
     * <li>获取用户关注的频道</li>
     * <li>删除频道（标记为已删除）</li>
     * </ul>
     * </p>
     * @throws Exception 当测试过程中发生错误时抛出异常
     */
    static void testChannel() throws Exception {
        //初始化服务API
        AdminConfig.initAdmin(AdminUrl, AdminSecret);

        //先创建3个用户
        InputOutputUserInfo userInfo = new InputOutputUserInfo();
        userInfo.setUserId("userId1");
        userInfo.setName("user1");
        userInfo.setMobile("13900000000");
        userInfo.setDisplayName("user 1");

        IMResult<OutputCreateUser> resultCreateUser = UserAdmin.createUser(userInfo);
        if (resultCreateUser != null && resultCreateUser.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Create user " + resultCreateUser.getResult().getName() + " success");
        } else {
            System.out.println("Create user failure");
            System.exit(-1);
        }

        userInfo = new InputOutputUserInfo();
        userInfo.setUserId("userId2");
        userInfo.setName("user2");
        userInfo.setMobile("13900000002");
        userInfo.setDisplayName("user 2");

        resultCreateUser = UserAdmin.createUser(userInfo);
        if (resultCreateUser != null && resultCreateUser.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Create user " + resultCreateUser.getResult().getName() + " success");
        } else {
            System.out.println("Create user failure");
            System.exit(-1);
        }

        userInfo = new InputOutputUserInfo();
        userInfo.setUserId("userId3");
        userInfo.setName("user3");
        userInfo.setMobile("13900000003");
        userInfo.setDisplayName("user 3");

        resultCreateUser = UserAdmin.createUser(userInfo);
        if (resultCreateUser != null && resultCreateUser.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Create user " + resultCreateUser.getResult().getName() + " success");
        } else {
            System.out.println("Create user failure");
            System.exit(-1);
        }

        //1. 先使用admin api创建频道
        InputCreateChannel inputCreateChannel = new InputCreateChannel();
        inputCreateChannel.setName("testChannel");
        inputCreateChannel.setOwner("userId1");
        String secret = "channelsecret";
        String channelId = "channelId123";
        inputCreateChannel.setSecret(secret);
        inputCreateChannel.setTargetId(channelId);
        inputCreateChannel.setAuto(1);
        inputCreateChannel.setCallback("http://192.168.1.81:8088/wf/channelId123");
        inputCreateChannel.setState(Channel_State_Mask_FullInfo | Channel_State_Mask_Unsubscribed_User_Access | Channel_State_Mask_Active_Subscribe | Channel_State_Mask_Message_Unsubscribed);
        IMResult<OutputCreateChannel> resultCreateChannel = ChannelAdmin.createChannel(inputCreateChannel);
        if (resultCreateChannel != null && resultCreateChannel.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("create channel success");
        } else {
            System.out.println("create channel failure");
            System.exit(-1);
        }


        //2. 初始化api，注意端口是80，不是18080 //使用完需要释放
        ChannelServiceApi channelServiceApi = new ChannelServiceApi(IMUrl, channelId, secret);


        //3. 测试channel api功能
        
        //测试用户关注频道
        IMResult<Void> resultVoid = channelServiceApi.subscribe("userId2");
        if (resultVoid != null && resultVoid.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("subscribe success");
        } else {
            System.out.println("subscribe failure");
            System.exit(-1);
        }

        //测试另一个用户关注频道
        resultVoid = channelServiceApi.subscribe("userId3");
        if (resultVoid != null && resultVoid.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("subscribe done");
        } else {
            System.out.println("subscribe failure");
            System.exit(-1);
        }

        //使用Admin API让userId4关注频道
        resultVoid = ChannelAdmin.subscribeChannel(channelId, "userId4");
        if (resultVoid != null && resultVoid.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("subscribe done");
        } else {
            System.out.println("subscribe failure");
            System.exit(-1);
        }

        //获取频道订阅者列表
        IMResult<OutputStringList> resultStringList = channelServiceApi.getSubscriberList();
        if (resultStringList != null && resultStringList.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && resultStringList.getResult().getList().contains("userId2") && resultStringList.getResult().getList().contains("userId3") && resultStringList.getResult().getList().contains("userId4")) {
            System.out.println("get subscriber done");
        } else {
            System.out.println("get subscriber failure");
            System.exit(-1);
        }

        //检查用户是否订阅了频道
        IMResult<Boolean> booleanIMResult = channelServiceApi.isSubscriber("userId2");
        if(booleanIMResult != null && booleanIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && booleanIMResult.getResult()) {
            System.out.println("is subscriber success");
        } else {
            System.out.println("is subscriber failure");
            System.exit(-1);
        }

        //测试用户取消关注频道
        resultVoid = channelServiceApi.unsubscribe("userId2");
        if (resultVoid != null && resultVoid.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("unsubscriber done");
        } else {
            System.out.println("unsubscriber failure");
            System.exit(-1);
        }

        //验证userId2已不在订阅者列表中
        resultStringList = channelServiceApi.getSubscriberList();
        if (resultStringList != null && resultStringList.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && resultStringList.getResult().getList().contains("userId3") && !resultStringList.getResult().getList().contains("userId2")) {
            System.out.println("get subscriber done");
        } else {
            System.out.println("get subscriber failure");
            System.exit(-1);
        }

        //测试频道获取用户信息
        IMResult<InputOutputUserInfo> resultGetUserInfo1 = channelServiceApi.getUserInfo("userId3");
        if (resultGetUserInfo1 != null && resultGetUserInfo1.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get user info success");
        } else {
            System.out.println("get user info failure");
            System.exit(-1);
        }

        //创建消息payload
        MessagePayload payload = new MessagePayload();
        payload.setType(1);
        payload.setSearchableContent("hello world");

        IMResult<SendMessageResult> resultSendMessage = channelServiceApi.sendMessage(0, null, payload);
        if (resultSendMessage != null && resultSendMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("send message to all the subscriber success");
        } else {
            System.out.println("send message to all the subscriber  failure");
            System.exit(-1);
        }

        if(commercialServer) {
            //测试重新发布消息
            IMResult<Void> republishResult = channelServiceApi.republishMessage(resultSendMessage.getResult().getMessageUid(), Arrays.asList("userId2", "userId3"));
            if (republishResult != null && republishResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("republish message success");
            } else {
                System.out.println("republish message failure");
                System.exit(-1);
            }
        }

        ArticleContent articleContent = new ArticleContent("article1", "https://media.wfcoss.cn/channel-assets/20220816/2dd76540daa9444dae44e942aa1c2bbc.png", "这是一个测试文章", "测试一下文章的功能", "https://mp.weixin.qq.com/s/W6tanLbALd3qqZM8r3MTgA", true);
        articleContent.addSubArticle("article2", "https://media.wfcoss.cn/channel-assets/20220816/2dd76540daa9444dae44e942aa1c2bbc.png", "这是第二个测试文章", "测试一下文章的功能", "https://mp.weixin.qq.com/s/W6tanLbALd3qqZM8r3MTgA", false);
        articleContent.addSubArticle("article3", "https://media.wfcoss.cn/channel-assets/20220816/2dd76540daa9444dae44e942aa1c2bbc.png", "这是第三个测试文章", "测试一下文章的功能", "https://mp.weixin.qq.com/s/W6tanLbALd3qqZM8r3MTgA", false);
        payload = articleContent.toPayload();

        resultSendMessage = channelServiceApi.sendMessage(0, null, payload);
        if (resultSendMessage != null && resultSendMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("send message to all the subscriber success");
        } else {
            System.out.println("send message to all the subscriber  failure");
            System.exit(-1);
        }

        //发送定向消息给指定用户
        payload.setSearchableContent("hello to user2");

        resultSendMessage = channelServiceApi.sendMessage(0, Arrays.asList("userId2"),payload);
        if (resultSendMessage != null && resultSendMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("send message to user2 success");
        } else {
            System.out.println("send message to user2 failure");
            System.exit(-1);
        }

        //测试修改频道描述信息
        IMResult<Void> voidIMResult = channelServiceApi.modifyChannelInfo(ProtoConstants.ModifyChannelInfoType.Modify_Channel_Desc, "this is a test channel, update at:" + new Date().toString());
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("modify channel profile success");
        } else {
            System.out.println("modify channel profile failure");
            System.exit(-1);
        }

        List<PojoChannelMenu> menus = new ArrayList<>();
        PojoChannelMenu menu1 = new PojoChannelMenu();
        menu1.menuId = UUID.randomUUID().toString();
        menu1.type = "view";
        menu1.name = "一级菜单1";
        menu1.key = "key1";
        menu1.url = "http://www.baidu.com";
        menus.add(menu1);

        PojoChannelMenu menu2 = new PojoChannelMenu();
        menu2.menuId = UUID.randomUUID().toString();
        menu2.type = "view";
        menu2.name = "一级菜单2";
        menu2.key = "key2";
        menu2.url = "http://www.sohu.com";
        menu2.subMenus = new ArrayList<>();
        menus.add(menu2);

        PojoChannelMenu menu21 = new PojoChannelMenu();
        menu21.menuId = UUID.randomUUID().toString();
        menu21.type = "click";
        menu21.name = "二级菜单21";
        menu21.key = "key21";
        menu21.url = "http://www.sohu.com";
        menu2.subMenus.add(menu21);

        String menuStr = new GsonBuilder().disableHtmlEscaping().create().toJson(menus);
        voidIMResult = channelServiceApi.modifyChannelInfo(ProtoConstants.ModifyChannelInfoType.Modify_Channel_Menu, menuStr);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("modify channel menu success");
        } else {
            System.out.println("modify channel menu failure");
            System.exit(-1);
        }

        IMResult<OutputGetChannelInfo> outputGetChannelInfoIMResult = channelServiceApi.getChannelInfo();
        if (outputGetChannelInfoIMResult != null && outputGetChannelInfoIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get channel info success");
        } else {
            System.out.println("get channel info failure");
            System.exit(-1);
        }

        //测试修改频道菜单
        List<PojoChannelMenu> testMenus = new ArrayList<>();
        PojoChannelMenu testMenu = new PojoChannelMenu();
        testMenu.menuId = UUID.randomUUID().toString();
        testMenu.type = "view";
        testMenu.name = "测试菜单";
        testMenu.key = "test_key";
        testMenu.url = "http://www.test.com";
        testMenus.add(testMenu);
        IMResult<Void> modifyMenuResult = channelServiceApi.modifyChannelMenu(testMenus);
        if (modifyMenuResult != null && modifyMenuResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("modify channel menu success");
        } else {
            System.out.println("modify channel menu failure");
        }
        OutputApplicationConfigData config = channelServiceApi.getApplicationSignature();
        System.out.println(config);

        //使用完需要释放
        channelServiceApi.close();
    }

    /**
     * 敏感词管理API测试
     * <p>
     * 测试以下功能：
     * <ul>
     * <li>添加敏感词到敏感词库</li>
     * <li>获取敏感词列表</li>
     * <li>从敏感词库移除敏感词</li>
     * <li>验证敏感词添加和移除操作</li>
     * </ul>
     * </p>
     * @throws Exception 当测试过程中发生错误时抛出异常
     */
    static void testSensitiveApi() throws Exception {
        List<String> words = Arrays.asList("a","b","c");
        IMResult<Void> addResult = SensitiveAdmin.addSensitives(words);
        if (addResult != null && addResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Add sensitive word response success");
        } else {
            System.out.println("Add sensitive word response error");
            System.exit(-1);
        }

        Thread.sleep(100);

        IMResult<InputOutputSensitiveWords> swResult = SensitiveAdmin.getSensitives();
        if (swResult != null && swResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && swResult.getResult().getWords().containsAll(words)) {
            System.out.println("Sensitive word added");
        } else {
            System.out.println("Sensitive word not added");
            System.exit(-1);
        }

        IMResult<Void> removeResult = SensitiveAdmin.removeSensitives(words);
        if (removeResult != null && removeResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Remove sensitive word response success");
        } else {
            System.out.println("Remove sensitive word response error");
            System.exit(-1);
        }

        Thread.sleep(100);
        swResult = SensitiveAdmin.getSensitives();
        if (swResult != null && swResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && !swResult.getResult().getWords().containsAll(words)) {
            System.out.println("Sensitive word removed");
        } else {
            System.out.println("Sensitive word not removed");
            System.exit(-1);
        }
    }

    /**
     * 朋友圈（动态）API测试
     * <p>
     * 测试以下功能：
     * <ul>
     * <li>发布动态（朋友圈）</li>
     * <li>发送文本动态内容</li>
     * <li>验证动态发送结果</li>
     * </ul>
     * </p>
     * @throws Exception 当测试过程中发生错误时抛出异常
     */
    static void testMomentsApi() throws Exception {
        FeedPojo feedPojo = new FeedPojo();
        feedPojo.sender = "73055b4105de4b70993b7258afbfc387";
        feedPojo.type = 0;
        feedPojo.text = "hello from admin";
        IMResult<SendMessageResult> sendResult = MomentsAdmin.postFeeds(feedPojo);
        if (sendResult != null && sendResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("send moments feed success");
        } else {
            System.out.println("send moments feed failure");
            System.exit(-1);
        }
    }

    static private MessagePayload getTestPayload(int i) {

        if(i%5 == 0) {
            TextMessageContent textMessageContent = new TextMessageContent("测试文本消息" + i);
            return textMessageContent.encode();
        } else if(i%5 == 1) {
            SoundMessageContent soundMessageContent = new SoundMessageContent();
            soundMessageContent.setDuration(7 + i%10);
            soundMessageContent.setRemoteMediaUrl("https://media.wfcoss.cn/firechat/voice_message_sample.amr");
            return soundMessageContent.encode();
        } else if(i%5 == 2) {
            ImageMessageContent imageMessageContent = new ImageMessageContent();
            String thumbnailBase64edData = "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";
            imageMessageContent.setThumbnailBytes(Base64.getDecoder().decode(thumbnailBase64edData));
            imageMessageContent.setRemoteMediaUrl("https://media.wfcoss.cn/firechat/image_message_sample.jpg");
            return imageMessageContent.encode();
        } else if(i%5 == 3) {
            VideoMessageContent videoMessageContent = new VideoMessageContent();
            String thumbnailBase64edData = "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";
            videoMessageContent.setThumbnailBytes(Base64.getDecoder().decode(thumbnailBase64edData));
            videoMessageContent.setDuration(3);
            videoMessageContent.setRemoteMediaUrl("https://media.wfcoss.cn/firechat/video_message_sample.mov");
            return videoMessageContent.encode();
        } else {
            LocationMessageContent locationMessageContent = new LocationMessageContent();
            String thumbnailBase64edData = "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";
            locationMessageContent.setThumbnailByte(Base64.getDecoder().decode(thumbnailBase64edData));
            locationMessageContent.setTitle("中国北京市海淀区阜成路北二街131号");
            locationMessageContent.setLatitude(39.926537312885166);
            locationMessageContent.setLongitude(116.32154022158235);
            return locationMessageContent.encode();
        }
    }
    static void testImportMessages() throws Exception {
        List<ImportMessagesData.ImportMessage> importMessages = new ArrayList<>();
        String sender = "95022ccbd25b404c80138cf275237daa";
        String target = "admin";
        List<String> receivers = Arrays.asList(sender, target);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        long timestamp = calendar.getTimeInMillis();
        for (int i = 0; i < 100; i++) {
            ImportMessagesData.ImportMessage importMessage = new ImportMessagesData.ImportMessage();
            if(i%2==0) {
                importMessage.sender = sender;
                importMessage.conversation = new Conversation(0, target, 0);
            } else {
                importMessage.sender = target;
                importMessage.conversation = new Conversation(0, sender, 0);
            }
            //消息所属用户
            importMessage.receivers = receivers;
            importMessage.timestamp = timestamp;
            importMessage.payload = getTestPayload(i);

            timestamp -= 300*1000;

            importMessages.add(importMessage);
        }
        IMResult<Void> imResult = MessageAdmin.importMessage(importMessages);
        if (imResult != null && imResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Import success");
        } else {
            System.out.println("Create device failure");
            System.exit(-1);
        }
    }

    //***********************************************
    //****  物联网相关的API，仅专业版支持
    //***********************************************
    /**
     * 物联网设备管理API测试（仅专业版支持）
     * <p>
     * 测试以下功能：
     * <ul>
     * <li>创建或更新设备</li>
     * <li>获取设备信息</li>
     * <li>设备绑定多个所有者</li>
     * <li>设备ID唯一性验证</li>
     * <li>销毁设备</li>
     * </ul>
     * </p>
     * @throws Exception 当测试过程中发生错误时抛出异常
     */
    static void testDevice() throws Exception {
        //创建设备信息，设置设备ID和所有者列表
        InputCreateDevice createDevice = new InputCreateDevice();
        createDevice.setDeviceId("deviceId1");
        createDevice.setOwners(Arrays.asList("opoGoG__", "userId1"));
        
        //测试创建设备（如果不存在则创建，存在则更新）
        IMResult<OutputCreateDevice> resultCreateDevice = UserAdmin.createOrUpdateDevice(createDevice);
        if (resultCreateDevice != null && resultCreateDevice.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Create device " + resultCreateDevice.getResult().getDeviceId() + " success");
        } else {
            System.out.println("Create device failure");
            System.exit(-1);
        }

        //测试获取设备信息
        IMResult<OutputDevice> getDevice = UserAdmin.getDevice("deviceId1");
        if (getDevice != null && getDevice.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && getDevice.getResult().getDeviceId().equals("deviceId1") && getDevice.getResult().getOwners().contains("opoGoG__")) {
            System.out.println("Get device " + resultCreateDevice.getResult().getDeviceId() + " success");
        } else {
            System.out.println("Get device failure");
            System.exit(-1);
        }

        //测试获取用户绑定的所有设备
        IMResult<OutputDeviceList> getUserDevices = UserAdmin.getUserDevices("userId1");
        if (getUserDevices != null && getUserDevices.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            boolean success = false;
            for (OutputDevice outputDevice : getUserDevices.getResult().getDevices()) {
                if (outputDevice.getDeviceId().equals("deviceId1")) {
                    success = true;
                    break;
                }
            }
            if (success) {
                System.out.println("Get user device success");
            } else {
                System.out.println("Get user device failure");
                System.exit(-1);
            }
        } else {
            System.out.println("Get device failure");
            System.exit(-1);
        }
    }

    /**
     * 会议功能测试（仅音视频高级版服务支持）
     * <p>
     * 测试以下会议功能：
     * <ul>
     * <li>获取会议列表</li>
     * <li>销毁会议</li>
     * <li>创建普通会议</li>
     * <li>创建高级会议</li>
     * <li>检查会议是否存在</li>
     * <li>开启会议录制</li>
     * <li>获取参会者列表</li>
     * <li>RTP转发相关操作</li>
     * </ul>
     * </p>
     * @throws Exception 当测试过程中发生错误时抛出异常
     */
    public static void testConference() throws Exception {
        //获取当前会议列表
        IMResult<PojoConferenceInfoList> listResult = ConferenceAdmin.listConferences(100, 0);
        if(listResult == null || listResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get conference list failure");
            System.exit(-1);
        } else {
            System.out.println("conference list " + listResult.getResult().conferenceInfoList);
        }

        //清理现有会议，避免影响测试
        for (PojoConferenceInfo conferenceInfo:listResult.getResult().conferenceInfoList) {
            IMResult<Void> destroyResult = ConferenceAdmin.destroy(conferenceInfo.roomId, conferenceInfo.advance);
            if(destroyResult == null || destroyResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("destroy room failure");
                System.exit(-1);
            } else {
                System.out.println("destroy room success");
            }
        }

        //再次获取会议列表，确认已清空
        listResult = ConferenceAdmin.listConferences(100, 0);
        if(listResult == null || listResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get conference list failure");
            System.exit(-1);
        } else {
            System.out.println("conference list " + listResult.getResult().conferenceInfoList);
        }

        //生成两个会议房间ID
        String roomId1 = UUID.randomUUID().toString();
        String roomId2 = UUID.randomUUID().toString();
        //创建普通会议房间（非高级版，最多9人）
        IMResult<Void> voidIMResult = ConferenceAdmin.createRoom(roomId1, "hello room description", "123456", 9, false, 0, false, true);
        if(voidIMResult == null || voidIMResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("create conference failure");
            System.exit(-1);
        } else {
            System.out.println("create conference");
        }

        //创建高级会议房间（支持更多功能，最多20人）
        voidIMResult = ConferenceAdmin.createRoom(roomId2, "hello room description advanced", "123456", 20, true, 0, false, true);
        if(voidIMResult == null || voidIMResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("create conference failure");
            System.exit(-1);
        } else {
            System.out.println("create conference");
        }

        //检查会议房间是否存在
        IMResult<Boolean> booleanIMResult = ConferenceAdmin.existsConferences(roomId1);
        if(booleanIMResult == null || booleanIMResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("exist conference failure");
            System.exit(-1);
        } else {
            System.out.println("exit conference success");
        }

        //开启会议录制功能（视频和音频都录制）
        voidIMResult = ConferenceAdmin.enableRecording(roomId2, true, true);
        if(voidIMResult == null || voidIMResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("recording conference failure");
            System.exit(-1);
        } else {
            System.out.println("recording conference success");
        }

        listResult = ConferenceAdmin.listConferences(100, 0);
        if(listResult == null || listResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get conference list failure");
            System.exit(-1);
        } else {
            System.out.println("conference list " + listResult.getResult().conferenceInfoList);
        }

        //获取每个会议的参会者列表
        for (PojoConferenceInfo conferenceInfo:listResult.getResult().conferenceInfoList) {
            IMResult<PojoConferenceParticipantList> listParticipantsResult = ConferenceAdmin.listParticipants(conferenceInfo.roomId, conferenceInfo.advance);
            if(listParticipantsResult == null || listParticipantsResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("list participants failure");
                System.exit(-1);
            } else {
                System.out.println("list participants success");
            }
        }

        //获取RTP转发器列表，并停止所有转发
        IMResult<PojoConferenceRtpForwarders> listForwarderResult = ConferenceAdmin.listRtpForwarders("6366963312");
        if(listForwarderResult == null || listForwarderResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("list rtp forward failure");
        } else {
            System.out.println("list rtp forward success");
            listForwarderResult.getResult().forwarders.forEach(rtpForwarder -> rtpForwarder.streams.forEach(rtpStream -> {
                try {
                    IMResult<Void> stopRtpForwardResult = ConferenceAdmin.stopRtpForward(listForwarderResult.getResult().roomId, rtpForwarder.publisherId, rtpStream.streamId);
                    if(stopRtpForwardResult == null || stopRtpForwardResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
                        System.out.println("rtp forward stop failure");
                    } else {
                        System.out.println("rtp forward stop success");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        }

        //测试RTP转发功能，将媒体流转发到指定地址
        IMResult<Void> rtpForwardResult = ConferenceAdmin.rtpForward("6366963312", "cygqmws2k", "192.168.1.81", 10000, 111, 0, 10005, 98, 0);
        if(rtpForwardResult == null || rtpForwardResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("rtp forward failure");
        } else {
            System.out.println("rtp forward success");
        }
    }

    //***********************************************
    //****  Mesh相关API，用于分布式IM
    //***********************************************
    /**
     * Mesh（分布式IM）API测试
     * <p>
     * 测试以下分布式IM功能：
     * <ul>
     * <li>搜索用户 - 跨域搜索用户</li>
     * <li>批量获取用户信息 - 获取多个用户的详细信息</li>
     * <li>发送好友请求 - 向其他域用户发送好友请求</li>
     * <li>处理好友请求 - 接受或拒绝好友请求</li>
     * <li>创建域 - 创建新的Mesh域</li>
     * <li>获取域信息 - 获取指定域的详细信息</li>
     * <li>获取所有域 - 获取Mesh网络中的所有域列表</li>
     * <li>Ping域 - 测试域的连通性</li>
     * <li>删除域 - 从Mesh网络中移除域</li>
     * <li>同步群组 - 跨域同步群组信息</li>
     * <li>发送消息 - 向其他域用户发送消息</li>
     * <li>发布消息 - 向多个域用户广播消息</li>
     * <li>添加加入群组请求 - 跨域申请加入群组</li>
     * <li>会议用户请求 - 跨域会议相关请求</li>
     * <li>会议用户事件 - 跨域会议事件处理</li>
     * </ul>
     * </p>
     * @throws Exception 当测试过程中发生错误时抛出异常
     */
    static void testMesh() throws Exception {
        //测试搜索用户
        IMResult<PojoSearchUserRes> searchUserResult = MeshAdmin.searchUser("test", 0, 0, 1);
        if (searchUserResult != null && searchUserResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("mesh search user success");
        } else {
            System.out.println("mesh search user failure");
        }

        //测试批量获取用户信息
        IMResult<OutputUserInfoList> batchUserInfosResult = MeshAdmin.getBatchUserInfos(Arrays.asList("user1", "user2"));
        if (batchUserInfosResult != null && batchUserInfosResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("mesh batch get user info success");
        } else {
            System.out.println("mesh batch get user info failure");
        }

        //测试发送好友请求
        IMResult<Void> sendFriendRequestResult = MeshAdmin.sendFriendRequest("user1", "user2", "hello");
        if (sendFriendRequestResult != null && (sendFriendRequestResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS || sendFriendRequestResult.getErrorCode() == ErrorCode.ERROR_CODE_ALREADY_FRIENDS)) {
            System.out.println("mesh send friend request success");
        } else {
            System.out.println("mesh send friend request failure");
        }

        //测试处理好友请求
        IMResult<Void> handleFriendRequestResult = MeshAdmin.handleFriendRequest("user2", "user1", 1);
        if (handleFriendRequestResult != null && handleFriendRequestResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("mesh handle friend request success");
        } else {
            System.out.println("mesh handle friend request failure");
        }

        //测试创建域
        String domainId = "test_domain_" + System.currentTimeMillis();
        InputOutputDomainInfo domainInfo = new InputOutputDomainInfo();
        domainInfo.setDomainId(domainId);
        domainInfo.setName("Test Domain");
        IMResult<Void> createDomainResult = MeshAdmin.createDomain(domainInfo);
        if (createDomainResult != null && createDomainResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("mesh create domain success");
        } else {
            System.out.println("mesh create domain failure");
        }

        //测试获取域
        IMResult<InputOutputDomainInfo> getDomainResult = MeshAdmin.getDomain(domainId);
        if (getDomainResult != null && getDomainResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("mesh get domain success");
        } else {
            System.out.println("mesh get domain failure");
        }

        //测试获取所有域
        IMResult<InputOutputDomainInfoList> getAllDomainResult = MeshAdmin.getAllDomain();
        if (getAllDomainResult != null && getAllDomainResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("mesh get all domain success");
        } else {
            System.out.println("mesh get all domain failure");
        }

        //测试ping域
        IMResult<PojoDomainPingResponse> pingDomainResult = MeshAdmin.pingDomain(domainId);
        if (pingDomainResult != null && pingDomainResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("mesh ping domain success");
        } else {
            System.out.println("mesh ping domain failure");
        }

        //测试删除域
        IMResult<Void> deleteDomainResult = MeshAdmin.deleteDomain(domainId);
        if (deleteDomainResult != null && deleteDomainResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("mesh delete domain success");
        } else {
            System.out.println("mesh delete domain failure");
        }

        //测试同步群组
        PojoGroupInfo meshGroupInfo = new PojoGroupInfo();
        meshGroupInfo.setTarget_id("mesh_group_" + System.currentTimeMillis());
        meshGroupInfo.setName("Mesh Test Group");
        meshGroupInfo.setOwner("user1");
        meshGroupInfo.setType(2);
        List<PojoGroupMember> meshMembers = new ArrayList<>();
        PojoGroupMember meshMember = new PojoGroupMember();
        meshMember.setMember_id("user1");
        meshMembers.add(meshMember);
        IMResult<Void> syncGroupResult = MeshAdmin.syncGroup(meshGroupInfo, meshMembers);
        if (syncGroupResult != null && syncGroupResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("mesh sync group success");
        } else {
            System.out.println("mesh sync group failure");
        }

        //测试发送消息
        Conversation meshConversation = new Conversation();
        meshConversation.setTarget("user2");
        meshConversation.setType(ProtoConstants.ConversationType.ConversationType_Private);
        MessagePayload meshPayload = new MessagePayload();
        meshPayload.setType(1);
        meshPayload.setSearchableContent("mesh test message");
        IMResult<SendMessageResult> meshSendMessageResult = MeshAdmin.sendMessage("user1", meshConversation, meshPayload, null);
        if (meshSendMessageResult != null && meshSendMessageResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("mesh send message success");
        } else {
            System.out.println("mesh send message failure");
        }

        //测试发布消息
        if (meshSendMessageResult != null && meshSendMessageResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            SendMessageData publishMessageData = new SendMessageData();
            publishMessageData.setSender("user1");
            publishMessageData.setConv(meshConversation);
            publishMessageData.setPayload(meshPayload);
            IMResult<SendMessageResult> publishMessageResult = MeshAdmin.publishMessage(publishMessageData, Arrays.asList("user2"), meshSendMessageResult.getResult().getMessageUid());
            if (publishMessageResult != null && publishMessageResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("mesh publish message success");
            } else {
                System.out.println("mesh publish message failure");
            }
        }

        //测试添加加入群组请求
        IMResult<Void> addJoinGroupRequestResult = MeshAdmin.addJoinGroupRequest("user2", meshGroupInfo.getTarget_id(), Arrays.asList("user3"), "please add me", null);
        if (addJoinGroupRequestResult != null && addJoinGroupRequestResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("mesh add join group request success");
        } else {
            System.out.println("mesh add join group request failure");
        }

        //测试会议用户请求
        IMResult<PojoUserConferenceResponse> userConferenceRequestResult = MeshAdmin.userConferenceRequest("client1", "user1", "test_request", 1, "room1", "test_data", false);
        if (userConferenceRequestResult != null && userConferenceRequestResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("mesh user conference request success");
        } else {
            System.out.println("mesh user conference request failure");
        }

        //测试会议用户事件
        IMResult<Void> userConferenceEventResult = MeshAdmin.userConferenceEvent("test_event_data", "user1", "client1", false);
        if (userConferenceEventResult != null && userConferenceEventResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("mesh user conference event success");
        } else {
            System.out.println("mesh user conference event failure");
        }

    }
}
