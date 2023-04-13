/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;

public class OutputRecallMessageData {
    private OutputMessageData message;
    private String userId;
    private long timestamp;
    private boolean isAdmin;

    public OutputRecallMessageData() {
    }

    public OutputRecallMessageData(OutputMessageData message, String userId, long timestamp, boolean isAdmin) {
        this.message = message;
        this.userId = userId;
        this.timestamp = timestamp;
        this.isAdmin = isAdmin;
    }

    public OutputMessageData getMessage() {
        return message;
    }

    public void setMessage(OutputMessageData message) {
        this.message = message;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }
}
