package cn.wildfirechat.sdk.model;

public class GetTokenRequest {
    private String userId;
    private String clientId;

    public GetTokenRequest(String userId, String clientId) {
        this.userId = userId;
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
