/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;

import java.util.List;

public class InputGetUserGroupByType {
    private String userId;
    private List<Integer> groupMemberTypes;

    public InputGetUserGroupByType() {
    }

    public InputGetUserGroupByType(String userId, List<Integer> groupMemberTypes) {
        this.userId = userId;
        this.groupMemberTypes = groupMemberTypes;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<Integer> getGroupMemberTypes() {
        return groupMemberTypes;
    }

    public void setGroupMemberTypes(List<Integer> groupMemberTypes) {
        this.groupMemberTypes = groupMemberTypes;
    }
}
