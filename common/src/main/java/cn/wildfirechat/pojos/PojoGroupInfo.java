/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;

public class PojoGroupInfo {
    String target_id;
    String name;
    String portrait;
    String owner;
    int type;
    String extra;
    int mute;
    int join_type;
    int private_chat;
    int searchable;
    int max_member_count;
    int history_message;

    public String getTarget_id() {
        return target_id;
    }

    public void setTarget_id(String target_id) {
        this.target_id = target_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPortrait() {
        return portrait;
    }

    public void setPortrait(String portrait) {
        this.portrait = portrait;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public int getMax_member_count() {
        return max_member_count;
    }

    public void setMax_member_count(int max_member_count) {
        this.max_member_count = max_member_count;
    }

    public int getMute() {
        return mute;
    }

    public void setMute(int mute) {
        this.mute = mute;
    }

    public int getJoin_type() {
        return join_type;
    }

    public void setJoin_type(int join_type) {
        this.join_type = join_type;
    }

    public int getPrivate_chat() {
        return private_chat;
    }

    public void setPrivate_chat(int private_chat) {
        this.private_chat = private_chat;
    }

    public int getSearchable() {
        return searchable;
    }

    public void setSearchable(int searchable) {
        this.searchable = searchable;
    }

    public int getHistory_message() {
        return history_message;
    }

    public void setHistory_message(int history_message) {
        this.history_message = history_message;
    }
}
