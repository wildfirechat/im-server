/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.persistence;

import cn.wildfirechat.proto.WFCMessage;
import com.hazelcast.core.MapLoader;
import com.hazelcast.core.MapStore;
import io.moquette.server.Server;

import java.util.Collection;
import java.util.Map;

public class UserLoader implements MapStore<String, WFCMessage.User> {
    @Override
    public void store(String s, WFCMessage.User user) {
        getDatabaseStore().updateUser(user);
    }

    @Override
    public void storeAll(Map<String, WFCMessage.User> map) {

    }

    @Override
    public void delete(String s) {
        getDatabaseStore().deleteUser(s);
    }

    @Override
    public void deleteAll(Collection<String> collection) {

    }

    private DatabaseStore getDatabaseStore() {
        return Server.getServer().getStore().messagesStore().getDatabaseStore();
    }

    @Override
    public WFCMessage.User load(String key) {
        return getDatabaseStore().getPersistUser(key);
    }

    @Override
    public Map<String, WFCMessage.User> loadAll(Collection<String> keys) {
        return null;
    }

    @Override
    public Iterable<String> loadAllKeys() {
        return null;
    }
}
