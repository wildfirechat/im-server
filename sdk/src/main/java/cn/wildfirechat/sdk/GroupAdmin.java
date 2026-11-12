package cn.wildfirechat.sdk;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.sdk.utilities.AdminHttpUtils;

import java.util.List;

/**
 * 群组管理类
 * <p>
 * 提供群组管理相关的功能，包括：
 * <ul>
 * <li>群组的创建、解散、转移</li>
 * <li>群组信息修改</li>
 * <li>按群组名称搜索群组</li>
 * <li>群组成员管理（添加、移除、踢出）</li>
 * <li>群组管理员设置</li>
 * <li>群组成员禁言</li>
 * <li>用户群组查询</li>
 * </ul>
 * </p>
 */
public class GroupAdmin {
    /**
     * 创建群组
     * @param operator 操作者用户ID
     * @param group_info 群组信息
     * @param members 群组成员列表
     * @param member_extra 成员额外信息
     * @param to_lines 消息同步到的线路列表
     * @param notify_message 通知消息
     * @return 创建结果，包含群组ID
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputCreateGroupResult> createGroup(String operator, PojoGroupInfo group_info, List<PojoGroupMember> members, String member_extra, List<Integer> to_lines, MessagePayload  notify_message) throws Exception {
        String path = APIPath.Create_Group;
        PojoGroup pojoGroup = new PojoGroup();
        pojoGroup.setGroup_info(group_info);
        pojoGroup.setMembers(members);
        InputCreateGroup createGroup = new InputCreateGroup();
        createGroup.setGroup(pojoGroup);
        createGroup.setOperator(operator);
        createGroup.setMember_extra(member_extra);
        createGroup.setTo_lines(to_lines);
        createGroup.setNotify_message(notify_message);

        return AdminHttpUtils.httpJsonPost(path, createGroup, OutputCreateGroupResult.class);
    }

    /**
     * 获取群组信息
     * @param groupId 群组ID
     * @return 群组信息
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<PojoGroupInfo> getGroupInfo(String groupId) throws Exception {
        String path = APIPath.Group_Get_Info;
        InputGetGroup input = new InputGetGroup();
        input.setGroupId(groupId);

        return AdminHttpUtils.httpJsonPost(path, input, PojoGroupInfo.class);
    }

    /**
     * 批量获取群组信息
     * @param groupIds 群组ID列表
     * @return 群组信息列表
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<PojoGroupInfoList> batchGroupInfos(List<String> groupIds) throws Exception {
        String path = APIPath.Group_Batch_Info;
        return AdminHttpUtils.httpJsonPost(path, groupIds, PojoGroupInfoList.class);
    }

    /**
     * 根据群组名称搜索群组（第一页，20条）
     * @param keyword 群组名称关键词，模糊匹配
     * @return 搜索结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<PojoSearchGroupRes> searchGroupByName(String keyword) throws Exception {
        return searchGroupByName(keyword, 0, 20);
    }

    /**
     * 根据群组名称搜索群组
     * @param keyword 群组名称关键词，模糊匹配
     * @param page 页码，从0开始
     * @param count 每页数量，为0时服务端使用默认值20，最大100
     * @return 搜索结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<PojoSearchGroupRes> searchGroupByName(String keyword, int page, int count) throws Exception {
        String path = APIPath.Group_Search;
        PojoSearchGroupReq req = new PojoSearchGroupReq();
        req.keyword = keyword;
        req.page = page;
        req.count = count;
        return AdminHttpUtils.httpJsonPost(path, req, PojoSearchGroupRes.class);
    }

    /**
     * 解散群组
     * @param operator 操作者用户ID
     * @param groupId 群组ID
     * @param to_lines 消息同步到的线路列表
     * @param notify_message 通知消息
     * @return 解散结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<Void> dismissGroup(String operator, String groupId, List<Integer> to_lines, MessagePayload  notify_message) throws Exception {
        String path = APIPath.Group_Dismiss;
        InputDismissGroup dismissGroup = new InputDismissGroup();
        dismissGroup.setOperator(operator);
        dismissGroup.setGroup_id(groupId);
        dismissGroup.setTo_lines(to_lines);
        dismissGroup.setNotify_message(notify_message);
        return AdminHttpUtils.httpJsonPost(path, dismissGroup, Void.class);
    }

    /**
     * 转让群组
     * @param operator 操作者用户ID
     * @param groupId 群组ID
     * @param newOwner 新群主用户ID
     * @param to_lines 消息同步到的线路列表
     * @param notify_message 通知消息
     * @return 转让结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<Void> transferGroup(String operator, String groupId, String newOwner, List<Integer> to_lines, MessagePayload  notify_message) throws Exception {
        String path = APIPath.Group_Transfer;
        InputTransferGroup transferGroup = new InputTransferGroup();
        transferGroup.setGroup_id(groupId);
        transferGroup.setNew_owner(newOwner);
        transferGroup.setOperator(operator);
        transferGroup.setTo_lines(to_lines);
        transferGroup.setNotify_message(notify_message);
        return AdminHttpUtils.httpJsonPost(path, transferGroup, Void.class);
    }

    /**
     * 修改群组信息
     * @param operator 操作者用户ID
     * @param groupId 群组ID
     * @param type 修改信息类型（ModifyGroupInfoType）
     * @param value 新值
     * @param to_lines 消息同步到的线路列表
     * @param notify_message 通知消息
     * @return 修改结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<Void> modifyGroupInfo(String operator, String groupId, /*ModifyGroupInfoType*/int type, String value, List<Integer> to_lines, MessagePayload  notify_message) throws Exception {
        String path = APIPath.Group_Modify_Info;
        InputModifyGroupInfo modifyGroupInfo = new InputModifyGroupInfo();
        modifyGroupInfo.setGroup_id(groupId);
        modifyGroupInfo.setOperator(operator);
        modifyGroupInfo.setTo_lines(to_lines);
        modifyGroupInfo.setType(type);
        modifyGroupInfo.setValue(value);
        modifyGroupInfo.setNotify_message(notify_message);
        return AdminHttpUtils.httpJsonPost(path, modifyGroupInfo, Void.class);
    }

    /**
     * 获取群组成员列表
     * @param groupId 群组ID
     * @return 群组成员列表
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputGroupMemberList> getGroupMembers(String groupId) throws Exception {
        String path = APIPath.Group_Member_List;
        InputGetGroup input = new InputGetGroup();
        input.setGroupId(groupId);
        return AdminHttpUtils.httpJsonPost(path, input, OutputGroupMemberList.class);
    }

    /**
     * 获取群组成员信息
     * @param groupId 群组ID
     * @param memberId 成员用户ID
     * @return 群组成员信息
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<PojoGroupMember> getGroupMember(String groupId, String memberId) throws Exception {
        String path = APIPath.Group_Member_Get;
        InputGetGroupMember input = new InputGetGroupMember();
        input.setGroupId(groupId);
        input.setMemberId(memberId);
        return AdminHttpUtils.httpJsonPost(path, input, PojoGroupMember.class);
    }

    /**
     * 添加群组成员
     * @param operator 操作者用户ID
     * @param groupId 群组ID
     * @param groupMembers 要添加的成员列表
     * @param member_extra 成员额外信息
     * @param to_lines 消息同步到的线路列表
     * @param notify_message 通知消息
     * @return 添加结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<Void> addGroupMembers(String operator, String groupId, List<PojoGroupMember> groupMembers, String member_extra, List<Integer> to_lines, MessagePayload  notify_message) throws Exception {
        String path = APIPath.Group_Member_Add;
        InputAddGroupMember addGroupMember = new InputAddGroupMember();
        addGroupMember.setGroup_id(groupId);
        addGroupMember.setMembers(groupMembers);
        addGroupMember.setOperator(operator);
        addGroupMember.setMemberExtra(member_extra);
        addGroupMember.setTo_lines(to_lines);
        addGroupMember.setNotify_message(notify_message);
        return AdminHttpUtils.httpJsonPost(path, addGroupMember, Void.class);
    }

    /**
     * 设置或取消群组管理员
     * @param operator 操作者用户ID
     * @param groupId 群组ID
     * @param groupMemberIds 成员用户ID列表
     * @param isManager true-设置为管理员，false-取消管理员
     * @param to_lines 消息同步到的线路列表
     * @param notify_message 通知消息
     * @return 设置结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<Void> setGroupManager(String operator, String groupId, List<String> groupMemberIds, boolean isManager, List<Integer> to_lines, MessagePayload  notify_message) throws Exception {
        String path = APIPath.Group_Set_Manager;
        InputSetGroupManager addGroupMember = new InputSetGroupManager();
        addGroupMember.setGroup_id(groupId);
        addGroupMember.setMembers(groupMemberIds);
        addGroupMember.setIs_manager(isManager);
        addGroupMember.setOperator(operator);
        addGroupMember.setTo_lines(to_lines);
        addGroupMember.setNotify_message(notify_message);
        return AdminHttpUtils.httpJsonPost(path, addGroupMember, Void.class);
    }

    /**
     * 禁言或解禁群组成员
     * @param operator 操作者用户ID
     * @param groupId 群组ID
     * @param groupMemberIds 成员用户ID列表
     * @param isMute true-禁言，false-解禁
     * @param to_lines 消息同步到的线路列表
     * @param notify_message 通知消息
     * @return 禁言结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<Void> muteGroupMemeber(String operator, String groupId, List<String> groupMemberIds, boolean isMute, List<Integer> to_lines, MessagePayload  notify_message) throws Exception {
        String path = APIPath.Group_Mute_Member;
        InputMuteGroupMember addGroupMember = new InputMuteGroupMember();
        addGroupMember.setGroup_id(groupId);
        addGroupMember.setMembers(groupMemberIds);
        addGroupMember.setIs_manager(isMute);
        addGroupMember.setOperator(operator);
        addGroupMember.setTo_lines(to_lines);
        addGroupMember.setNotify_message(notify_message);
        return AdminHttpUtils.httpJsonPost(path, addGroupMember, Void.class);
    }

    /**
     * 允许或禁止群组成员发言
     * @param operator 操作者用户ID
     * @param groupId 群组ID
     * @param groupMemberIds 成员用户ID列表
     * @param isAllow true-允许，false-禁止
     * @param to_lines 消息同步到的线路列表
     * @param notify_message 通知消息
     * @return 设置结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<Void> allowGroupMemeber(String operator, String groupId, List<String> groupMemberIds, boolean isAllow, List<Integer> to_lines, MessagePayload  notify_message) throws Exception {
        String path = APIPath.Group_Allow_Member;
        InputMuteGroupMember addGroupMember = new InputMuteGroupMember();
        addGroupMember.setGroup_id(groupId);
        addGroupMember.setMembers(groupMemberIds);
        addGroupMember.setIs_manager(isAllow);
        addGroupMember.setOperator(operator);
        addGroupMember.setTo_lines(to_lines);
        addGroupMember.setNotify_message(notify_message);
        return AdminHttpUtils.httpJsonPost(path, addGroupMember, Void.class);
    }

    /**
     * 踢出群组成员
     * @param operator 操作者用户ID
     * @param groupId 群组ID
     * @param groupMemberIds 要踢出的成员用户ID列表
     * @param to_lines 消息同步到的线路列表
     * @param notify_message 通知消息
     * @return 踢出结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<Void> kickoffGroupMembers(String operator, String groupId, List<String> groupMemberIds, List<Integer> to_lines, MessagePayload  notify_message) throws Exception {
        String path = APIPath.Group_Member_Kickoff;
        InputKickoffGroupMember kickoffGroupMember = new InputKickoffGroupMember();
        kickoffGroupMember.setGroup_id(groupId);
        kickoffGroupMember.setMembers(groupMemberIds);
        kickoffGroupMember.setOperator(operator);
        kickoffGroupMember.setTo_lines(to_lines);
        kickoffGroupMember.setNotify_message(notify_message);
        return AdminHttpUtils.httpJsonPost(path, kickoffGroupMember, Void.class);
    }

    /**
     * 退出群组
     * @param operator 操作者用户ID
     * @param groupId 群组ID
     * @param to_lines 消息同步到的线路列表
     * @param notify_message 通知消息
     * @return 退出结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<Void> quitGroup(String operator, String groupId, List<Integer> to_lines, MessagePayload  notify_message) throws Exception {
        String path = APIPath.Group_Member_Quit;
        InputQuitGroup quitGroup = new InputQuitGroup();
        quitGroup.setGroup_id(groupId);
        quitGroup.setOperator(operator);
        quitGroup.setTo_lines(to_lines);
        quitGroup.setNotify_message(notify_message);
        return AdminHttpUtils.httpJsonPost(path, quitGroup, Void.class);
    }

    /**
     * 设置群组成员别名
     * @param operator 操作者用户ID
     * @param groupId 群组ID
     * @param memberId 成员用户ID
     * @param alias 别名
     * @param to_lines 消息同步到的线路列表
     * @param notify_message 通知消息
     * @return 设置结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<Void> setGroupMemberAlias(String operator, String groupId, String memberId, String alias, List<Integer> to_lines, MessagePayload  notify_message) throws Exception {
        String path = APIPath.Group_Set_Member_Alias;
        InputSetGroupMemberAlias input = new InputSetGroupMemberAlias();
        input.setGroup_id(groupId);
        input.setOperator(operator);
        input.setMemberId(memberId);
        input.setAlias(alias);
        input.setTo_lines(to_lines);
        input.setNotify_message(notify_message);
        return AdminHttpUtils.httpJsonPost(path, input, Void.class);
    }

    /**
     * 设置群组成员额外信息
     * @param operator 操作者用户ID
     * @param groupId 群组ID
     * @param memberId 成员用户ID
     * @param extra 额外信息
     * @param to_lines 消息同步到的线路列表
     * @param notify_message 通知消息
     * @return 设置结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<Void> setGroupMemberExtra(String operator, String groupId, String memberId, String extra, List<Integer> to_lines, MessagePayload  notify_message) throws Exception {
        String path = APIPath.Group_Set_Member_Extra;
        InputSetGroupMemberExtra input = new InputSetGroupMemberExtra();
        input.setGroup_id(groupId);
        input.setOperator(operator);
        input.setMemberId(memberId);
        input.setExtra(extra);
        input.setTo_lines(to_lines);
        input.setNotify_message(notify_message);
        return AdminHttpUtils.httpJsonPost(path, input, Void.class);
    }

    /**
     * 设置群组备注（用户个性化设置）
     * @param userId 用户ID
     * @param groupId 群组ID
     * @param remark 备注内容
     * @return 设置结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<Void> setGroupRemark(String userId, String groupId, String remark) throws Exception {
        return GeneralAdmin.setUserSetting(userId, 26, groupId, remark);
    }

    /**
     * 获取群组备注（用户个性化设置）
     * @param userId 用户ID
     * @param groupId 群组ID
     * @return 备注内容
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<String> getGroupRemark(String userId, String groupId) throws Exception {
        IMResult<UserSettingPojo> imResult = GeneralAdmin.getUserSetting(userId, 26, groupId);
        IMResult<String> result = new IMResult<>();
        result.code = imResult.code;;
        result.msg = imResult.msg;;
        if(imResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            result.result = imResult.result.getValue();
        }
        return result;
    }

    /**
     * 设置群组是否收藏（用户个性化设置）
     * @param userId 用户ID
     * @param groupId 群组ID
     * @param fav true-收藏，false-取消收藏
     * @return 设置结果
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<Void> setFavGroup(String userId, String groupId, boolean fav) throws Exception {
        return GeneralAdmin.setUserSetting(userId, 6, groupId, fav?"1":"0");
    }

    /**
     * 检查群组是否收藏（用户个性化设置）
     * @param userId 用户ID
     * @param groupId 群组ID
     * @return true-已收藏，false-未收藏
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<Boolean> isFavGroup(String userId, String groupId) throws Exception {
        IMResult<UserSettingPojo> imResult = GeneralAdmin.getUserSetting(userId, 6, groupId);
        IMResult<Boolean> result = new IMResult<>();
        result.code = imResult.code;;
        result.msg = imResult.msg;;
        if(imResult.getErrorCode() == ErrorCode.ERROR_CODE_SUCCESS) {
            result.result = "1".equals(imResult.getResult().getValue());
        }
        return result;
    }

    /**
     * 获取用户的群组列表
     * @param user 用户ID
     * @return 用户所属的群组ID列表
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputGroupIds> getUserGroups(String user) throws Exception {
        String path = APIPath.Get_User_Groups;
        InputUserId inputUserId = new InputUserId();
        inputUserId.setUserId(user);
        return AdminHttpUtils.httpJsonPost(path, inputUserId, OutputGroupIds.class);
    }

    /**
     * 根据成员类型获取用户的群组列表
     * @param user 用户ID
     * @param groupMemberType 群组成员类型列表（GroupMemberType）
     * @return 用户所属的群组ID列表
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputGroupIds> getUserGroupsByType(String user, List</*ProtoConstants.GroupMemberType*/Integer> groupMemberType) throws Exception {
        String path = APIPath.Get_User_Groups_By_Type;
        InputGetUserGroupByType input = new InputGetUserGroupByType(user, groupMemberType);
        return AdminHttpUtils.httpJsonPost(path, input, OutputGroupIds.class);
    }

    /**
     * 获取两个用户的共同群组
     * @param user1 用户1的ID
     * @param user2 用户2的ID
     * @return 共同群组ID列表
     * @throws Exception 请求失败时抛出异常
     */
    public static IMResult<OutputGroupIds> getCommonGroups(String user1, String user2) throws Exception {
        String path = APIPath.Get_Common_Groups;
        StringPairPojo intput = new StringPairPojo(user1, user2);
        return AdminHttpUtils.httpJsonPost(path, intput, OutputGroupIds.class);
    }

}
