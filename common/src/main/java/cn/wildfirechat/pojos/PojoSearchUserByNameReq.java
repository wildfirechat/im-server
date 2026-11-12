/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;

/**
 * 用户名称搜索请求
 * <p>按用户名或昵称进行模糊搜索，支持分页。</p>
 */
public class PojoSearchUserByNameReq {
    /** 搜索关键词，匹配用户名或昵称 */
    public String keyword;
    /** 页码，从0开始 */
    public int page;
    /** 每页数量，为0时服务端使用默认值20，最大100 */
    public int count;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
