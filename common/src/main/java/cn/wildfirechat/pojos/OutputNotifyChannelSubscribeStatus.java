/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;

public class OutputNotifyChannelSubscribeStatus {
    private String userId;
    private String channelId;
    private int status;

    public OutputNotifyChannelSubscribeStatus() {
    }

    public OutputNotifyChannelSubscribeStatus(String userId, String channelId, boolean subscirbed) {
        this.userId = userId;
        this.channelId = channelId;
        this.status = subscirbed ? 1 : 0;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
