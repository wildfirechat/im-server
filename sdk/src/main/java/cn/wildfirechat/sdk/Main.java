package cn.wildfirechat.sdk;

import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.messagecontentbuilder.RichNotificationContentBuilder;
import cn.wildfirechat.messagecontentbuilder.TextMessageContentBuilder;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.sdk.utilities.AdminHttpUtils;
import cn.wildfirechat.sdk.utilities.RobotHttpUtils;
import com.google.gson.Gson;
import io.netty.util.internal.StringUtil;


import java.util.*;

import static cn.wildfirechat.pojos.MyInfoType.Modify_DisplayName;
import static cn.wildfirechat.proto.ProtoConstants.ChannelState.*;
import static cn.wildfirechat.proto.ProtoConstants.SystemSettingType.Group_Max_Member_Count;

public class Main {
    private static boolean commercialServer = false;
    private static boolean advanceVoip = false;
    //管理端口是8080
    private static String AdminUrl = "http://localhost:18080";
    private static String AdminSecret = "123456";

    //机器人和频道使用IM服务的公开端口80，注意不是18080
    private static String IMUrl = "http://localhost";


    public static void main(String[] args) throws Exception {
        //admin使用的是18080端口，超级管理接口，理论上不能对外开放端口，也不能让非内部服务知悉密钥。
        testAdmin();

        //Robot和Channel都是使用的80端口，第三方可以创建或者为第三方创建，第三方可以使用robot或者channel与IM系统进行对接。
        testRobot();
        testChannel();
    }


    static void testAdmin() throws Exception {
        //初始化服务API
        AdminConfig.initAdmin(AdminUrl, AdminSecret);

        testUser();
        testUserRelation();
        testGroup();
        testChatroom();
        testMessage();
        testGeneralApi();
        testSensitiveApi();
        if (commercialServer) {
            testDevice();
        }
        if(advanceVoip) {
            testConference();
        }

        System.out.println("Congratulation, all admin test case passed!!!!!!!");
    }

