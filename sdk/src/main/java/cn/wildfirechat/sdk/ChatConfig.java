package cn.wildfirechat.sdk;

import cn.wildfirechat.sdk.utilities.HttpUtils;

public class ChatConfig {
    public static void init(String url, String secret) {
        HttpUtils.init(url, secret);
    }
}
