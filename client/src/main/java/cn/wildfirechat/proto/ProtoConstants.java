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
        int Image = 2;
        int Voice = 3;
        int Location = 4;
        int Video = 5;
        int RichMedia = 6;
        int Custom = 7;
    }

    //ModifyGroupInfoRequest -> type
    public interface ModifyGroupInfoType {
        int Modify_Group_Name = 0;
        int Modify_Group_Portrait = 1;
        int Modify_Group_Extra = 2;
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
    public interface ChannelStatus {
        int Channel_Status_Public = 0;
        int Channel_Status_Private = 1;
        int Channel_Status_Destoryed = 2;
    }


    public interface UserType {
        int UserType_Normal = 0;
        int UserType_Robot = 1;
        int UserType_Device = 2;
    }
}

