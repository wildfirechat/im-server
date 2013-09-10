package org.dna.mqtt.moquette.proto.messages;

/**
 * The attributes Qos, Dup and Retain aren't used for Connect message
 * 
 * @author andrea
 */
public class ConnectMessage extends AbstractMessage {
    String m_protocolName;
    byte m_procotolVersion;
    
    //Connection flags
    boolean m_cleanSession;
    boolean m_willFlag;
    byte m_willQos;
    boolean m_willRetain;
    boolean m_passwordFlag;
    boolean m_userFlag;
    int m_keepAlive;
    
    //Variable part
    String m_username;
    String m_password;
    String m_clientID;
    String m_willtopic;
    String m_willMessage;

    public boolean isCleanSession() {
        return m_cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.m_cleanSession = cleanSession;
    }

    public int getKeepAlive() {
        return m_keepAlive;
    }

    public void setKeepAlive(int keepAlive) {
        this.m_keepAlive = keepAlive;
    }

    public boolean isPasswordFlag() {
        return m_passwordFlag;
    }

    public void setPasswordFlag(boolean passwordFlag) {
        this.m_passwordFlag = passwordFlag;
    }

    public byte getProcotolVersion() {
        return m_procotolVersion;
    }

    public void setProcotolVersion(byte procotolVersion) {
        this.m_procotolVersion = procotolVersion;
    }

    public String getProtocolName() {
        return m_protocolName;
    }

    public void setProtocolName(String protocolName) {
        this.m_protocolName = protocolName;
    }

    public boolean isUserFlag() {
        return m_userFlag;
    }

    public void setUserFlag(boolean userFlag) {
        this.m_userFlag = userFlag;
    }

    public boolean isWillFlag() {
        return m_willFlag;
    }

    public void setWillFlag(boolean willFlag) {
        this.m_willFlag = willFlag;
    }

    public byte getWillQos() {
        return m_willQos;
    }

    public void setWillQos(byte willQos) {
        this.m_willQos = willQos;
    }

    public boolean isWillRetain() {
        return m_willRetain;
    }

    public void setWillRetain(boolean willRetain) {
        this.m_willRetain = willRetain;
    }

    public String getPassword() {
        return m_password;
    }

    public void setPassword(String password) {
        this.m_password = password;
    }

    public String getUsername() {
        return m_username;
    }

    public void setUsername(String username) {
        this.m_username = username;
    }

    public String getClientID() {
        return m_clientID;
    }

    public void setClientID(String clientID) {
        this.m_clientID = clientID;
    }

    public String getWillTopic() {
        return m_willtopic;
    }

    public void setWillTopic(String topic) {
        this.m_willtopic = topic;
    }

    public String getWillMessage() {
        return m_willMessage;
    }

    public void setWillMessage(String willMessage) {
        this.m_willMessage = willMessage;
    }
    
}
