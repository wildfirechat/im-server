package cn.wildfirechat.sdk.utilities;

import cn.wildfirechat.sdk.model.IMResult;
import com.google.gson.Gson;
import ikidou.reflect.TypeBuilder;

public class JsonUtils {

    public static <T> IMResult<T> fromJsonObject(String content, Class<T> clazz) {
        TypeBuilder builder = TypeBuilder.newInstance(IMResult.class);
        if (!clazz.equals(Void.class)) {
            builder.addTypeParam(clazz);
        }
        return new Gson().fromJson(content, builder.build());
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }
}
