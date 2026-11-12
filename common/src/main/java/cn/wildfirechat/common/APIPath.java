package cn.wildfirechat.common;

/**
 * API路径接口
 * <p>
 * 定义了野火IM服务器所有管理接口的API路径。
 * 包括聊天室、敏感词、域、用户、好友、黑名单、消息、群组、会议、频道等模块的接口路径。
 * </p>
 */
public interface APIPath {
    String Create_Chatroom = "/admin/chatroom/create";
    String Chatroom_Destroy = "/admin/chatroom/del";
    String Chatroom_Info = "/admin/chatroom/info";
    String Chatroom_GetMembers = "/admin/chatroom/members";
    String Chatroom_GetUserChatroom = "/admin/chatroom/user_chatroom";
    String Chatroom_SetBlacklist = "/admin/chatroom/set_black_status";
    String Chatroom_GetBlacklist = "/admin/chatroom/get_black_status";
    String Chatroom_SetManager = "/admin/chatroom/set_manager";
    String Chatroom_GetManagerList = "/admin/chatroom/get_manager_list";
    String Chatroom_MuteAll = "/admin/chatroom/mute_all";


    String Sensitive_Add = "/admin/sensitive/add";
    String Sensitive_Del = "/admin/sensitive/del";
    String Sensitive_Query = "/admin/sensitive/query";

    String Create_Domain = "/admin/domain/create";
    String Get_Domain = "/admin/domain/get";
    String Destroy_Domain = "/admin/domain/destroy";
    String List_Domain = "/admin/domain/list";
    String Ping_Domain = "/admin/domain/ping";

    String Create_User = "/admin/user/create";
    String Update_User = "/admin/user/update";
    String Destroy_User = "/admin/user/destroy";
    String Search_User = "/admin/user/search_user";
    String User_Search_By_Name = "/admin/user/search_by_name";
    String Create_Robot = "/admin/robot/create";
    String CreateOrUpdate_Device = "/admin/device/create";
    String Get_Device = "/admin/device/get";
    String Get_User_Devices = "/admin/device/user_devices";
    String User_Get_Token = "/admin/user/get_token";
    String User_Update_Block_Status = "/admin/user/update_block_status";
    String User_Get_Info = "/admin/user/get_info";
    String User_Batch_Get_Infos = "/admin/user/batch_get_infos";
    String User_Get_Email_Info = "/admin/user/get_info_by_email";
    String User_Get_By_Mobile = "/admin/user/get_info_by_mobile";
    String User_Get_All = "/admin/user/all";
    String User_Get_Robot_Info = "/admin/user/get_robot_info";
    String User_Get_All_Robots = "/admin/user/get_all_robots";
    String User_Get_User_Robots = "/admin/user/get_user_robots";
    String User_Get_Blocked_List = "/admin/user/get_blocked_list";
    String User_Check_Block_Status = "/admin/user/check_block_status";
    String User_Get_Online_Status = "/admin/user/onlinestatus";
    String User_Put_Setting = "/admin/user/put_setting";
    String User_Get_Setting = "/admin/user/get_setting";
    String User_Kickoff_Client = "/admin/user/kickoff_client";
    String User_Online_Count = "/admin/user/online_count";
    String User_Online_List = "/admin/user/online_list";
    String User_Session_List = "/admin/user/session_list";
    String User_Unread_count = "/admin/user/unread_count";
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

    String Handle_Friend_Send_Request = "/admin/friend/handle_send_request";

    String Admin_Moments_Post_Feed = "/admin/moments/feed/post";

    String Msg_Send = "/admin/message/send";
    String Msg_Publish = "/admin/message/publish";
    String Msg_Recall = "/admin/message/recall";
    String Msg_Delete = "/admin/message/delete";
    String Msg_Update = "/admin/message/update";
    String Msg_GetOne = "/admin/message/get_one";
    String Msg_Broadcast = "/admin/message/broadcast";
    String Msg_Multicast = "/admin/message/multicast";
    String Msg_RecallBroadCast = "/admin/message/recall_broadcast";
    String Msg_RecallMultiCast = "/admin/message/recall_multicast";
    String Msg_DeleteBroadCast = "/admin/message/delete_broadcast";
    String Msg_DeleteMultiCast = "/admin/message/delete_multicast";
    String Msg_ConvRead = "/admin/message/conv_read";
    String Msg_Delivery = "/admin/message/delivery";
    String Conversation_Delete = "/admin/conversation/delete";
    String Msg_Clear_By_User = "/admin/message/clear_by_user";
    String Msg_Import = "/admin/message/import";

    String Create_Group = "/admin/group/create";
    String Group_Dismiss = "/admin/group/del";
    String Group_Transfer = "/admin/group/transfer";
    String Group_Get_Info = "/admin/group/get_info";
    String Group_Batch_Info = "/admin/group/batch_infos";
    String Group_Search = "/admin/group/search";
    String Group_Modify_Info = "/admin/group/modify";
    String Group_Member_List = "/admin/group/member/list";
    String Group_Member_Get = "/admin/group/member/get";
    String Group_Member_Add = "/admin/group/member/add";
    String Group_Member_Kickoff = "/admin/group/member/del";
    String Group_Member_Quit = "/admin/group/member/quit";
    String Group_Set_Manager = "/admin/group/manager/set";
    String Group_Mute_Member = "/admin/group/manager/mute";
    String Group_Allow_Member = "/admin/group/manager/allow";
    String Group_Join_Request_Add = "/admin/group/join_request/add";
    String Get_User_Groups = "/admin/group/of_user";
    String Get_User_Groups_By_Type = "/admin/group/of_user_by_type";
    String Group_Set_Member_Alias = "/admin/group/member/set_alias";
    String Group_Set_Member_Extra = "/admin/group/member/set_extra";
    String Get_Common_Groups = "/admin/group/common_group";

