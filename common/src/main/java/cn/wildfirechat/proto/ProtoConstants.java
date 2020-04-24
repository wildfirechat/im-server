package cn.wildfirechat.proto;

public class ProtoConstants {

    //message Conversation -> type
    public interface ConversationType {
        int ConversationType_Private = 0;
        int ConversationType_Group = 1;
        int ConversationType_ChatRoom = 2;
        int ConversationType_Channel = 3;
        int ConversationType_Thing = 4;
    }

    //message GroupInfo -> type
    public interface GroupType {
        //member can add quit change group name and portrait, owner can do all the operations
        int GroupType_Normal = 0;
        //every member can add quit change group name and portrait, no one can kickoff others
        int GroupType_Free = 1;
        //member can only quit, owner can do all the operations
        int GroupType_Restricted = 2;
    }

    //message GroupMember -> type
    public interface GroupMemberType {
        int GroupMemberType_Normal = 0;
        int GroupMemberType_Manager = 1;
        int GroupMemberType_Owner = 2;
        int GroupMemberType_Silent = 3;
        int GroupMemberType_Removed = 4;
    }

    //message FriendRequest -> status
    public interface FriendRequestStatus {
        int RequestStatus_Sent = 0;
        int RequestStatus_Accepted = 1;
        int RequestStatus_Rejected = 2;
    }

    //message UploadDeviceTokenRequest -> platform
    public interface Platform {
        int Platform_UNSET = 0;
        int Platform_iOS = 1;
        int Platform_Android = 2;
        int Platform_Windows = 3;
        int Platform_OSX = 4;
        int Platform_WEB = 5;
        int Platform_WX = 6;
        int Platform_LINUX = 7;
    }

    //message NotifyMessage & PullMessageRequest -> type
    public interface PullType {
        int Pull_Normal = 0;
        int Pull_ChatRoom = 1;
        int Pull_Group = 2;
    }

    //message UserResult -> code
    public interface UserResultCode {
        int Success = 0;
        int NotFound = 1;
        int NotModified = 2;
    }

    //message ChatroomInfo -> state
    public interface ChatroomState {
        int Chatroom_State_Normal = 0;
        int Chatroom_State_NotStart = 1;
        int Chatroom_State_End = 2;
    }


    //message MessageContent -> contentType
    public interface ContentType {
        int Unknown = 0;
        int Text = 1;
        int Voice = 2;
        int Image = 3;
        int Location = 4;
        int File = 5;
        int Video = 6;
        int Sticker = 7;
        int RichMedia = 8;
    }

    //ModifyGroupInfoRequest -> type
    public interface ModifyGroupInfoType {
        int Modify_Group_Name = 0;
        int Modify_Group_Portrait = 1;
        int Modify_Group_Extra = 2;
        int Modify_Group_Mute = 3;
        int Modify_Group_JoinType = 4;
        int Modify_Group_PrivateChat = 5;
        int Modify_Group_Searchable = 6;
    }

    //ModifyGroupInfoRequest -> type
    public interface PersistFlag {
        int Not_Persist = 0;
        int Persist = 1;
        int Persist_And_Count = 3;
        int Transparent = 4;
    }


    //ModifyChannelInfoRequest -> type
    public interface ModifyChannelInfoType {
        int Modify_Channel_Name = 0;
        int Modify_Channel_Portrait = 1;
        int Modify_Channel_Desc = 2;
        int Modify_Channel_Extra = 3;
        int Modify_Channel_Secret = 4;
        int Modify_Channel_Callback = 5;
        int Modify_Channel_OnlyCallback = 6;
    }

    //Channel -> status
    //第0位表示是否允许查看用户所有信息，还是只允许看用户id，用户名称，用户昵称和用户头像
    //第1位表示是否允许查看非订阅用户信息
    //第2位表示是否允许主动添加用户订阅关系
    //第3位表示是否允许给非订阅用户发送消息
    //第4位表示是否私有
    //第6位表示是否删除
    public interface ChannelState {
        int Channel_State_Mask_FullInfo = 0x01;
        int Channel_State_Mask_Unsubscribed_User_Access = 0x02;
        int Channel_State_Mask_Active_Subscribe = 0x04;
        int Channel_State_Mask_Message_Unsubscribed = 0x08;
        int Channel_State_Mask_Private = 0x10;
        int Channel_State_Mask_Deleted = 0x40;
    }


    public interface UserType {
        int UserType_Normal = 0;
        int UserType_Robot = 1;
        int UserType_Device = 2;
        int UserType_Admin = 3;
        int UserType_Super_Admin = 100;
    }


    public interface SystemSettingType {
        int Group_Max_Member_Count = 1;
    }

    public interface SearchUserType {
        int SearchUserType_General = 0;
        int SearchUserType_Name_Mobile = 1;
        int SearchUserType_Name = 2;
        int SearchUserType_Mobile = 3;
    }

    public interface UserStatus {
        int Normal = 0;
        int Muted = 1;
        int Forbidden = 2;
    }

    public interface BlacklistStrategy {
        int Message_Reject = 0;
        int Message_Ignore = 1;
    }



    public static final int MESSAGE_CONTENT_TYPE_CREATE_GROUP = 104;
    public static final int MESSAGE_CONTENT_TYPE_ADD_GROUP_MEMBER = 105;
    public static final int MESSAGE_CONTENT_TYPE_KICKOF_GROUP_MEMBER = 106;
    public static final int MESSAGE_CONTENT_TYPE_QUIT_GROUP = 107;
    public static final int MESSAGE_CONTENT_TYPE_DISMISS_GROUP = 108;
    public static final int MESSAGE_CONTENT_TYPE_TRANSFER_GROUP_OWNER = 109;
    public static final int MESSAGE_CONTENT_TYPE_CHANGE_GROUP_NAME = 110;
    public static final int MESSAGE_CONTENT_TYPE_MODIFY_GROUP_ALIAS = 111;
    public static final int MESSAGE_CONTENT_TYPE_CHANGE_GROUP_PORTRAIT = 112;

    public static final int MESSAGE_CONTENT_TYPE_CHANGE_MUTE = 113;
    public static final int MESSAGE_CONTENT_TYPE_CHANGE_JOINTYPE = 114;
    public static final int MESSAGE_CONTENT_TYPE_CHANGE_PRIVATECHAT = 115;
    public static final int MESSAGE_CONTENT_TYPE_CHANGE_SEARCHABLE = 116;

    public static final int MESSAGE_CONTENT_TYPE_SET_MANAGER = 117;
}

