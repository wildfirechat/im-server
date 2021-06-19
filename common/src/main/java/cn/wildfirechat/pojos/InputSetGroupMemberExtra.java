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

public class InputSetGroupMemberExtra extends InputGroupBase {
    private String group_id;
    private String memberId;
    private String extra;

    public String getGroup_id() {
        return group_id;
    }

    public void setGroup_id(String group_id) {
        this.group_id = group_id;
    }

    public boolean isValide() {
        return true;
    }

    public WFCMessage.ModifyGroupMemberExtra toProtoGroupRequest() {
        WFCMessage.ModifyGroupMemberExtra.Builder modifyAliasBuilder = WFCMessage.ModifyGroupMemberExtra.newBuilder();
        modifyAliasBuilder.setGroupId(group_id);
        modifyAliasBuilder.setMemberId(memberId);
        modifyAliasBuilder.setExtra(StringUtil.isNullOrEmpty(extra)?"": extra);

        if (to_lines != null) {
            for (Integer line : to_lines) {
                modifyAliasBuilder.addToLine(line);
            }
        }

        if (notify_message != null) {
            modifyAliasBuilder.setNotifyContent(notify_message.toProtoMessageContent());
        }
        return modifyAliasBuilder.build();
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}
