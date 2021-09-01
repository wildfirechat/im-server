package cn.wildfirechat.sdk;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.sdk.utilities.AdminHttpUtils;

public class RelationAdmin {
    public static IMResult<Void> setUserFriend(String userId, String targetId, boolean isFriend, String extra) throws Exception {
        String path = APIPath.Friend_Update_Status;
        InputUpdateFriendStatusRequest input = new InputUpdateFriendStatusRequest();
        input.setUserId(userId);
        input.setFriendUid(targetId);
        input.setStatus(isFriend ? 0 : 1); //历史遗留问题，在IM数据库中0是好友，1是好友被删除。
        input.setExtra(extra);
        return AdminHttpUtils.httpJsonPost(path, input, Void.class);
    }

    public static IMResult<OutputStringList> getFriendList(String userId) throws Exception {
        String path = APIPath.Friend_Get_List;
        InputUserId input = new InputUserId();
        input.setUserId(userId);
        return AdminHttpUtils.httpJsonPost(path, input, OutputStringList.class);
    }

    public static IMResult<Void> setUserBlacklist(String userId, String targetId, boolean isBlacklist) throws Exception {
        String path = APIPath.Blacklist_Update_Status;
        InputBlacklistRequest input = new InputBlacklistRequest();
        input.setUserId(userId);
        input.setTargetUid(targetId);
        input.setStatus(isBlacklist ? 2 : 1);
        return AdminHttpUtils.httpJsonPost(path, input, Void.class);
    }

    public static IMResult<OutputStringList> getUserBlacklist(String userId) throws Exception {
        String path = APIPath.Blacklist_Get_List;
        InputUserId input = new InputUserId();
        input.setUserId(userId);
        return AdminHttpUtils.httpJsonPost(path, input, OutputStringList.class);
    }

    public static IMResult<Void> updateFriendAlias(String operator, String targetId, String alias) throws Exception {
        String path = APIPath.Friend_Set_Alias;
        InputUpdateAlias input = new InputUpdateAlias();
        input.setOperator(operator);
        input.setTargetId(targetId);
        input.setAlias(alias);
        return AdminHttpUtils.httpJsonPost(path, input, Void.class);
    }

    public static IMResult<OutputGetAlias> getFriendAlias(String operator, String targetId) throws Exception {
        String path = APIPath.Friend_Get_Alias;
        InputGetAlias input = new InputGetAlias();
        input.setOperator(operator);
        input.setTargetId(targetId);
        return AdminHttpUtils.httpJsonPost(path, input, OutputGetAlias.class);
    }

    public static IMResult<Void> updateFriendExtra(String operator, String targetId, String extra) throws Exception {
        String path = APIPath.Friend_Set_Extra;
        InputUpdateFriendExtra input = new InputUpdateFriendExtra();
        input.setOperator(operator);
        input.setTargetId(targetId);
        input.setExtra(extra);
        return AdminHttpUtils.httpJsonPost(path, input, Void.class);
    }
    public static IMResult<Void> sendFriendRequest(String userId, String targetId, String reason, boolean force) throws Exception {
        String path = APIPath.Friend_Send_Request;
        InputAddFriendRequest input = new InputAddFriendRequest();
        input.setUserId(userId);
        input.setFriendUid(targetId);
        input.setReason(reason);
        input.setForce(force);
        return AdminHttpUtils.httpJsonPost(path, input, Void.class);
    }

    public static IMResult<RelationPojo> getRelation(String userId, String targetId) throws Exception {
        String path = APIPath.Relation_Get;
        StringPairPojo input = new StringPairPojo(userId, targetId);
        return AdminHttpUtils.httpJsonPost(path, input, RelationPojo.class);
    }
}
