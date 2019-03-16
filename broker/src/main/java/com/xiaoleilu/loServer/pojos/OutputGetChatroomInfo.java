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

public class OutputGetChatroomInfo {
    private String chatroomId;
    private String title;
    private String desc;
    private String portrait;
    private String extra;
    private int memberCount;
    private long createDt;
    private long updateDt;

    public OutputGetChatroomInfo(String chatroomId, int memberCount, WFCMessage.ChatroomInfo chatroomInfo) {
        this.chatroomId = chatroomId;
        this.title = chatroomInfo.getTitle();
        this.desc = chatroomInfo.getDesc();
        this.portrait = chatroomInfo.getPortrait();
        this.memberCount = memberCount;
        this.createDt = chatroomInfo.getCreateDt();
        this.updateDt = chatroomInfo.getUpdateDt();
    }

    public OutputGetChatroomInfo() {
    }

    public int getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(int memberCount) {
        this.memberCount = memberCount;
    }

    public long getCreateDt() {
        return createDt;
    }

    public void setCreateDt(long createDt) {
        this.createDt = createDt;
    }

    public long getUpdateDt() {
        return updateDt;
    }

    public void setUpdateDt(long updateDt) {
        this.updateDt = updateDt;
    }

    public String getChatroomId() {
        return chatroomId;
    }

    public void setChatroomId(String chatroomId) {
        this.chatroomId = chatroomId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getPortrait() {
        return portrait;
    }

    public void setPortrait(String portrait) {
        this.portrait = portrait;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}
