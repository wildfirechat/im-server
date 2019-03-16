/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.model;

import java.io.Serializable;

public class FriendData implements Serializable {
    private String userId;
    private String friendUid;
    private int state;
    private long timestamp;


    public FriendData(String userId, String friendUid, int state, long timestamp) {
        this.userId = userId;
        this.friendUid = friendUid;
        this.state = state;
        this.timestamp = timestamp;
    }

    public FriendData() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFriendUid() {
        return friendUid;
    }

    public void setFriendUid(String friendUid) {
        this.friendUid = friendUid;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
