package cn.wildfirechat.pojos;

public class InputGetToken {
    private String userId;
    private String clientId;

    public InputGetToken(String userId, String clientId) {
        this.userId = userId;
        this.clientId = clientId;
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
