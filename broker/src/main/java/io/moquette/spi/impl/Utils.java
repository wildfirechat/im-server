
package io.moquette.spi.impl;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
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

    public static int messageId(MqttMessage msg) {
        return ((MqttMessageIdVariableHeader) msg.variableHeader()).messageId();
    }

    public static byte[] readBytesAndRewind(ByteBuf payload) {
        byte[] payloadContent = new byte[payload.readableBytes()];
        int mark = payload.readerIndex();
        payload.readBytes(payloadContent);
        payload.readerIndex(mark);
        return payloadContent;
    }
}
