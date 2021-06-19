package cn.wildfirechat.sdk;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.sdk.utilities.AdminHttpUtils;

import java.util.List;

public class GroupAdmin {
    public static IMResult<OutputCreateGroupResult> createGroup(String operator, PojoGroupInfo group_info, List<PojoGroupMember> members, List<Integer> to_lines, MessagePayload  notify_message) throws Exception {
        String path = APIPath.Create_Group;
        PojoGroup pojoGroup = new PojoGroup();
        pojoGroup.setGroup_info(group_info);
        pojoGroup.setMembers(members);
        InputCreateGroup createGroup = new InputCreateGroup();
        createGroup.setGroup(pojoGroup);
        createGroup.setOperator(operator);
        createGroup.setTo_lines(to_lines);
        createGroup.setNotify_message(notify_message);

        return AdminHttpUtils.httpJsonPost(path, createGroup, OutputCreateGroupResult.class);
    }

    public static IMResult<PojoGroupInfo> getGroupInfo(String groupId) throws Exception {
        String path = APIPath.Group_Get_Info;
        InputGetGroup input = new InputGetGroup();
        input.setGroupId(groupId);

        return AdminHttpUtils.httpJsonPost(path, input, PojoGroupInfo.class);
    }

    public static IMResult<Void> dismissGroup(String operator, String groupId, List<Integer> to_lines, MessagePayload  notify_message) throws Exception {
        String path = APIPath.Group_Dismiss;
        InputDismissGroup dismissGroup = new InputDismissGroup();
        dismissGroup.setOperator(operator);
        dismissGroup.setGroup_id(groupId);
        dismissGroup.setTo_lines(to_lines);
        dismissGroup.setNotify_message(notify_message);
        return AdminHttpUtils.httpJsonPost(path, dismissGroup, Void.class);
    }

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


    public static IMResult<OutputGroupMemberList> getGroupMembers(String groupId) throws Exception {
        String path = APIPath.Group_Member_List;
        InputGetGroup input = new InputGetGroup();
        input.setGroupId(groupId);
        return AdminHttpUtils.httpJsonPost(path, input, OutputGroupMemberList.class);
    }

    public static IMResult<Void> addGroupMembers(String operator, String groupId, List<PojoGroupMember> groupMembers, List<Integer> to_lines, MessagePayload  notify_message) throws Exception {
        String path = APIPath.Group_Member_Add;
        InputAddGroupMember addGroupMember = new InputAddGroupMember();
        addGroupMember.setGroup_id(groupId);
        addGroupMember.setMembers(groupMembers);
        addGroupMember.setOperator(operator);
        addGroupMember.setTo_lines(to_lines);
        addGroupMember.setNotify_message(notify_message);
        return AdminHttpUtils.httpJsonPost(path, addGroupMember, Void.class);
    }

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

    public static IMResult<Void> quitGroup(String operator, String groupId, List<Integer> to_lines, MessagePayload  notify_message) throws Exception {
        String path = APIPath.Group_Member_Quit;
        InputQuitGroup quitGroup = new InputQuitGroup();
        quitGroup.setGroup_id(groupId);
        quitGroup.setOperator(operator);
        quitGroup.setTo_lines(to_lines);
        quitGroup.setNotify_message(notify_message);
        return AdminHttpUtils.httpJsonPost(path, quitGroup, Void.class);
    }

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

    public static IMResult<OutputGroupIds> getUserGroups(String user) throws Exception {
        String path = APIPath.Get_User_Groups;
        InputUserId inputUserId = new InputUserId();
        inputUserId.setUserId(user);
        return AdminHttpUtils.httpJsonPost(path, inputUserId, OutputGroupIds.class);
    }

}
