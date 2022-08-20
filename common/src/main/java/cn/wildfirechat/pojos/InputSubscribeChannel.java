/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;

public class InputSubscribeChannel {
    private String channelId;
    private String userId;
    private int subscribe; //1，订阅；0，取消订阅

    public InputSubscribeChannel() {
    }

    public InputSubscribeChannel(String channelId, String userId, int subscribe) {
        this.channelId = channelId;
        this.userId = userId;
        this.subscribe = subscribe;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getSubscribe() {
        return subscribe;
    }

    public void setSubscribe(int subscribe) {
        this.subscribe = subscribe;
    }
}
