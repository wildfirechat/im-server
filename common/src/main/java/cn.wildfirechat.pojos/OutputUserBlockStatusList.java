/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;


import java.util.List;

public class OutputUserBlockStatusList {
    private List<InputOutputUserBlockStatus> statusList;

    public List<InputOutputUserBlockStatus> getStatusList() {
        return statusList;
    }

    public void setStatusList(List<InputOutputUserBlockStatus> statusList) {
        this.statusList = statusList;
    }
}
