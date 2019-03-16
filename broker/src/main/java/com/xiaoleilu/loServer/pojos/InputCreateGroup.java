/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.pojos;


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
        if (!StringUtil.isNullOrEmpty(group.getGroup_info().target_id)) {
            groupInfoBuilder.setTargetId(group.getGroup_info().getTarget_id());
        }

        if (!StringUtil.isNullOrEmpty(group.getGroup_info().name)) {
            groupInfoBuilder.setName(group.getGroup_info().getName());
        }

        if (!StringUtil.isNullOrEmpty(group.getGroup_info().portrait)) {
            groupInfoBuilder.setPortrait(group.getGroup_info().getPortrait());
        }
        if (!StringUtil.isNullOrEmpty(group.getGroup_info().owner)) {
            groupInfoBuilder.setOwner(group.getGroup_info().getOwner());
        }

            groupInfoBuilder.setType(group.getGroup_info().getType());

        if (!StringUtil.isNullOrEmpty(group.getGroup_info().extra)) {
            groupInfoBuilder.setExtra(group.getGroup_info().getExtra());
        }


        groupBuilder.setGroupInfo(groupInfoBuilder);
        for (PojoGroupMember pojoGroupMember : group.getMembers()) {
            WFCMessage.GroupMember.Builder groupMemberBuilder = WFCMessage.GroupMember.newBuilder().setMemberId(pojoGroupMember.getMember_id());
            if (!StringUtil.isNullOrEmpty(pojoGroupMember.getAlias())) {
                groupMemberBuilder.setAlias(pojoGroupMember.getAlias());
            }
            groupMemberBuilder.setType(pojoGroupMember.getType());
            groupBuilder.addMembers(groupMemberBuilder);
        }

        WFCMessage.CreateGroupRequest.Builder createGroupReqBuilder = WFCMessage.CreateGroupRequest.newBuilder();
        createGroupReqBuilder.setGroup(groupBuilder);
        for (Integer line : to_lines
             ) {
            createGroupReqBuilder.addToLine(line);
        }

        createGroupReqBuilder.setNotifyContent(notify_message.toProtoMessageContent());
        return createGroupReqBuilder.build();
    }

    public PojoGroup getGroup() {
        return group;
    }

    public void setGroup(PojoGroup group) {
        this.group = group;
    }
}
