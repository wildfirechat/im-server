package cn.wildfirechat.sdk;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.*;
import cn.wildfirechat.sdk.model.IMResult;
import cn.wildfirechat.sdk.utilities.AdminHttpUtils;

import java.util.List;

public class SensitiveAdmin {
    public static IMResult<Void> addSensitives(List<String> sensitives) throws Exception {
        String path = APIPath.Sensitive_Add;
        InputOutputSensitiveWords input = new InputOutputSensitiveWords();
        input.setWords(sensitives);
        return AdminHttpUtils.httpJsonPost(path, input, Void.class);
    }

    public static IMResult<Void> removeSensitives(List<String> sensitives) throws Exception {
        String path = APIPath.Sensitive_Del;
        InputOutputSensitiveWords input = new InputOutputSensitiveWords();
        input.setWords(sensitives);
        return AdminHttpUtils.httpJsonPost(path, input, Void.class);
    }

    public static IMResult<InputOutputSensitiveWords> getSensitives() throws Exception {
        String path = APIPath.Sensitive_Query;
        return AdminHttpUtils.httpJsonPost(path, null, InputOutputSensitiveWords.class);
    }

}
