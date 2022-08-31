package win.liyufan.im;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonUtil {
    public static final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
}
