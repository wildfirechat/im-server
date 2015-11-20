package io.moquette.interception.messages;

import io.moquette.proto.messages.ConnectMessage;

/**
 * @author Wagner Macedo
 */
public class InterceptConnectMessage extends InterceptAbstractMessage {
    private final ConnectMessage msg;

    public InterceptConnectMessage(ConnectMessage msg) {
        super(msg);
        this.msg = msg;
    }

    public String getClientID() {
        return msg.getClientID();
    }

    public boolean isCleanSession() {
        return msg.isCleanSession();
    }

    public int getKeepAlive() {
        return msg.getKeepAlive();
    }

    public boolean isPasswordFlag() {
        return msg.isPasswordFlag();
    }

    public byte getProtocolVersion() {
        return msg.getProtocolVersion();
    }

    public String getProtocolName() {
        return msg.getProtocolName();
    }

    public boolean isUserFlag() {
        return msg.isUserFlag();
    }

    public boolean isWillFlag() {
        return msg.isWillFlag();
    }

    public byte getWillQos() {
        return msg.getWillQos();
    }

    public boolean isWillRetain() {
        return msg.isWillRetain();
    }

    public String getUsername() {
        return msg.getUsername();
    }

    public byte[] getPassword() {
        return msg.getPassword();
    }

    public String getWillTopic() {
        return msg.getWillTopic();
    }

    public byte[] getWillMessage() {
        return msg.getWillMessage();
    }
}
