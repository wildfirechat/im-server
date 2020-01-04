package cn.wildfirechat.sdk;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.sdk.utilities.AdminHttpUtils;

public class FriendAdmin {
    public static IMResult<Void> updateFriendStatus(String operator, String targetId, int status) throws Exception {
        String path = APIPath.Friend_Update_Status;
        InputFriendRequest input = new InputFriendRequest();
        input.setUserId(operator);
        input.setFriendUid(targetId);
        input.setStatus(status);
        return AdminHttpUtils.httpJsonPost(path, input, Void.class);
    }

    public static IMResult<OutputStringList> getFriendStatusList(String operator, int status) throws Exception {
        String path = APIPath.Friend_Get_List;
        InputGetFriendList input = new InputGetFriendList();
        input.setUserId(operator);
        input.setStatus(status);
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
}
