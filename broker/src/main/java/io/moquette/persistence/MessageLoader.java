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
import win.liyufan.im.MessageBundle;

import java.util.Collection;
import java.util.Map;

public class MessageLoader implements MapStore<Long, MessageBundle> {
    private DatabaseStore getDatabaseStore() {
        return Server.getServer().getStore().messagesStore().getDatabaseStore();
    }
    /**
     * Loads the value of a given key. If distributed map doesn't contain the value
     * for the given key then Hazelcast will call implementation's load (key) method
     * to obtain the value. Implementation can use any means of loading the given key;
     * such as an O/R mapping tool, simple SQL or reading a file etc.
     *
     * @param key@return value of the key, value cannot be null
     */
    @Override
    public MessageBundle load(Long key) {
        return getDatabaseStore().getMessage(key);
    }

    /**
     * Loads given keys. This is batch load operation so that implementation can
     * optimize the multiple loads.
     * <p>
     * For any key in the input keys, there should be a single mapping in the resulting map. Also the resulting
     * map should not have any keys that are not part of the input keys.
     * <p>
     * The given collection should not contain any <code>null</code> keys.
     * The returned Map should not contain any <code>null</code> keys or values.
     *
     * @param keys keys of the values entries to load
     * @return map of loaded key-value pairs.
     */
    @Override
    public Map<Long, MessageBundle> loadAll(Collection<Long> keys) {
        return getDatabaseStore().getMessages(keys);
    }

    /**
     * Loads all of the keys from the store. The returned {@link Iterable} may return the keys lazily
     * by loading them in batches. The {@link Iterator} of this {@link Iterable} may implement the
     * {@link Closeable} interface in which case it will be closed once iteration is over.
     * This is intended for releasing resources such as closing a JDBC result set.
     * <p>
     * The returned Iterable should not contain any <code>null</code> keys.
     *
     * @return all the keys. Keys inside the Iterable cannot be null.
     */
    @Override
    public Iterable<Long> loadAllKeys() {
        return null;
    }

    @Override
    public void store(Long aLong, MessageBundle messageBundle) {
        getDatabaseStore().persistMessage(messageBundle.getMessage(), false);
    }

    @Override
    public void storeAll(Map<Long, MessageBundle> map) {

    }

    @Override
    public void delete(Long aLong) {

    }

    @Override
    public void deleteAll(Collection<Long> collection) {

    }
}
