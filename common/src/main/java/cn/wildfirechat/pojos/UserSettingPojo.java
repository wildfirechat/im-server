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

public class UserSettingPojo {
    private String userId;
    private int scope;
    private String key;
    private String value;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getScope() {
        return scope;
    }

    public void setScope(int scope) {
        this.scope = scope;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    public WFCMessage.ModifyUserSettingReq toProtoRequest() {
        WFCMessage.ModifyUserSettingReq.Builder builder = WFCMessage.ModifyUserSettingReq.newBuilder();
        builder.setScope(scope);
        if (!StringUtil.isNullOrEmpty(key))
            builder.setKey(key);
        if(!StringUtil.isNullOrEmpty(value))
            builder.setValue(value);

        return builder.build();
    }
}
