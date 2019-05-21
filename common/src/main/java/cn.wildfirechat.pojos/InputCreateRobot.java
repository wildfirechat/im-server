/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package cn.wildfirechat.pojos;


import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import io.netty.util.internal.StringUtil;

public class InputCreateRobot {
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

    public WFCMessage.Robot toRobot() {
        WFCMessage.Robot.Builder builder = WFCMessage.Robot.newBuilder();
        if (!StringUtil.isNullOrEmpty(userId))
            builder.setUid(userId);
        if (!StringUtil.isNullOrEmpty(owner))
        builder.setOwner(owner);
        if (!StringUtil.isNullOrEmpty(secret))
        builder.setSecret(secret);
        if (!StringUtil.isNullOrEmpty(callback))
        builder.setCallback(callback);
        if (!StringUtil.isNullOrEmpty(robotExtra))
        builder.setExtra(robotExtra);
        builder.setState(0);
        return builder.build();
    }

    public WFCMessage.User toUser() {
        WFCMessage.User.Builder newUserBuilder = WFCMessage.User.newBuilder()
            .setUid(userId);
        if (name != null)
            newUserBuilder.setName(name);
        if (displayName != null)
            newUserBuilder.setDisplayName(displayName);
        if (getPortrait() != null)
            newUserBuilder.setPortrait(getPortrait());
        if (getEmail() != null)
            newUserBuilder.setEmail(getEmail());
        if (getAddress() != null)
            newUserBuilder.setAddress(getAddress());
        if (getCompany() != null)
            newUserBuilder.setCompany(getCompany());
        if (getSocial() != null)
            newUserBuilder.setSocial(getSocial());

        if (getMobile() != null)
            newUserBuilder.setMobile(getMobile());
        if (getExtra() != null)
            newUserBuilder.setExtra(getExtra());
        newUserBuilder.setType(ProtoConstants.UserType.UserType_Robot);
        newUserBuilder.setGender(gender);

        newUserBuilder.setUpdateDt(System.currentTimeMillis());
        return newUserBuilder.build();
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
}
