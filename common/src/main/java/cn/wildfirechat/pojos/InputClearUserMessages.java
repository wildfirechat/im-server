/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;

public class InputClearUserMessages {
    public InputClearUserMessages() {
    }

    public InputClearUserMessages(String userId, Conversation conversation, long startTime, long endTime) {
        this.userId = userId;
        this.conversation = conversation;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String userId;
    public Conversation conversation;
    public long startTime;
    public long endTime;
}
