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

public class InputKickoffGroupMember extends InputGroupBase {
    private String group_id;
    private List<String> members;

    public String getGroup_id() {
        return group_id;
    }

    public void setGroup_id(String group_id) {
        this.group_id = group_id;
    }

    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public boolean isValide() {
        if (StringUtil.isNullOrEmpty(group_id) || StringUtil.isNullOrEmpty(operator) || members == null || members.isEmpty())
            return false;
        return true;
    }

    public WFCMessage.RemoveGroupMemberRequest toProtoGroupRequest() {
        WFCMessage.RemoveGroupMemberRequest.Builder removedGroupBuilder = WFCMessage.RemoveGroupMemberRequest.newBuilder();
        removedGroupBuilder.setGroupId(group_id);
        removedGroupBuilder.addAllRemovedMember(members);

        if (to_lines != null) {
            for (Integer line : to_lines
            ) {
                removedGroupBuilder.addToLine(line);
            }
        }

        if (notify_message != null) {
            removedGroupBuilder.setNotifyContent(notify_message.toProtoMessageContent());
        }
        return removedGroupBuilder.build();
    }

}
