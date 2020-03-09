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


    
}
