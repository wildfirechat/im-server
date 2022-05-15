package cn.wildfirechat.pojos;


import cn.wildfirechat.proto.WFCMessage;

public class OutputRobot {
    private String userId;
    private String name;
    private String password;
    private String displayName;
    private String portrait;
    private int gender;
    private String mobile;
    private String email;
    private String address;
    private String company;
    private String social;
    private String extra;
    private long updateDt;

    private String owner;
    private String secret;
    private String callback;
    private String robotExtra;

    public String getSocial() {
        return social;
    }

    public void setSocial(String social) {
        this.social = social;
    }

    public long getUpdateDt() {
        return updateDt;
    }

    public void setUpdateDt(long updateDt) {
        this.updateDt = updateDt;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPortrait() {
        return portrait;
    }

    public void setPortrait(String portrait) {
        this.portrait = portrait;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getCallback() {
        return callback;
    }

    public void setCallback(String callback) {
        this.callback = callback;
    }

    public String getRobotExtra() {
        return robotExtra;
    }

    public void setRobotExtra(String robotExtra) {
        this.robotExtra = robotExtra;
    }

    public void fromUser(WFCMessage.User user) {
        userId = user.getUid();
        name = user.getName();
        displayName = user.getDisplayName();
        portrait = user.getPortrait();
        gender = user.getGender();
        mobile = user.getMobile();
        email = user.getEmail();
        address = user.getAddress();
        company = user.getCompany();
        social = user.getSocial();
        extra = user.getExtra();
        updateDt = user.getUpdateDt();
    }
    public void fromRobot(WFCMessage.Robot robot, boolean withSecret) {
        setOwner(robot.getOwner());
        if(withSecret) {
            setSecret(robot.getSecret());
        }
        setCallback(robot.getCallback());
        setRobotExtra(robot.getExtra());
    }
}
