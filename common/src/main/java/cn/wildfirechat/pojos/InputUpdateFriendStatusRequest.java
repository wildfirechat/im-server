/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;


public class InputUpdateFriendStatusRequest {
    private String userId;
    private String friendUid;
    private int status;
    private String extra;


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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }
}
