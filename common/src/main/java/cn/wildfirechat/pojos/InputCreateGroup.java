/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;


import cn.wildfirechat.proto.WFCMessage;
import io.netty.util.internal.StringUtil;

import java.util.List;

public class InputCreateGroup extends InputGroupBase {
    private PojoGroup group;

    public boolean isValide() {
        return true;
    }

    public WFCMessage.CreateGroupRequest toProtoGroupRequest() {
        WFCMessage.Group.Builder groupBuilder = WFCMessage.Group.newBuilder();
        WFCMessage.GroupInfo.Builder groupInfoBuilder = WFCMessage.GroupInfo.newBuilder();
        PojoGroupInfo group_info = group.getGroup_info();
        if (!StringUtil.isNullOrEmpty(group_info.target_id)) {
            groupInfoBuilder.setTargetId(group_info.getTarget_id());
        }

        if (!StringUtil.isNullOrEmpty(group_info.name)) {
            groupInfoBuilder.setName(group_info.getName());
        }

        if (!StringUtil.isNullOrEmpty(group_info.portrait)) {
            groupInfoBuilder.setPortrait(group_info.getPortrait());
        }
        if (!StringUtil.isNullOrEmpty(group_info.owner)) {
            groupInfoBuilder.setOwner(group_info.getOwner());
        }

        groupInfoBuilder.setType(group_info.getType());

        if (!StringUtil.isNullOrEmpty(group_info.extra)) {
            groupInfoBuilder.setExtra(group_info.getExtra());
        }

        if (group_info.join_type > 0 && group_info.join_type < 128) {
            groupInfoBuilder.setJoinType(group_info.join_type);
        }

        if (group_info.mute > 0 && group_info.mute < 128) {
            groupInfoBuilder.setMute(group_info.mute);
        }
        if (group_info.private_chat > 0 && group_info.private_chat < 128) {
            groupInfoBuilder.setPrivateChat(group_info.private_chat);
        }
        if (group_info.searchable > 0 && group_info.searchable < 128) {
            groupInfoBuilder.setSearchable(group_info.searchable);
        }
        if(group_info.history_message > 0&& group_info.history_message < 128) {
            groupInfoBuilder.setHistoryMessage(group_info.history_message);
        }
        if(group_info.max_member_count > 0) {
            groupInfoBuilder.setMaxMemberCount(group_info.max_member_count);
        }

        groupBuilder.setGroupInfo(groupInfoBuilder);
        if(group.getMembers() != null) {
            for (PojoGroupMember pojoGroupMember : group.getMembers()) {
                WFCMessage.GroupMember.Builder groupMemberBuilder = WFCMessage.GroupMember.newBuilder().setMemberId(pojoGroupMember.getMember_id());
                if (!StringUtil.isNullOrEmpty(pojoGroupMember.getAlias())) {
                    groupMemberBuilder.setAlias(pojoGroupMember.getAlias());
                }
                groupMemberBuilder.setType(pojoGroupMember.getType());
                if (!StringUtil.isNullOrEmpty(pojoGroupMember.getExtra())) {
                    groupMemberBuilder.setExtra(pojoGroupMember.extra);
                }
                groupBuilder.addMembers(groupMemberBuilder);
            }
        }

        WFCMessage.CreateGroupRequest.Builder createGroupReqBuilder = WFCMessage.CreateGroupRequest.newBuilder();
        createGroupReqBuilder.setGroup(groupBuilder);
        if (to_lines != null) {
            for (Integer line : to_lines
            ) {
                createGroupReqBuilder.addToLine(line);
            }
        }

        if (notify_message != null) {
            createGroupReqBuilder.setNotifyContent(notify_message.toProtoMessageContent());
        }
        return createGroupReqBuilder.build();
    }

    public PojoGroup getGroup() {
        return group;
    }

    public void setGroup(PojoGroup group) {
        this.group = group;
    }
}
