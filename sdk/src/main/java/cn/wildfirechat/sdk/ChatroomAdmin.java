package cn.wildfirechat.sdk;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.sdk.utilities.AdminHttpUtils;

public class ChatroomAdmin {
    public static IMResult<OutputCreateChatroom> createChatroom(InputCreateChatroom input) throws Exception {
        String path = APIPath.Create_Chatroom;
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
    
}