    String Sync_Group = "/admin/mesh/group_sync";
    String Conference_User_Request = "/admin/conference/user_request";
    String Conference_User_Event = "/admin/conference/user_event";

    String Create_Channel = "/admin/channel/create";
    String Destroy_Channel = "/admin/channel/destroy";
    String Get_Channel_Info = "/admin/channel/get";
    String List_Channel_Info = "/admin/channel/list";
    String Subscribe_Channel = "/admin/channel/subscribe";
    String Batch_Subscribe_Channel = "/admin/channel/batch_subscribe";
    String Check_User_Subscribe_Channel = "/admin/channel/is_subscribed";
    String List_Channel_Subscriber = "/admin/channel/list_subscriber";
    String Get_System_Setting = "/admin/system/get_setting";
    String Put_System_Setting = "/admin/system/put_setting";
    String GET_CUSTOMER = "/admin/customer";
    String Health = "/admin/health";
    String Get_Conversation_Files = "/admin/file/conversation_files";
    String Get_User_Files = "/admin/file/user_files";
    String Get_Message_File = "/admin/file/message_file";

    String Get_Presigned_Upload_Url = "/admin/oss/get_upload_url";

    String Conference_List = "/admin/conference/list";
    String Conference_Exist = "/admin/conference/exist";
    String Conference_List_Participant = "/admin/conference/list_participant";
    String Conference_Create = "/admin/conference/create";
    String Conference_Destroy = "/admin/conference/destroy";
    String Conference_Recording = "/admin/conference/recording";
    String Conference_Rtp_Forward = "/admin/conference/rtp_forward";
    String Conference_Stop_Rtp_Forward = "/admin/conference/stop_rtp_forward";
    String Conference_List_Rtp_Forward = "/admin/conference/list_rtp_forward";

    String Channel_User_Info = "/channel/user_info";
    String Channel_Update_Profile = "/channel/update_profile";
    String Channel_Get_Profile = "/channel/get_profile";
    String Channel_Message_Send = "/channel/message/send";
    String Channel_Msg_Recall = "/channel/message/recall";
    String Channel_Msg_Republish = "/channel/message/republish";
    String Channel_Subscribe = "/channel/subscribe";
    String Channel_Subscriber_List = "/channel/subscriber_list";
    String Channel_Is_Subscriber = "/channel/is_subscriber";
    String Channel_Application_Get_UserInfo = "/channel/application/get_user_info";

    String Robot_User_Info = "/robot/user_info";
    String Robot_User_Get_Email_Info = "/robot/user/get_info_by_email";
    String Robot_User_Batch_Get_Infos = "/robot/user/batch_get_infos";
    String Robot_Friend_Get_List = "/robot/friend/list";
    String Robot_Search_User = "/robot/user/search";
    String Robot_Get_User_Robots = "/robot/user/get_user_robots";
    String Robot_Get_Profile = "/robot/profile";
    String Robot_Message_Send = "/robot/message/send";
    String Robot_Message_Reply = "/robot/message/reply";
    String Robot_Message_Recall = "/robot/message/recall";
    String Robot_Message_Update = "/robot/message/update";
    String Robot_Get_Message = "/robot/message/get_one";
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
    String Robot_Group_Member_Get = "/robot/group/member/get";
    String Robot_Group_Member_Kickoff = "/robot/group/member/del";
    String Robot_Group_Modify_Info = "/robot/group/modify";
    String Robot_Group_Set_Member_Alias = "/robot/group/member/set_alias";
    String Robot_Group_Set_Member_Extra = "/robot/group/member/set_extra";
    String Robot_Group_Mute_Member = "/robot/group/manager/mute";
    String Robot_Group_Member_Quit = "/robot/group/member/quit";
    String Robot_Group_Transfer = "/robot/group/transfer";
    String Robot_Group_Set_Manager = "/robot/group/manager/set";
    String Robot_Conversation_Set_User_Setting = "/robot/conversation/user_setting";

    String Robot_Moments_Post_Feed = "/robot/moments/feed/post";
    String Robot_Moments_Pull_Feeds = "/robot/moments/feed/pull";
    String Robot_Moments_Update_Feed = "/robot/moments/feed/update";
    String Robot_Moments_Post_Comment = "/robot/moments/comment/post";
    String Robot_Moments_Pull_Comment = "/robot/moments/comment/pull";
    String Robot_Moments_Fetch_Feed = "/robot/moments/feed/pull_one";
    String Robot_Moments_Fetch_Profiles = "/robot/moments/profiles/pull";
    String Robot_Moments_Recall_Comment = "/robot/moments/comment/recall";
    String Robot_Moments_Recall_Feed = "/robot/moments/feed/recall";
    String Robot_Moments_Update_Profiles_List_Value = "/robot/moments/profiles/list/push";
    String Robot_Moments_Update_Profiles_Value = "/robot/moments/profiles/value/push";

    String Robot_Conference_Request = "/robot/conference/request";

    String Robot_Get_Presigned_Upload_Url = "/robot/oss/get_upload_url";

    String Robot_Set_Online = "/robot/set_online";
}
