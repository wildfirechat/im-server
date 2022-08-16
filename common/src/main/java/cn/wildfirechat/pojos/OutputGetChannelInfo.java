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

import java.util.ArrayList;
import java.util.List;

public class OutputGetChannelInfo {
    public static class OutputMenu {
        public String menuId;
        public String type;
        public String name;
        public String key;
        public String url;
        public String mediaId;
        public String articleId;
        public String appId;
        public String appPage;
        public String extra;
        public List<OutputMenu> subMenus;

        public static OutputMenu fromPbInfo(WFCMessage.ChannelMenu channelMenuMenu) {
            OutputMenu out = new OutputMenu();
            out.menuId = channelMenuMenu.getMenuId();
            out.type = channelMenuMenu.getType();
            out.name = channelMenuMenu.getName();
            out.key = channelMenuMenu.getKey();
            out.url = channelMenuMenu.getUrl();
            out.mediaId = channelMenuMenu.getMediaId();
            out.articleId = channelMenuMenu.getArticleId();
            out.appId = channelMenuMenu.getAppId();
            out.appPage = channelMenuMenu.getAppPage();
            out.extra = channelMenuMenu.getExtra();
            if (channelMenuMenu.getSubMenuCount() > 0) {
                out.subMenus = new ArrayList<>();
                for (WFCMessage.ChannelMenu menuMenu : channelMenuMenu.getSubMenuList()) {
                    out.subMenus.add(fromPbInfo(menuMenu));
                }
            }
            return out;
        }

        public WFCMessage.ChannelMenu.Builder toPbInfo() {
            WFCMessage.ChannelMenu.Builder builder = WFCMessage.ChannelMenu.newBuilder();
            builder.setType(type);
            builder.setName(name);
            if (!StringUtil.isNullOrEmpty(menuId)) builder.setMenuId(menuId);
            if (!StringUtil.isNullOrEmpty(key)) builder.setKey(key);
            if (!StringUtil.isNullOrEmpty(url)) builder.setUrl(url);
            if (!StringUtil.isNullOrEmpty(mediaId)) builder.setMediaId(mediaId);
            if (!StringUtil.isNullOrEmpty(articleId)) builder.setArticleId(articleId);
            if (!StringUtil.isNullOrEmpty(appId)) builder.setAppId(appId);
            if (!StringUtil.isNullOrEmpty(appPage)) builder.setAppPage(appPage);
            if (!StringUtil.isNullOrEmpty(extra)) builder.setExtra(extra);
            if (subMenus != null && !subMenus.isEmpty()) {
                subMenus.forEach(menuMenu -> builder.addSubMenu(menuMenu.toPbInfo()));
            }
            return builder;
        }
    }

    public static class OutputMenuList extends ArrayList<OutputMenu> {

    }

    private String channelId;
    private String name;
    private String desc;
    private String portrait;
    private String extra;
    private String owner;
    private int state;
    private int status;
    private long updateDt;
    private String callback;
    private int automatic;
    private String secret;
    public List<OutputMenu> menus;

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public long getUpdateDt() {
        return updateDt;
    }

    public void setUpdateDt(long updateDt) {
        this.updateDt = updateDt;
    }

    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }

    public int getAutomatic() {
        return automatic;
    }

    public void setAutomatic(int automatic) {
        this.automatic = automatic;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public static OutputGetChannelInfo fromPbInfo(WFCMessage.ChannelInfo channelInfo) {
        OutputGetChannelInfo out = new OutputGetChannelInfo();
        out.automatic = channelInfo.getAutomatic();
        out.callback = channelInfo.getCallback();
        out.channelId = channelInfo.getTargetId();
        out.desc = channelInfo.getDesc();
        out.extra = channelInfo.getExtra();
        out.name = channelInfo.getName();
        out.owner = channelInfo.getOwner();
        out.portrait = channelInfo.getPortrait();
        out.state = channelInfo.getStatus();
        out.status = channelInfo.getStatus();;
        out.updateDt = channelInfo.getUpdateDt();
        out.secret = channelInfo.getSecret();
        if (channelInfo.getMenuCount() > 0) {
            out.menus = new ArrayList<>();
            for (WFCMessage.ChannelMenu channelMenuMenu : channelInfo.getMenuList()) {
                out.menus.add(OutputMenu.fromPbInfo(channelMenuMenu));
            }
        }
        return out;
    }
}

