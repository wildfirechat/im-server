/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package io.moquette.persistence;

import com.hazelcast.core.MapStore;
import io.moquette.server.Server;

import java.util.Collection;
import java.util.Map;

public class UserStatusLoader implements MapStore<String, Integer> {
    @Override
    public void store(String s, Integer status) {
        getDatabaseStore().updateUserStatus(s, status);
    }

    @Override
    public void storeAll(Map<String, Integer> map) {

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
    public Integer load(String key) {
        return getDatabaseStore().getUserStatus(key);
    }

    @Override
    public Map<String, Integer> loadAll(Collection<String> keys) {
        return null;
    }

    @Override
    public Iterable<String> loadAllKeys() {
        return null;
    }
}
