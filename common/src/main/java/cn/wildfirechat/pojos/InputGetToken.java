package cn.wildfirechat.pojos;

public class InputGetToken {
    private String userId;
    private String clientId;
    private Integer platform;

    public InputGetToken() {
    }

    public InputGetToken(String userId, String clientId, int platform) {
        this.userId = userId;
        this.clientId = clientId;
        this.platform = platform;
    }

    public Integer getPlatform() {
        return platform;
    }

    public void setPlatform(Integer platform) {
        this.platform = platform;
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
