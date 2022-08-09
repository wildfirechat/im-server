/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;

import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;

import java.util.List;

public class GroupNotificationBinaryContent {
    //groupId
    private String g;

    //operator
    private String o;

    //value1(name or something)
    private String n;

    //members
    private List<String> ms;

    //value2(member or something)
    private String m;

    public GroupNotificationBinaryContent() {
    }

    public GroupNotificationBinaryContent(String g, String o, String n, String m) {
        this.g = g;
        this.o = o;
        this.n = n;
        this.m = m;
    }

    public GroupNotificationBinaryContent(String g, String operator, String name, List<String> members) {
        this.g = g;
        this.o = operator;
        this.n = name;
        this.ms = members;
    }

    public String getG() {
        return g;
    }

    public void setG(String g) {
        this.g = g;
    }

    public String getM() {
        return m;
    }

    public void setM(String m) {
        this.m = m;
    }

    public String getO() {
        return o;
    }

    public void setO(String o) {
        this.o = o;
    }

    public String getN() {
        return n;
    }

    public void setN(String n) {
        this.n = n;
    }

    public List<String> getMs() {
        return ms;
    }

    public void setMs(List<String> ms) {
        this.ms = ms;
    }

    public WFCMessage.MessageContent getGroupNotifyContent(int groupContentType) {
        return WFCMessage.MessageContent.newBuilder().setType(groupContentType).setData(ByteString.copyFromUtf8(new Gson().toJson(this))).build();
    }

    public WFCMessage.MessageContent getAddGroupNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_ADD_GROUP_MEMBER);
    }

    public WFCMessage.MessageContent getCreateGroupNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_CREATE_GROUP);
    }

    public WFCMessage.MessageContent getDismissGroupNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_DISMISS_GROUP);
    }

    public WFCMessage.MessageContent getKickokfMemberGroupNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_KICKOF_GROUP_MEMBER);
    }

    public WFCMessage.MessageContent getKickokfMemberVisibleGroupNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_KICKOF_GROUP_MEMBER_VISIBLE);
    }

    public WFCMessage.MessageContent getQuitGroupNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_QUIT_GROUP);
    }

    public WFCMessage.MessageContent getQuitVisibleGroupNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_QUIT_GROUP_VISIBLE);
    }

    public WFCMessage.MessageContent getTransferGroupNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_TRANSFER_GROUP_OWNER);
    }

    public WFCMessage.MessageContent getChangeGroupNameNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_CHANGE_GROUP_NAME);
    }

    public WFCMessage.MessageContent getChangeGroupPortraitNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_CHANGE_GROUP_PORTRAIT);
    }

    public WFCMessage.MessageContent getModifyGroupMemberAliasNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_MODIFY_GROUP_ALIAS);
    }

    public WFCMessage.MessageContent getModifyGroupMemberExtraNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_MODIFY_GROUP_MEMBER_EXTRA);
    }

    public WFCMessage.MessageContent getChangeGroupMuteNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_CHANGE_MUTE);
    }

    public WFCMessage.MessageContent getChangeGroupJointypeNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_CHANGE_JOINTYPE);
    }

    public WFCMessage.MessageContent getChangeGroupPrivatechatNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_CHANGE_PRIVATECHAT);
    }

    public WFCMessage.MessageContent getChangeGroupSearchableNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_CHANGE_SEARCHABLE);
    }

    public WFCMessage.MessageContent getSetGroupManagerNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_SET_MANAGER);
    }

    public WFCMessage.MessageContent getMuteGroupMemberNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_MUTE_MEMBER);
    }

    public WFCMessage.MessageContent getAllowGroupMemberNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_ALLOW_MEMBER);
    }

    public WFCMessage.MessageContent getChangeGroupExtraNotifyContent() {
        return getGroupNotifyContent(ProtoConstants.MESSAGE_CONTENT_TYPE_MODIFY_GROUP_EXTRA);
    }
}
