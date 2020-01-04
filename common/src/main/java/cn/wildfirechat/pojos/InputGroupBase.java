/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;


import java.util.List;

public class InputGroupBase {
    public String operator;
    public List<Integer> to_lines;
    public MessagePayload  notify_message;

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public List<Integer> getTo_lines() {
        return to_lines;
    }

    public void setTo_lines(List<Integer> to_lines) {
        this.to_lines = to_lines;
    }

    public MessagePayload getNotify_message() {
        return notify_message;
    }

    public void setNotify_message(MessagePayload notify_message) {
        this.notify_message = notify_message;
    }
}
