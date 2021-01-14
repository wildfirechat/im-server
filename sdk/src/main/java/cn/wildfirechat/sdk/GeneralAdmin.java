package cn.wildfirechat.sdk;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.sdk.utilities.AdminHttpUtils;

public class GeneralAdmin {
    public static IMResult<SystemSettingPojo> getSystemSetting(int id) throws Exception {
        String path = APIPath.Get_System_Setting;
        SystemSettingPojo input = new SystemSettingPojo();
        input.id = id;
        return AdminHttpUtils.httpJsonPost(path, input, SystemSettingPojo.class);
    }

    public static IMResult<Void> setSystemSetting(int id, String value, String desc) throws Exception {
        String path = APIPath.Put_System_Setting;
        SystemSettingPojo input = new SystemSettingPojo();
        input.id = id;
        input.value = value;
        input.desc = desc;
        return AdminHttpUtils.httpJsonPost(path, input, Void.class);
    }

    public static IMResult<OutputCreateChannel> createChannel(InputCreateChannel inputCreateChannel) throws Exception {
        String path = APIPath.Create_Channel;
        return AdminHttpUtils.httpJsonPost(path, inputCreateChannel, OutputCreateChannel.class);
    }

    //以下仅专业版支持
    public static IMResult<Void> setConversationTop(String userId, int conversationType, String target, int line, boolean isTop) throws Exception {
        String key = conversationType + "-" + line + "-" + target;
        String value = isTop?"1":"0";
        return setUserSetting(userId, 3, key, value);
    }

    public static IMResult<Boolean> getConversationTop(String userId, int conversationType, String target, int line) throws Exception {
        String key = conversationType + "-" + line + "-" + target;
        IMResult<UserSettingPojo> result = getUserSetting(userId, 3, key);
        IMResult<Boolean> out = new IMResult<Boolean>();
        out.code = result.code;
        out.msg = result.msg;
        out.result = result.result != null && "1".equals(result.result.getValue());
        return out;
    }


    private static IMResult<UserSettingPojo> getUserSetting(String userId, int scope, String key) throws Exception {
        String path = APIPath.User_Get_Setting;
        UserSettingPojo pojo = new UserSettingPojo();
        pojo.setUserId(userId);
        pojo.setScope(scope);
        pojo.setKey(key);
        return AdminHttpUtils.httpJsonPost(path, pojo, UserSettingPojo.class);
    }

    private static IMResult<Void> setUserSetting(String userId, int scope, String key, String value) throws Exception {
        String path = APIPath.User_Put_Setting;
        UserSettingPojo pojo = new UserSettingPojo();
        pojo.setUserId(userId);
        pojo.setScope(scope);
        pojo.setKey(key);
        pojo.setValue(value);
        return AdminHttpUtils.httpJsonPost(path, pojo, Void.class);
    }
}
