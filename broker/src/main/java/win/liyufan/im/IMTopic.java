/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package win.liyufan.im;

public interface IMTopic {
	String SendMessageTopic = "MS";
    String RecallMessageTopic = "MR";
	String PullMessageTopic = "MP";
	String NotifyMessageTopic = "MN";
    String NotifyRecallMessageTopic = "RMN";

    String GetUserSettingTopic = "UG";
    String PutUserSettingTopic = "UP";
    String NotifyUserSettingTopic = "UN";

    String CreateGroupTopic = "GC";
	String AddGroupMemberTopic = "GAM";
	String KickoffGroupMemberTopic = "GKM";
	String QuitGroupTopic = "GQ";
	String DismissGroupTopic = "GD";
	String ModifyGroupInfoTopic = "GMI";
    String ModifyGroupAliasTopic = "GMA";
    String GetGroupInfoTopic = "GPGI";
    String GetGroupMemberTopic = "GPGM";
    String TransferGroupTopic = "GTG";


    String GetUserInfoTopic = "UPUI";
    String ModifyMyInfoTopic = "MMI";

	String GetQiniuUploadTokenTopic = "GQNUT";

    String AddFriendRequestTopic = "FAR";
    String HandleFriendRequestTopic = "FHR";
    String FriendRequestPullTopic = "FRP";
    String NotifyFriendRequestTopic = "FRN";
    String RriendRequestUnreadSyncTopic = "FRUS";

    String DeleteFriendTopic = "FDL";
    String FriendPullTopic = "FP";
    String NotifyFriendTopic = "FN";
    String BlackListUserTopic = "BLU";
    String SetFriendAliasTopic = "FALS";


    String UploadDeviceTokenTopic = "UDT";

    String UserSearchTopic = "US";

    String JoinChatroomTopic = "CRJ";
    String QuitChatroomTopic = "CRQ";
    String GetChatroomInfoTopic = "CRI";
    String GetChatroomMemberTopic = "CRMI";

    String RouteTopic = "ROUTE";

    String CreateChannelTopic = "CHC";
    String ModifyChannelInfoTopic = "CHMI";
    String TransferChannelInfoTopic = "CHT";
    String DestoryChannelInfoTopic = "CHD";
    String ChannelSearchTopic = "CHS";
    String ChannelListenTopic = "CHL";
    String ChannelPullTopic = "CHP";

    String GetTokenTopic = "GETTOKEN";

    String LoadRemoteMessagesTopic = "LRM";
}
