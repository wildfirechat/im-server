package org.dna.mqtt.moquette.proto.messages;

/**
 * The attributes Qos, Dup and Retain aren't used.
 * 
 * @author andrea
 */
public class ConnAckMessage extends AbstractMessage {
    public static final byte CONNECTION_ACCEPTED = 0x00;
    public static final byte UNNACEPTABLE_PROTOCOL_VERSION = 0x01;
    public static final byte IDENTIFIER_REJECTED = 0x02;
    public static final byte SERVER_UNAVAILABLE = 0x03;
    public static final byte BAD_USERNAME_OR_PASSWORD = 0x04;
    public static final byte NOT_AUTHORIZED = 0x05;
    
    private byte m_returnCode;

    public byte getReturnCode() {
        return m_returnCode;
    }

    public void setReturnCode(byte returnCode) {
        this.m_returnCode = returnCode;
    }
    
}
