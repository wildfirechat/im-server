package cn.wildfirechat.sdk;

import cn.wildfirechat.sdk.utilities.AdminHttpUtils;
import cn.wildfirechat.sdk.utilities.ChannelHttpUtils;
import cn.wildfirechat.sdk.utilities.RobotHttpUtils;

public class ChatConfig {
    public static void initAdmin(String url, String secret) {
        AdminHttpUtils.init(url, secret);
    }

    public static void initRobot(String url, String robotId, String secret) {
        RobotHttpUtils.init(url, robotId, secret);
    }
}
