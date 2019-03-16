/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.persistence;

public class TargetEntry {
    public TargetEntry(Type type, String target) {
        this.type = type;
        this.target = target;
    }

    public enum Type {
        TARGET_TYPE_USER,
        TARGET_TYPE_CHATROOM,
        TARGET_TYPE_MASTER_NODE,
    }
    public Type type;
    public String target;
}
