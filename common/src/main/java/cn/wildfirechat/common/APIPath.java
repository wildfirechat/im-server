package cn.wildfirechat.common;

public interface APIPath {
    String Create_Chatroom = "/admin/chatroom/create";
    String Chatroom_Destroy = "/admin/chatroom/del";
    String Chatroom_Info = "/admin/chatroom/info";
    String Chatroom_GetMembers = "/admin/chatroom/members";
    String Chatroom_SetBlacklist = "/admin/chatroom/set_black_status";
    String Chatroom_GetBlacklist = "/admin/chatroom/get_black_status";
    String Chatroom_SetManager = "/admin/chatroom/set_manager";
    String Chatroom_GetManagerList = "/admin/chatroom/get_manager_list";
    String Chatroom_MuteAll = "/admin/chatroom/mute_all";


    String Sensitive_Add = "/admin/sensitive/add";
    String Sensitive_Del = "/admin/sensitive/del";
    String Sensitive_Query = "/admin/sensitive/query";

    String Create_User = "/admin/user/create";
    String Update_User = "/admin/user/update";
    String Destroy_User = "/admin/user/destroy";
    String Create_Robot = "/admin/robot/create";
    String CreateOrUpdate_Device = "/admin/device/create";
    String Get_Device = "/admin/device/get";
    String Get_User_Devices = "/admin/device/user_devices";
    String User_Get_Token = "/admin/user/get_token";
    String User_Update_Block_Status = "/admin/user/update_block_status";
    String User_Get_Info = "/admin/user/get_info";
    String User_Get_Robot_Info = "/admin/user/get_robot_info";
    String User_Get_Blocked_List = "/admin/user/get_blocked_list";
    String User_Check_Block_Status = "/admin/user/check_block_status";
    String User_Get_Online_Status = "/admin/user/onlinestatus";
    String User_Put_Setting = "/admin/user/put_setting";
    String User_Get_Setting = "/admin/user/get_setting";
    String User_Kickoff_Client = "/admin/user/kickoff_client";
    String User_Online_Count = "/admin/user/online_count";
    String User_Online_List = "/admin/user/online_list";
    String User_Application_Get_UserInfo = "/admin/user/app_get_user_info";

    String Friend_Update_Status = "/admin/friend/status";
    String Friend_Get_List = "/admin/friend/list";
    String Blacklist_Update_Status = "/admin/blacklist/status";
    String Blacklist_Get_List = "/admin/blacklist/list";
    String Friend_Get_Alias = "/admin/friend/get_alias";
    String Friend_Set_Alias = "/admin/friend/set_alias";
    String Friend_Set_Extra = "/admin/friend/set_extra";
    String Friend_Send_Request = "/admin/friend/send_request";
    String Friend_Get_Requests = "/admin/friend/get_requests";
    String Relation_Get = "/admin/relation/get";

    String Msg_Send = "/admin/message/send";
    String Msg_Recall = "/admin/message/recall";
    String Msg_Delete = "/admin/message/delete";
    String Msg_Update = "/admin/message/update";
    String Msg_GetOne = "/admin/message/get_one";
    String Msg_Broadcast = "/admin/message/broadcast";
    String Msg_Multicast = "/admin/message/multicast";
    String Msg_RecallBroadCast = "/admin/message/recall_broadcast";
    String Msg_RecallMultiCast = "/admin/message/recall_multicast";
    String Msg_ConvRead = "/admin/message/conv_read";
    String Msg_Delivery = "/admin/message/delivery";

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
    String Group_Mute_Member = "/admin/group/manager/mute";
    String Group_Allow_Member = "/admin/group/manager/allow";
    String Get_User_Groups = "/admin/group/of_user";
    String Group_Set_Member_Alias = "/admin/group/member/set_alias";
    String Group_Set_Member_Extra = "/admin/group/member/set_extra";

    String Create_Channel = "/admin/channel/create";
    String Destroy_Channel = "/admin/channel/destroy";
    String Get_Channel_Info = "/admin/channel/get";
    String Subscribe_Channel = "/admin/channel/subscribe";
    String Get_System_Setting = "/admin/system/get_setting";
    String Put_System_Setting = "/admin/system/put_setting";

    String Health = "/admin/health";

    String Conference_List = "/admin/conference/list";
    String Conference_List_Participant = "/admin/conference/list_participant";
    String Conference_Create = "/admin/conference/create";
    String Conference_Destroy = "/admin/conference/destroy";

    String Channel_User_Info = "/channel/user_info";
    String Channel_Update_Profile = "/channel/update_profile";
    String Channel_Get_Profile = "/channel/get_profile";
    String Channel_Message_Send = "/channel/message/send";
    String Channel_Subscribe = "/channel/subscribe";
    String Channel_Subscriber_List = "/channel/subscriber_list";
    String Channel_Application_Get_UserInfo = "/channel/application/get_user_info";

    String Robot_User_Info = "/robot/user_info";
    String Robot_Get_Profile = "/robot/profile";
    String Robot_Message_Send = "/robot/message/send";
    String Robot_Set_Callback = "/robot/set_callback";
    String Robot_Get_Callback = "/robot/get_callback";
    String Robot_Delete_Callback = "/robot/delete_callback";
    String Robot_Update_Profile = "/robot/update_profile";
    String Robot_Application_Get_UserInfo = "/robot/application/get_user_info";
    String Robot_Group_Member_Add = "/robot/group/member/add";
    String Robot_Group_Allow_Member = "/robot/group/manager/allow";
    String Robot_Create_Group = "/robot/group/create";
    String Robot_Group_Dismiss = "/robot/group/del";
    String Robot_Group_Get_Info = "/robot/group/get_info";
    String Robot_Group_Member_List = "/robot/group/member/list";
    String Robot_Group_Member_Kickoff = "/robot/group/member/del";
    String Robot_Group_Modify_Info = "/robot/group/modify";
    String Robot_Group_Set_Member_Alias = "/robot/group/member/set_alias";
    String Robot_Group_Set_Member_Extra = "/robot/group/member/set_extra";
    String Robot_Group_Mute_Member = "/robot/group/manager/mute";
    String Robot_Group_Member_Quit = "/robot/group/member/quit";
    String Robot_Group_Transfer = "/robot/group/transfer";
    String Robot_Group_Set_Manager = "/robot/group/manager/set";
}
