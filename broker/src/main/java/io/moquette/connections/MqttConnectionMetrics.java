
package io.moquette.connections;

/**
 * A class that represents the metrics of a given MQTT connection.
 *
 * @author lbarrios
 *
 */
public class MqttConnectionMetrics {

    private long readKb;
    private long writtenKb;
    private long readMessages;
    private long writtenMessages;

    public MqttConnectionMetrics(long readBytes, long writtenBytes, long readMessages, long writtenMessages) {
        this.readKb = readBytes / 1024;
        this.writtenKb = writtenBytes / 1024;
        this.readMessages = readMessages;
        this.writtenMessages = writtenMessages;
    }

    public long getReadKb() {
        return readKb;
    }

    public long getWrittenKb() {
        return writtenKb;
    }

    public long getReadMessages() {
        return readMessages;
    }

    public long getWrittenMessages() {
        return writtenMessages;
    }

}
