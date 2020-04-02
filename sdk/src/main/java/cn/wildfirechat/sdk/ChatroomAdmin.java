package cn.wildfirechat.sdk;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.sdk.utilities.AdminHttpUtils;

public class ChatroomAdmin {
    public static IMResult<OutputCreateChatroom> createChatroom(String chatroomId, String title, String desc ,String portrait, String extra, Integer state) throws Exception {
        String path = APIPath.Create_Chatroom;
        InputCreateChatroom input = new InputCreateChatroom();
        input.setChatroomId(chatroomId);
        input.setTitle(title);
        input.setDesc(desc);
        input.setPortrait(portrait);
        input.setExtra(extra);
        input.setState(state);
        return AdminHttpUtils.httpJsonPost(path, input, OutputCreateChatroom.class);
    }

    public static IMResult<Void> destroyChatroom(String chatroomId) throws Exception {
        String path = APIPath.Chatroom_Destroy;
        InputDestoryChatroom input = new InputDestoryChatroom();
        input.setChatroomId(chatroomId);
        return AdminHttpUtils.httpJsonPost(path, input, Void.class);
    }

    public static IMResult<OutputGetChatroomInfo> getChatroomInfo(String chatroomId) throws Exception {
        String path = APIPath.Chatroom_Info;
        InputGetChatroomInfo input = new InputGetChatroomInfo(chatroomId);
        return AdminHttpUtils.httpJsonPost(path, input, OutputGetChatroomInfo.class);
    }

    public static IMResult<OutputStringList> getChatroomMembers(String chatroomId) throws Exception {
        String path = APIPath.Chatroom_GetMembers;
        InputGetChatroomInfo input = new InputGetChatroomInfo(chatroomId);
        return AdminHttpUtils.httpJsonPost(path, input, OutputStringList.class);
    }

    //仅专业版支持
    //status：0，正常；1，禁言；2，禁止加入
    public static IMResult<Void> setChatroomBlacklist(String chatroomId, String userId, int status) throws Exception {
        String path = APIPath.Chatroom_SetBlacklist;
        InputSetChatroomBlacklist input = new InputSetChatroomBlacklist(chatroomId, userId, status, 0);
        return AdminHttpUtils.httpJsonPost(path, input, Void.class);
    }

    public static IMResult<OutputChatroomBlackInfos> getChatroomBlacklist(String chatroomId) throws Exception {
        String path = APIPath.Chatroom_GetBlacklist;
        InputChatroomId input = new InputChatroomId(chatroomId);
        return AdminHttpUtils.httpJsonPost(path, input, OutputChatroomBlackInfos.class);
    }

    //status: 1 set; 0 unset
    public static IMResult<Void> setChatroomManager(String chatroomId, String userId, int status) throws Exception {
        String path = APIPath.Chatroom_SetManager;
        InputSetChatroomManager input = new InputSetChatroomManager(chatroomId, userId, status);
        return AdminHttpUtils.httpJsonPost(path, input, Void.class);
    }

    public static IMResult<OutputStringList> getChatroomManagerList(String chatroomId) throws Exception {
        String path = APIPath.Chatroom_GetManagerList;
        InputChatroomId input = new InputChatroomId(chatroomId);
        return AdminHttpUtils.httpJsonPost(path, input, OutputStringList.class);
    }

    public static IMResult<Void> setChatroomMute(String chatroomId, boolean mute) throws Exception {
        String path = APIPath.Chatroom_MuteAll;
        InputChatroomMute input = new InputChatroomMute(chatroomId, mute ? 1 : 0);
        return AdminHttpUtils.httpJsonPost(path, input, Void.class);
    }

}
