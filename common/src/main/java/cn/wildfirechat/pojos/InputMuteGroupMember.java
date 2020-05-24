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

public class InputMuteGroupMember extends InputGroupBase {
    private String group_id;
    private List<String> members;
    private boolean is_manager;

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

    public boolean isIs_manager() {
        return is_manager;
    }

    public void setIs_manager(boolean is_manager) {
        this.is_manager = is_manager;
    }

    public boolean isValide() {
        if (StringUtil.isNullOrEmpty(operator) || StringUtil.isNullOrEmpty(group_id) || members == null || members.isEmpty()) {
            return false;
        }
        return true;
    }

    public WFCMessage.SetGroupManagerRequest toProtoGroupRequest() {
        WFCMessage.SetGroupManagerRequest.Builder addGroupBuilder = WFCMessage.SetGroupManagerRequest.newBuilder();
        addGroupBuilder.setGroupId(group_id);
        addGroupBuilder.addAllUserId(members);
        addGroupBuilder.setType(is_manager ? 1 : 0);

        if (to_lines != null) {
            for (Integer line : to_lines
            ) {
                addGroupBuilder.addToLine(line);
            }
        }

        if (notify_message != null) {
            addGroupBuilder.setNotifyContent(notify_message.toProtoMessageContent());
        }
        return addGroupBuilder.build();
    }

}
