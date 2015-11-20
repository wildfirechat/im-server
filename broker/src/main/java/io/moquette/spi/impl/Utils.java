package io.moquette.spi.impl;

import java.util.Map;

/**
 * Utility static methods, like Map get with default value, or elvis operator.
 */
public class Utils {
    public static <T, K> T defaultGet(Map<K, T> map, K key, T defaultValue) {
        T value = map.get(key);
        if (value != null) {
            return value;
        }
        return defaultValue;
    }
}
