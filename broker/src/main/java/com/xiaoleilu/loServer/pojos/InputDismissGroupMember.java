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

public class InputDismissGroupMember extends InputGroupBase {
    private String group_id;


    public String getGroup_id() {
        return group_id;
    }

    public void setGroup_id(String group_id) {
        this.group_id = group_id;
    }

    public boolean isValide() {
        return true;
    }

    public WFCMessage.DismissGroupRequest toProtoGroupRequest() {
        WFCMessage.DismissGroupRequest.Builder dismissGroupBuilder = WFCMessage.DismissGroupRequest.newBuilder();
        dismissGroupBuilder.setGroupId(group_id);

        for (Integer line : to_lines
             ) {
            dismissGroupBuilder.addToLine(line);
        }

        dismissGroupBuilder.setNotifyContent(notify_message.toProtoMessageContent());
        return dismissGroupBuilder.build();
    }

}
