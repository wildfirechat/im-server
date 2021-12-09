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

import java.util.Arrays;
import java.util.List;

public class InputModifyGroupInfo extends InputGroupBase {
    private String group_id;
    //ModifyGroupInfoType
    private int type;
    private String value;

    public boolean isValide() {
        if (StringUtil.isNullOrEmpty(group_id) || StringUtil.isNullOrEmpty(operator))
            return false;
        return true;
    }
    public String getGroup_id() {
        return group_id;
    }

    public void setGroup_id(String group_id) {
        this.group_id = group_id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public WFCMessage.ModifyGroupInfoRequest toProtoGroupRequest() {
        if(notify_message != null && notify_message.getType() > 0) {
            return WFCMessage.ModifyGroupInfoRequest.newBuilder()
                .setGroupId(group_id)
                .setType(type)
                .setValue(value == null ? "" : value)
                .addAllToLine(to_lines == null || to_lines.isEmpty() ? Arrays.asList(0) : to_lines)
                .setNotifyContent(notify_message.toProtoMessageContent())
                .build();
        } else {
            return WFCMessage.ModifyGroupInfoRequest.newBuilder()
                .setGroupId(group_id)
                .setType(type)
                .setValue(value == null ? "" : value)
                .addAllToLine(to_lines == null || to_lines.isEmpty() ? Arrays.asList(0) : to_lines)
                .build();
        }
    }
}