    //***********************************************
    //****  用户相关的API
    //***********************************************
    static void testUser() throws Exception {
        InputOutputUserInfo userInfo = new InputOutputUserInfo();
        //用户ID，必须保证唯一性
        userInfo.setUserId("userId1");
        //用户名，一般是用户登录帐号，也必须保证唯一性。也就是说所有用户的userId必须不能重复，所有用户的name必须不能重复，但可以同一个用户的userId和name是同一个，一般建议userId使用一个uuid，name是"微信号"且可以修改，
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

        InputCreateRobot createRobot = new InputCreateRobot();
        createRobot.setUserId("robot1");
        createRobot.setName("robot1");
        createRobot.setDisplayName("机器人");
        createRobot.setOwner("userId1");
        createRobot.setSecret("123456");
        createRobot.setCallback("http://127.0.0.1:8883/robot/recvmsg");
        IMResult<OutputCreateRobot> resultCreateRobot = UserAdmin.createRobot(createRobot);
        if (resultCreateRobot != null && resultCreateRobot.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Create robot " + resultCreateRobot.getResult().getUserId() + " success");
        } else {
            System.out.println("Create robot failure");
            System.exit(-1);
        }

        IMResult<OutputRobot> outputRobotIMResult = UserAdmin.getRobotInfo("robot1");
        if(outputRobotIMResult != null && outputRobotIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Get robot success");
        } else {
            System.out.println("Get robot failure");
            System.exit(-1);
        }

        IMResult<Void> destroyResult = UserAdmin.destroyRobot("robot1");
        if(destroyResult != null && destroyResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("success");
        } else {
            System.out.println("destroy user failure");
            System.exit(-1);
        }

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

        IMResult<InputOutputUserInfo> resultGetUserInfo2 = UserAdmin.getUserByMobile(userInfo.getMobile());
        if (resultGetUserInfo2 != null && resultGetUserInfo2.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            if (userInfo.getUserId().equals(resultGetUserInfo2.getResult().getUserId())
                && userInfo.getName().equals(resultGetUserInfo2.getResult().getName())
                && userInfo.getMobile().equals(resultGetUserInfo2.getResult().getMobile())
                && userInfo.getDisplayName().equals(resultGetUserInfo2.getResult().getDisplayName())) {
                System.out.println("get user info success");
            } else {
                System.out.println("get user info by mobile failure");
                System.exit(-1);
            }
        } else {
            System.out.println("get user info by mobile failure");
            System.exit(-1);
        }

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
        InputOutputUserInfo updateUserInfo = new InputOutputUserInfo();
        updateUserInfo.setUserId(System.currentTimeMillis()+"");
        updateUserInfo.setDisplayName("updatedUserName");
        updateUserInfo.setPortrait("updatedUserPortrait");
        int updateUserFlag = ProtoConstants.UpdateUserInfoMask.Update_User_DisplayName | ProtoConstants.UpdateUserInfoMask.Update_User_Portrait;
        IMResult<Void> result = UserAdmin.updateUserInfo(updateUserInfo, updateUserFlag);
        if(result != null && result.getErrorCode() == ErrorCode.ERROR_CODE_NOT_EXIST) {
            System.out.println("updateUserInfo success");
        } else {
            System.out.println("updateUserInfo failure");
            System.exit(-1);
        }

        updateUserInfo.setUserId(userInfo.getUserId());
        result = UserAdmin.updateUserInfo(updateUserInfo, updateUserFlag);
        if(result != null && result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("updateUserInfo success");
        } else {
            System.out.println("updateUserInfo failure");
            System.exit(-1);
        }

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

        IMResult<OutputGetIMTokenData> resultGetToken = UserAdmin.getUserToken(userInfo.getUserId(), "client111", ProtoConstants.Platform.Platform_Android);
        if (resultGetToken != null && resultGetToken.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get token success: " + resultGetToken.getResult().getToken());
        } else {
            System.out.println("get user token failure");
            System.exit(-1);
        }

        IMResult<Void> resultVoid =UserAdmin.updateUserBlockStatus(userInfo.getUserId(), 2);
        if (resultVoid != null && resultVoid.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("block user done");
        } else {
            System.out.println("block user failure");
            System.exit(-1);
        }

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

        resultVoid =UserAdmin.updateUserBlockStatus(userInfo.getUserId(), 0);
        if (resultVoid != null && resultVoid.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("block user done");
        } else {
            System.out.println("block user failure");
            System.exit(-1);
        }

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

        IMResult<OutputCheckUserOnline> outputCheckUserOnline = UserAdmin.checkUserOnlineStatus(userInfo.getUserId());
        if (outputCheckUserOnline != null && outputCheckUserOnline.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("check user online status success:" + outputCheckUserOnline.getResult().getSessions().size());
        } else {
            System.out.println("block user online failure");
            System.exit(-1);
        }

        //慎用，这个方法可能功能不完全，如果用户不在需要，建议使用block功能屏蔽用户
        IMResult<Void> voidIMResult = UserAdmin.destroyUser("user11");
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("destroy user success");
        } else {
            System.out.println("destroy user failure");
            System.exit(-1);
        }

