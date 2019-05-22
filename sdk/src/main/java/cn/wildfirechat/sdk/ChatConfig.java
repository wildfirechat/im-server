package cn.wildfirechat.sdk;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.*;

public class ChatConfig {
    public static void init(String url, String secret) {
        HttpUtils.init(url, secret);
    }
}
