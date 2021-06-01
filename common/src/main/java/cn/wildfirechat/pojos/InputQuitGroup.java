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

public class InputQuitGroup extends InputGroupBase {
    private String group_id;

    public String getGroup_id() {
        return group_id;
    }

    public void setGroup_id(String group_id) {
        this.group_id = group_id;
    }

    public boolean isValide() {
        if (StringUtil.isNullOrEmpty(group_id) || StringUtil.isNullOrEmpty(operator))
            return false;
        return true;
    }

    public WFCMessage.QuitGroupRequest toProtoGroupRequest() {
        WFCMessage.QuitGroupRequest.Builder removedGroupBuilder = WFCMessage.QuitGroupRequest.newBuilder();
        removedGroupBuilder.setGroupId(group_id);

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
