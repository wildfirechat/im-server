package cn.wildfirechat.common;

public interface APIPath {
    String Create_Chatroom = "/admin/chatroom/create";
    String Create_Group = "/admin/group/create";
    String Create_Robot = "/admin/robot/create";
    String Create_User = "/admin/user/create";

    String Chatroom_Destroy = "/admin/chatroom/del";
    String Chatroom_Info = "/admin/chatroom/info";


    String Sensitive_Add = "/admin/sensitive/add";
    String Sensitive_Del = "/admin/sensitive/del";
    String Sensitive_Query = "/admin/sensitive/query";

    String Get_User_Token = "/admin/user/token";
    String User_Status = "/admin/user/status";
    String Get_User_Info = "/admin/user/info";
    String User_Status_List = "/admin/user/statuslist";
    String User_Check_Status = "/admin/user/checkstatus";
    String User_Online_Status = "/admin/user/onlinestatus";


    String Friend_Status = "/admin/friend/status";
    String Friend_List = "/admin/friend/list";

    String Msg_Send = "/admin/message/send";
    String Msg_Recall = "/admin/message/recall";

    String Group_Dismiss = "/admin/group/del";
    String Group_Member_Add = "/admin/group/member/add";
    String Group_Member_Kickoff = "/admin/group/member/del";
    String Group_Owner = "/admin/group/owner";


    String Channel_Message_Send = "/channel/message/send";

    String Robot_User_Info = "/robot/user_info";
    String Robot_Message_Send = "/robot/message/send";
}
