/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;

public class InputSetChatroomBlacklist {
    private String chatroomId;
    private String userId;
    private int status;
    private long expiredTime;

    public InputSetChatroomBlacklist() {
    }

    public InputSetChatroomBlacklist(String chatroomId, String userId, int status, long expiredTime) {
        this.chatroomId = chatroomId;
        this.userId = userId;
        this.status = status;
        this.expiredTime = expiredTime;
    }

    public String getChatroomId() {
        return chatroomId;
    }

    public void setChatroomId(String chatroomId) {
        this.chatroomId = chatroomId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getExpiredTime() {
        return expiredTime;
    }

    public void setExpiredTime(long expiredTime) {
        this.expiredTime = expiredTime;
    }
}
