package cn.wildfirechat.sdk;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.sdk.utilities.AdminHttpUtils;

public class FriendAdmin {
    public static IMResult<Void> updateFriendStatus(String userId, String targetId, int status) throws Exception {
        String path = APIPath.Friend_Update_Status;
        InputFriendRequest input = new InputFriendRequest();
        input.setUserId(userId);
        input.setFriendUid(targetId);
        input.setStatus(status);
        return AdminHttpUtils.httpJsonPost(path, input, Void.class);
    }

    public static IMResult<Void> getFriendStatusList(String userId, int status) throws Exception {
        String path = APIPath.Friend_Get_List;
        InputGetFriendList input = new InputGetFriendList();
        input.setUserId(userId);
        input.setStatus(status);
        return AdminHttpUtils.httpJsonPost(path, input, Void.class);
    }

}
