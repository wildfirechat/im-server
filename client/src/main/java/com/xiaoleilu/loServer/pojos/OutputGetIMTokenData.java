package cn.wildfirechat.pojos;

public class OutputGetIMTokenData {
    private String userId;
    private String token;

    public OutputGetIMTokenData() {
    }

    public OutputGetIMTokenData(String userId, String imToken) {
        this.userId = userId;
        this.token = imToken;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
