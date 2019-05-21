/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;

import java.util.List;

public class PojoGroup {
    private PojoGroupInfo group_info;
    private List<PojoGroupMember> members;

    public PojoGroupInfo getGroup_info() {
        return group_info;
    }

    public void setGroup_info(PojoGroupInfo group_info) {
        this.group_info = group_info;
    }

    public List<PojoGroupMember> getMembers() {
        return members;
    }

    public void setMembers(List<PojoGroupMember> members) {
        this.members = members;
    }
}
