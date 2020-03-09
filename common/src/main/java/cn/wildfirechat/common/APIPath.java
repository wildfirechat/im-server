package cn.wildfirechat.common;

public interface APIPath {
    String Create_Chatroom = "/admin/chatroom/create";
    String Chatroom_Destroy = "/admin/chatroom/del";
    String Chatroom_Info = "/admin/chatroom/info";
    String Chatroom_GetMembers = "/admin/chatroom/members";


    String Sensitive_Add = "/admin/sensitive/add";
    String Sensitive_Del = "/admin/sensitive/del";
    String Sensitive_Query = "/admin/sensitive/query";

    String Create_User = "/admin/user/create";
    String Destroy_User = "/admin/user/destroy";
    String Create_Robot = "/admin/robot/create";
    String User_Get_Token = "/admin/user/get_token";
    String User_Update_Block_Status = "/admin/user/update_block_status";
    String User_Get_Info = "/admin/user/get_info";
    String User_Get_Blocked_List = "/admin/user/get_blocked_list";
    String User_Check_Block_Status = "/admin/user/check_block_status";
    String User_Get_Online_Status = "/admin/user/onlinestatus";


    String Friend_Update_Status = "/admin/friend/status";
    String Friend_Get_List = "/admin/friend/list";
    String Blacklist_Update_Status = "/admin/blacklist/status";
    String Blacklist_Get_List = "/admin/blacklist/list";
    String Friend_Get_Alias = "/admin/friend/get_alias";
    String Friend_Set_Alias = "/admin/friend/set_alias";

    String Msg_Send = "/admin/message/send";
    String Msg_Recall = "/admin/message/recall";
    String Msg_Broadcast = "/admin/message/broadcast";
    String Msg_Multicast = "/admin/message/multicast";

    String Create_Group = "/admin/group/create";
    String Group_Dismiss = "/admin/group/del";
    String Group_Transfer = "/admin/group/transfer";
    String Group_Get_Info = "/admin/group/get_info";
    String Group_Modify_Info = "/admin/group/modify";
    String Group_Member_List = "/admin/group/member/list";
    String Group_Member_Add = "/admin/group/member/add";
    String Group_Member_Kickoff = "/admin/group/member/del";
    String Group_Member_Quit = "/admin/group/member/quit";
    String Group_Set_Manager = "/admin/group/manager/set";
    String Get_User_Groups = "/admin/group/of_user";


    String Create_Channel = "/admin/channel/create";
    String Get_System_Setting = "/admin/system/get_setting";
    String Put_System_Setting = "/admin/system/put_setting";

    String Channel_User_Info = "/channel/user_info";
    String Channel_Update_Profile = "/channel/update_profile";
    String Channel_Get_Profile = "/channel/get_profile";
    String Channel_Message_Send = "/channel/message/send";
    String Channel_Subscribe = "/channel/subscribe";
    String Channel_Subscriber_List = "/channel/subscriber_list";

    String Robot_User_Info = "/robot/user_info";
    String Robot_Message_Send = "/robot/message/send";
}
