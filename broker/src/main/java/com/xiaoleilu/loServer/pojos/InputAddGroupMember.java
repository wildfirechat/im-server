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

public class InputAddGroupMember extends InputGroupBase {
    private String group_id;
    private List<PojoGroupMember> members;

    public String getGroup_id() {
        return group_id;
    }

    public void setGroup_id(String group_id) {
        this.group_id = group_id;
    }

    public List<PojoGroupMember> getMembers() {
        return members;
    }

    public void setMembers(List<PojoGroupMember> members) {
        this.members = members;
    }

    public boolean isValide() {
        return true;
    }

    public WFCMessage.AddGroupMemberRequest toProtoGroupRequest() {
        WFCMessage.AddGroupMemberRequest.Builder addGroupBuilder = WFCMessage.AddGroupMemberRequest.newBuilder();
        addGroupBuilder.setGroupId(group_id);
        for (PojoGroupMember pojoGroupMember : getMembers()) {
            WFCMessage.GroupMember.Builder groupMemberBuilder = WFCMessage.GroupMember.newBuilder().setMemberId(pojoGroupMember.getMember_id());
            if (!StringUtil.isNullOrEmpty(pojoGroupMember.getAlias())) {
                groupMemberBuilder.setAlias(pojoGroupMember.getAlias());
            }
            groupMemberBuilder.setType(pojoGroupMember.getType());
            addGroupBuilder.addAddedMember(groupMemberBuilder);
        }

        for (Integer line : to_lines
             ) {
            addGroupBuilder.addToLine(line);
        }

        addGroupBuilder.setNotifyContent(notify_message.toProtoMessageContent());
        return addGroupBuilder.build();
    }

}
