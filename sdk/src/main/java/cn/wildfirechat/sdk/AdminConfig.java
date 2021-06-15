package cn.wildfirechat.sdk;

import cn.wildfirechat.sdk.utilities.AdminHttpUtils;

public class AdminConfig {
    public static void initAdmin(String url, String secret) {
        AdminHttpUtils.init(url, secret);
    }
}
