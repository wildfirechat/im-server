/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;

import java.util.List;

/**
 * 群组搜索结果
 */
public class PojoSearchGroupRes {
    /** 搜索关键词 */
    public String keyword;
    /** 匹配的群组列表 */
    public List<PojoGroupInfo> groupInfos;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public List<PojoGroupInfo> getGroupInfos() {
        return groupInfos;
    }

    public void setGroupInfos(List<PojoGroupInfo> groupInfos) {
        this.groupInfos = groupInfos;
    }
}
