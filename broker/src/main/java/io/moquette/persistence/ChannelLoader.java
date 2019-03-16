/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.persistence;

import cn.wildfirechat.proto.WFCMessage;
import com.hazelcast.core.MapStore;
import io.moquette.server.Server;

import java.util.Collection;
import java.util.Map;

public class ChannelLoader implements MapStore<String, WFCMessage.ChannelInfo> {
    @Override
    public void store(String s, WFCMessage.ChannelInfo channelInfo) {
        getDatabaseStore().updateChannelInfo(channelInfo);
    }

    @Override
    public void storeAll(Map<String, WFCMessage.ChannelInfo> map) {

    }

    @Override
    public void delete(String s) {
        getDatabaseStore().removeChannelInfo(s);
    }

    @Override
    public void deleteAll(Collection<String> collection) {

    }

    private DatabaseStore getDatabaseStore() {
        return Server.getServer().getStore().messagesStore().getDatabaseStore();
    }

    @Override
    public WFCMessage.ChannelInfo load(String key) {
        return getDatabaseStore().getPersistChannelInfo(key);
    }

    @Override
    public Map<String, WFCMessage.ChannelInfo> loadAll(Collection<String> keys) {
        return null;
    }

    @Override
    public Iterable<String> loadAllKeys() {
        return null;
    }
}
