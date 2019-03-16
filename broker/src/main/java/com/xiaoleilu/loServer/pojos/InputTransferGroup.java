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

public class InputTransferGroup extends InputGroupBase {
    private String group_id;
    private String new_owner;


    public boolean isValide() {
        return true;
    }

    public WFCMessage.TransferGroupRequest toProtoGroupRequest() {
        WFCMessage.TransferGroupRequest.Builder groupBuilder = WFCMessage.TransferGroupRequest.newBuilder();

        groupBuilder.setGroupId(group_id);
        groupBuilder.setNewOwner(new_owner);
        for (Integer line : to_lines
             ) {
            groupBuilder.addToLine(line);
        }

        groupBuilder.setNotifyContent(notify_message.toProtoMessageContent());
        return groupBuilder.build();
    }

    public String getGroup_id() {
        return group_id;
    }

    public void setGroup_id(String group_id) {
        this.group_id = group_id;
    }

    public String getNew_owner() {
        return new_owner;
    }

    public void setNew_owner(String new_owner) {
        this.new_owner = new_owner;
    }
}
