package com.xiaoleilu.loServer.pojos;

public class InputGetToken {
    private String userId;
    private String clientId;

    public InputGetToken(String userId, String name) {
        this.userId = userId;
        this.clientId = name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
}