        if (commercialServer) {
            IMResult<GetOnlineUserCountResult> getOnlineUserCountResultIMResult = UserAdmin.getOnlineUserCount();
            if (getOnlineUserCountResultIMResult != null && getOnlineUserCountResultIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("get user online count success");
            } else {
                System.out.println("get user online count failure");
                System.exit(-1);
            }

            IMResult<GetOnlineUserResult> getOnlineUserResultIMResult = UserAdmin.getOnlineUser(1, 0, 100);
            if (getOnlineUserResultIMResult != null && getOnlineUserResultIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("get user online success");
            } else {
                System.out.println("get user online failure");
                System.exit(-1);
            }
        }
    }

    //***********************************************
    //****  用户关系相关的API
    //***********************************************
    static void testUserRelation() throws Exception {

        //先创建2个用户
        InputOutputUserInfo userInfo = new InputOutputUserInfo();
        userInfo.setUserId("ff1");
        userInfo.setName("ff1");
        userInfo.setMobile("13800000000");
        userInfo.setDisplayName("ff1");

        IMResult<OutputCreateUser> resultCreateUser = UserAdmin.createUser(userInfo);
        if (resultCreateUser != null && resultCreateUser.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Create user " + resultCreateUser.getResult().getName() + " success");
        } else {
            System.out.println("Create user failure");
            System.exit(-1);
        }

        userInfo = new InputOutputUserInfo();
        userInfo.setUserId("ff2");
        userInfo.setName("ff2");
        userInfo.setMobile("13800000001");
        userInfo.setDisplayName("ff2");

        resultCreateUser = UserAdmin.createUser(userInfo);
        if (resultCreateUser != null && resultCreateUser.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Create user " + resultCreateUser.getResult().getName() + " success");
        } else {
            System.out.println("Create user failure");
            System.exit(-1);
        }

        IMResult<Void> result = RelationAdmin.sendFriendRequest("ff1", "ff2", "hello", true);
        if (result != null && (result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS || result.getErrorCode() == ErrorCode.ERROR_CODE_ALREADY_FRIENDS)) {
            System.out.println("send friend request success");
        } else {
            System.out.println("failure");
            System.exit(-1);
        }

        result = RelationAdmin.sendFriendRequest("ff1", "ff2", "hello2", false);
        if (result != null && result.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("success");
        } else {
            System.out.println("failure");
            System.exit(-1);
        }

        result = RelationAdmin.sendFriendRequest("ff1", "ff2", "hello3", true);
        if (result != null && (result.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS || result.getErrorCode() == ErrorCode.ERROR_CODE_ALREADY_FRIENDS)) {
            System.out.println("success");
        } else {
            System.out.println("send friend request success");
            System.exit(-1);
        }

        IMResult<Void> updateFriendStatusResult = RelationAdmin.setUserFriend("ff1", "ff2", true, "{\"from\":1}");
        if (updateFriendStatusResult != null && updateFriendStatusResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("update friend status success");
        } else {
            System.out.println("update friend status failure");
            System.exit(-1);
        }

        IMResult<OutputStringList> resultGetFriendList = RelationAdmin.getFriendList("ff1");
        if (resultGetFriendList != null && resultGetFriendList.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && resultGetFriendList.getResult().getList().contains("ff2")) {
            System.out.println("get friend status success");
        } else {
            System.out.println("get friend status failure");
            System.exit(-1);
        }

        updateFriendStatusResult = RelationAdmin.setUserFriend("ff1", "ff2", false, null);
        if (updateFriendStatusResult != null && updateFriendStatusResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("update friend status success");
        } else {
            System.out.println("update friend status failure");
            System.exit(-1);
        }

        resultGetFriendList = RelationAdmin.getFriendList("ff1");
        if (resultGetFriendList != null && resultGetFriendList.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && !resultGetFriendList.getResult().getList().contains("ff2")) {
            System.out.println("get friend status success");
        } else {
            System.out.println("get friend status failure");
            System.exit(-1);
        }


        IMResult<Void> updateBlacklistStatusResult = RelationAdmin.setUserBlacklist("ff1", "ff2", true);
        if (updateBlacklistStatusResult != null && updateBlacklistStatusResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("update blacklist status success");
        } else {
            System.out.println("update blacklist status failure");
            System.exit(-1);
        }

        resultGetFriendList = RelationAdmin.getUserBlacklist("ff1");
        if (resultGetFriendList != null && resultGetFriendList.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && resultGetFriendList.getResult().getList().contains("ff2")) {
            System.out.println("get blacklist status success");
        } else {
            System.out.println("get blacklist status failure");
            System.exit(-1);
        }

        String alias = "hello" + System.currentTimeMillis();
        IMResult<Void> updateFriendAlias = RelationAdmin.updateFriendAlias("ff1", "ff2", alias);
        if (updateFriendAlias != null && updateFriendAlias.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("update friend alias success");
        } else {
            System.out.println("update friend alias failure");
            System.exit(-1);
        }

        IMResult<OutputGetAlias> getFriendAlias = RelationAdmin.getFriendAlias("ff1", "ff2");
        if (getFriendAlias != null && getFriendAlias.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && getFriendAlias.getResult().getAlias().equals(alias)) {
            System.out.println("get friend alias success");
        } else {
            System.out.println("get friend alias failure");
            System.exit(-1);
        }

        String friendExtra = "hello friend extra";
        IMResult<Void> setExtraResult = RelationAdmin.updateFriendExtra("ff1", "ff2", friendExtra);
        if (setExtraResult != null && setExtraResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("set friend extra success");
        } else {
            System.out.println("set friend extra failure");
            System.exit(-1);
        }

        IMResult<RelationPojo> getRelation = RelationAdmin.getRelation("ff1", "ff2");
        if (getRelation != null && getRelation.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get friend relation success");
        } else {
            System.out.println("get friend relation failure");
            System.exit(-1);
        }

        if(!friendExtra.equals(getRelation.getResult().extra)) {
            System.out.println("set friend extra failure");
            System.exit(-1);
        }
    }
    //***********************************************
    //****  群组相关功能
    //***********************************************
    static void testGroup() throws Exception {

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

        IMResult<OutputCreateGroupResult> resultCreateGroup = GroupAdmin.createGroup(groupInfo.getOwner(), groupInfo, members, null, null);
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

        voidIMResult = GroupAdmin.addGroupMembers("user1", groupInfo.getTarget_id(), Arrays.asList(m), null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("add group member success");
        } else {
            System.out.println("add group member failure");
            System.exit(-1);
        }

        voidIMResult = GroupAdmin.kickoffGroupMembers("user1", groupInfo.getTarget_id(), Arrays.asList("user3"), null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("kickoff group member success");
        } else {
            System.out.println("kickoff group member failure");
            System.exit(-1);
        }

        voidIMResult = GroupAdmin.setGroupMemberAlias("user1", groupInfo.getTarget_id(), "user3", "test user3", null, null);
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
    }

    //***********************************************
    //****  消息相关功能
    //***********************************************
    static void testMessage() throws Exception {
        Conversation conversation = new Conversation();
        conversation.setTarget("ff2");
        conversation.setType(ProtoConstants.ConversationType.ConversationType_Private);
        MessagePayload payload = TextMessageContentBuilder.newBuilder("Hello world").build();

        IMResult<SendMessageResult> resultSendMessage = MessageAdmin.sendMessage("ff1", conversation, payload, null);
        if (resultSendMessage != null && resultSendMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("send message success");
        } else {
            System.out.println("send message failure");
            System.exit(-1);
        }

        RichNotificationContentBuilder builder = RichNotificationContentBuilder.newBuilder("产品审核通知", "您好，您的SSL证书以审核通过并成功办理，请关注", "https://www.baidu.com")
            .remark("谢谢惠顾")
            .exName("证书小助手")
            .appId("1234567890")
            .addItem("登陆账户", "野火IM", "#173177")
            .addItem("产品名称", "域名wildifrechat.cn申请的免费SSL证书", "#173177")
            .addItem("审核通过", "通过", "#173177")
            .addItem("说明", "请登陆账户查看处理", "#173177");


        resultSendMessage = MessageAdmin.sendMessage("ff1", conversation, builder.build(), null);
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

        IMResult<Void> voidIMResult = MessageAdmin.recallMessage("user1", resultSendMessage.getResult().getMessageUid());
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("recall message success");
        } else {
            System.out.println("recall message failure");
            System.exit(-1);
        }

        if (commercialServer) {
            voidIMResult = MessageAdmin.deleteMessage(resultSendMessage.getResult().getMessageUid());
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
        }

        List<String> multicastReceivers = Arrays.asList("user2", "user3", "user4");
        IMResult<MultiMessageResult> resultMulticastMessage = MessageAdmin.multicastMessage("user1", multicastReceivers, 0, payload);
        if (resultMulticastMessage != null && resultMulticastMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("multi message success, messageid is " + resultMulticastMessage.getResult().getMessageUid());
        } else {
            System.out.println("multi message failure");
            System.exit(-1);
        }

        voidIMResult = MessageAdmin.recallMultiCastMessage("user1", resultMulticastMessage.result.getMessageUid(), multicastReceivers);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Success");
        } else {
            System.out.println("failure");
        }
    }

    //***********************************************
    //****  一些其它的功能，比如创建频道，更新用户设置等
    //***********************************************
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

        String channelName = "MyChannel";
        String channelOwner = "user1";
        InputCreateChannel inputCreateChannel = new InputCreateChannel();
        inputCreateChannel.setName(channelName);
        inputCreateChannel.setOwner(channelOwner);
        IMResult<OutputCreateChannel> resultCreateChannel = GeneralAdmin.createChannel(inputCreateChannel);
        if (resultCreateChannel != null && resultCreateChannel.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("success");
            inputCreateChannel.setTargetId(resultCreateChannel.result.getTargetId());
        } else {
            System.out.println("create channel failure");
            System.exit(-1);
        }

        IMResult<OutputGetChannelInfo> resultGetChannel = GeneralAdmin.getChannelInfo(inputCreateChannel.getTargetId());
        if(resultGetChannel != null && resultGetChannel.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS
            && resultGetChannel.getResult().getName().equals(channelName)
            && resultGetChannel.getResult().getOwner().equals(channelOwner)) {
            System.out.println("success");
        } else {
            System.out.println("get channel failure");
            System.exit(-1);
        }

        IMResult<Void> voidIMResult = GeneralAdmin.destroyChannel(inputCreateChannel.getTargetId());
        if(voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("success");
        } else {
            System.out.println("destroy channel failure");
            System.exit(-1);
        }

        resultGetChannel = GeneralAdmin.getChannelInfo(inputCreateChannel.getTargetId());
        if(resultGetChannel != null && resultGetChannel.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && (resultGetChannel.getResult().getState() & ProtoConstants.ChannelState.Channel_State_Mask_Deleted) > 0) {
            System.out.println("success");
        } else {
            System.out.println("get channel failure");
            System.exit(-1);
        }

        IMResult<HealthCheckResult> health = GeneralAdmin.healthCheck();
        if(health != null && health.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println(health.result);
        } else {
            System.out.println("health check failure");
            System.exit(-1);
        }
    }

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
        createRobot.setSecret(robotSecret);
        createRobot.setCallback("http://127.0.0.1:8883/robot/recvmsg");
        IMResult<OutputCreateRobot> resultCreateRobot = UserAdmin.createRobot(createRobot);
        if (resultCreateRobot != null && resultCreateRobot.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Create robot " + resultCreateRobot.getResult().getUserId() + " success");
        } else {
            System.out.println("Create robot failure");
            System.exit(-1);
        }

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

        voidIMResult1 = robotService.deleteCallback();
        if(voidIMResult1 != null && voidIMResult1.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("delete callback success");
        } else {
            System.out.println("delete callback failure");
            System.exit(-1);
        }

        callbackPojoIMResult = robotService.getCallback();
        if(callbackPojoIMResult != null && callbackPojoIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && StringUtil.isNullOrEmpty(callbackPojoIMResult.getResult().getUrl())) {
            System.out.println("get callback success");
        } else {
            System.out.println("get callback failure");
            System.exit(-1);
        }

        Conversation conversation = new Conversation();
        conversation.setTarget("user2");
        conversation.setType(ProtoConstants.ConversationType.ConversationType_Private);
        MessagePayload payload = new MessagePayload();
        payload.setType(1);
        payload.setSearchableContent("hello world");

        IMResult<SendMessageResult> resultRobotSendMessage = robotService.sendMessage("robot1", conversation, payload);
        if (resultRobotSendMessage != null && resultRobotSendMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("robot send message success");
        } else {
            System.out.println("robot send message failure");
            System.exit(-1);
        }

        IMResult<InputOutputUserInfo> resultRobotGetUserInfo = robotService.getUserInfo("userId1");
        if (resultRobotGetUserInfo != null && resultRobotGetUserInfo.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("robot get user info success");
        } else {
            System.out.println("robot get user info by userId failure");
            System.exit(-1);
        }

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

        IMResult<OutputCreateGroupResult> resultCreateGroup = robotService.createGroup(groupInfo, members, null, null);
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


        IMResult<OutputGroupMemberList> resultGetMembers = robotService.getGroupMembers(groupInfo.getTarget_id());
        if (resultGetMembers != null && resultGetMembers.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get group member success");
        } else {
            System.out.println("create group failure");
            System.exit(-1);
        }

        PojoGroupMember m = new PojoGroupMember();
        m.setMember_id("user0");
        m.setAlias("hello user0");

        voidIMResult = robotService.addGroupMembers(groupInfo.getTarget_id(), Arrays.asList(m), null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("add group member success");
        } else {
            System.out.println("add group member failure");
            System.exit(-1);
        }

        voidIMResult = robotService.kickoffGroupMembers(groupInfo.getTarget_id(), Arrays.asList("user3"), null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("kickoff group member success");
        } else {
            System.out.println("kickoff group member failure");
            System.exit(-1);
        }

        voidIMResult = robotService.setGroupMemberAlias(groupInfo.getTarget_id(), "user3", "test user3", null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("set group member alias success");
        } else {
            System.out.println("set group member alias failure");
            System.exit(-1);
        }

        if(commercialServer) {
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
        voidIMResult = robotService.transferGroup(groupInfo.getTarget_id(), "user2", null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("transfer success");
        } else {
            System.out.println("create group failure");
            System.exit(-1);
        }

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

        voidIMResult = robotService.quitGroup(groupInfo.getTarget_id(), null, null);
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("quit group success");
        } else {
            System.out.println("quit group failure");
            System.exit(-1);
        }

    }

    //***测试频道API功能，仅专业版支持***
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
        IMResult<OutputCreateChannel> resultCreateChannel = GeneralAdmin.createChannel(inputCreateChannel);
        if (resultCreateChannel != null && resultCreateChannel.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("create channel success");
        } else {
            System.out.println("create channel failure");
            System.exit(-1);
        }


        //2. 初始化api，注意端口是80，不是18080
        ChannelServiceApi channelServiceApi = new ChannelServiceApi(IMUrl, channelId, secret);


        //3. 测试channel api功能
        IMResult<Void> resultVoid = channelServiceApi.subscribe("userId2");
        if (resultVoid != null && resultVoid.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("subscribe success");
        } else {
            System.out.println("subscribe failure");
            System.exit(-1);
        }

        resultVoid = channelServiceApi.subscribe("userId3");
        if (resultVoid != null && resultVoid.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("subscribe done");
        } else {
            System.out.println("subscribe failure");
            System.exit(-1);
        }

        resultVoid = GeneralAdmin.subscribeChannel(channelId, "userId4");
        if (resultVoid != null && resultVoid.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("subscribe done");
        } else {
            System.out.println("subscribe failure");
            System.exit(-1);
        }

        IMResult<OutputStringList> resultStringList = channelServiceApi.getSubscriberList();
        if (resultStringList != null && resultStringList.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && resultStringList.getResult().getList().contains("userId2") && resultStringList.getResult().getList().contains("userId3") && resultStringList.getResult().getList().contains("userId4")) {
            System.out.println("get subscriber done");
        } else {
            System.out.println("get subscriber failure");
            System.exit(-1);
        }

        resultVoid = channelServiceApi.unsubscribe("userId2");
        if (resultVoid != null && resultVoid.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("unsubscriber done");
        } else {
            System.out.println("unsubscriber failure");
            System.exit(-1);
        }

        resultStringList = channelServiceApi.getSubscriberList();
        if (resultStringList != null && resultStringList.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && resultStringList.getResult().getList().contains("userId3") && !resultStringList.getResult().getList().contains("userId2")) {
            System.out.println("get subscriber done");
        } else {
            System.out.println("get subscriber failure");
            System.exit(-1);
        }

        IMResult<InputOutputUserInfo> resultGetUserInfo1 = channelServiceApi.getUserInfo("userId3");
        if (resultGetUserInfo1 != null && resultGetUserInfo1.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get user info success");
        } else {
            System.out.println("get user info failure");
            System.exit(-1);
        }

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

        ArticleContent articleContent = new ArticleContent("article1", "https://media.wfcoss.cn/channel-assets/20220816/2dd76540daa9444dae44e942aa1c2bbc.png", "这是一个测试文章", "https://mp.weixin.qq.com/s/W6tanLbALd3qqZM8r3MTgA", true);
        articleContent.addSubArticle("article2", "https://media.wfcoss.cn/channel-assets/20220816/2dd76540daa9444dae44e942aa1c2bbc.png", "这是第二个测试文章", "https://mp.weixin.qq.com/s/W6tanLbALd3qqZM8r3MTgA", false);
        articleContent.addSubArticle("article3", "https://media.wfcoss.cn/channel-assets/20220816/2dd76540daa9444dae44e942aa1c2bbc.png", "这是第三个测试文章", "https://mp.weixin.qq.com/s/W6tanLbALd3qqZM8r3MTgA", false);
        payload = articleContent.toPayload();

        resultSendMessage = channelServiceApi.sendMessage(0, null, payload);
        if (resultSendMessage != null && resultSendMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("send message to all the subscriber success");
        } else {
            System.out.println("send message to all the subscriber  failure");
            System.exit(-1);
        }

        payload.setSearchableContent("hello to user2");

        resultSendMessage = channelServiceApi.sendMessage(0, Arrays.asList("userId2"),payload);
        if (resultSendMessage != null && resultSendMessage.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("send message to user2 success");
        } else {
            System.out.println("send message to user2 failure");
            System.exit(-1);
        }

        IMResult<Void> voidIMResult = channelServiceApi.modifyChannelInfo(ProtoConstants.ModifyChannelInfoType.Modify_Channel_Desc, "this is a test channel, update at:" + new Date().toString());
        if (voidIMResult != null && voidIMResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("modify channel profile success");
        } else {
            System.out.println("modify channel profile failure");
            System.exit(-1);
        }

        List<OutputGetChannelInfo.OutputMenu> menus = new ArrayList<>();
        OutputGetChannelInfo.OutputMenu menu1 = new OutputGetChannelInfo.OutputMenu();
        menu1.menuId = UUID.randomUUID().toString();
        menu1.type = "view";
        menu1.name = "一级菜单1";
        menu1.key = "key1";
        menu1.url = "http://www.baidu.com";
        menus.add(menu1);

        OutputGetChannelInfo.OutputMenu menu2 = new OutputGetChannelInfo.OutputMenu();
        menu2.menuId = UUID.randomUUID().toString();
        menu2.type = "view";
        menu2.name = "一级菜单2";
        menu2.key = "key2";
        menu2.url = "http://www.sohu.com";
        menu2.subMenus = new ArrayList<>();
        menus.add(menu2);

        OutputGetChannelInfo.OutputMenu menu21 = new OutputGetChannelInfo.OutputMenu();
        menu21.menuId = UUID.randomUUID().toString();
        menu21.type = "click";
        menu21.name = "二级菜单21";
        menu21.key = "key21";
        menu21.url = "http://www.sohu.com";
        menu2.subMenus.add(menu21);

        String menuStr = new Gson().toJson(menus);
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
        OutputApplicationConfigData config = channelServiceApi.getApplicationSignature();
        System.out.println(config);
    }
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

    //***********************************************
    //****  物联网相关的API，仅专业版支持
    //***********************************************
    static void testDevice() throws Exception {
        InputCreateDevice createDevice = new InputCreateDevice();
        createDevice.setDeviceId("deviceId1");
        createDevice.setOwners(Arrays.asList("opoGoG__", "userId1"));
        IMResult<OutputCreateDevice> resultCreateDevice = UserAdmin.createOrUpdateDevice(createDevice);
        if (resultCreateDevice != null && resultCreateDevice.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("Create device " + resultCreateDevice.getResult().getDeviceId() + " success");
        } else {
            System.out.println("Create device failure");
            System.exit(-1);
        }

        IMResult<OutputDevice> getDevice = UserAdmin.getDevice("deviceId1");
        if (getDevice != null && getDevice.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS && getDevice.getResult().getDeviceId().equals("deviceId1") && getDevice.getResult().getOwners().contains("opoGoG__")) {
            System.out.println("Get device " + resultCreateDevice.getResult().getDeviceId() + " success");
        } else {
            System.out.println("Get device failure");
            System.exit(-1);
        }

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

    /*
    会议相关接口，仅音视频高级版服务支持
     */
    public static void testConference() throws Exception {
        IMResult<PojoConferenceInfoList> listResult = ConferenceAdmin.listConferences();
        if(listResult == null || listResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get conference list failure");
            System.exit(-1);
        } else {
            System.out.println("conference list " + listResult.getResult().conferenceInfoList);
        }

        for (PojoConferenceInfo conferenceInfo:listResult.getResult().conferenceInfoList) {
            IMResult<Void> destroyResult = ConferenceAdmin.destroy(conferenceInfo.roomId, conferenceInfo.advance);
            if(destroyResult == null || destroyResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("destroy room failure");
                System.exit(-1);
            } else {
                System.out.println("destroy room success");
            }
        }

        listResult = ConferenceAdmin.listConferences();
        if(listResult == null || listResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get conference list failure");
            System.exit(-1);
        } else {
            System.out.println("conference list " + listResult.getResult().conferenceInfoList);
        }

        IMResult<Void> createResult = ConferenceAdmin.createRoom("helloroomid", "hello room description", "123456", 9, false, 0, false, true);
        if(createResult == null || createResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("create conference failure");
            System.exit(-1);
        } else {
            System.out.println("create conference");
        }

        createResult = ConferenceAdmin.createRoom("helloroomid2", "hello room description advanced", "123456", 20, true, 0, false, true);
        if(createResult == null || createResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("create conference failure");
            System.exit(-1);
        } else {
            System.out.println("create conference");
        }

        listResult = ConferenceAdmin.listConferences();
        if(listResult == null || listResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
            System.out.println("get conference list failure");
            System.exit(-1);
        } else {
            System.out.println("conference list " + listResult.getResult().conferenceInfoList);
        }

        for (PojoConferenceInfo conferenceInfo:listResult.getResult().conferenceInfoList) {
            IMResult<PojoConferenceParticipantList> listParticipantsResult = ConferenceAdmin.listParticipants(conferenceInfo.roomId, conferenceInfo.advance);
            if(listParticipantsResult == null || listParticipantsResult.getErrorCode() != ErrorCode.ERROR_CODE_SUCCESS) {
                System.out.println("list participants failure");
                System.exit(-1);
            } else {
                System.out.println("list participants success");
            }
        }
    }
}
