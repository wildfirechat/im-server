/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;

public class InputGetConvReadTime {
    private String userId;
    private int conversationType;
    private String target;
    private int line;

    public InputGetConvReadTime() {
    }

    public InputGetConvReadTime(String userId, int conversationType, String target, int line) {
        this.userId = userId;
        this.conversationType = conversationType;
        this.target = target;
        this.line = line;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getConversationType() {
        return conversationType;
    }

    public void setConversationType(int conversationType) {
        this.conversationType = conversationType;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }
}
