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

public class InputCreateDevice {
    private String deviceId;
    private String owner;
    private String extra;

    public WFCMessage.Device toDevice() {
        WFCMessage.Device.Builder builder = WFCMessage.Device.newBuilder();
        if (!StringUtil.isNullOrEmpty(deviceId))
            builder.setUid(deviceId);
        if (!StringUtil.isNullOrEmpty(owner))
            builder.setOwner(owner);
        if (!StringUtil.isNullOrEmpty(extra))
            builder.setExtra(extra);
        builder.setState(0);
        return builder.build();
    }


    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
}
