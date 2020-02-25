package win.liyufan.im;

import io.netty.util.internal.StringUtil;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class I18n {
    private static ClassLoader Loader = null;
    static {
        String configPath = System.getProperty("wildfirechat.path", null);
        String resBundlePaht = "config/i18n";

        File file = new File(configPath, resBundlePaht);
        URL[] urls = new URL[0];
        try {
            urls = new URL[]{file.toURI().toURL()};
            Loader = new URLClassLoader(urls);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, ResourceBundle> bundleMap = new ConcurrentHashMap<>();

    public static String getString(String language, String key) {
        ResourceBundle bundle = bundleMap.get(language);

        if (StringUtil.isNullOrEmpty(language)) language = "zh_CN";

        if (bundle == null) {
            bundle = ResourceBundle.getBundle("messages", new Locale(language), Loader);
            bundleMap.put(language, bundle);
        }

        try {
            return bundle.getString(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "糟糕，字符串 " + key + " 没有找到";
    }

    public static void test() throws MalformedURLException {

        // 设置定制的语言国家代码
        Locale locale1 = new Locale("zh");
        Locale locale2 = new Locale("en");
        ResourceBundle rb = ResourceBundle.getBundle("messages", locale1, Loader);



        // 获得相应的key值
        String greeting = rb.getString("Above_Greeting_Message");
        String userInfo = rb.getString("Friend_Can_Start_Chat");

        System.out.println(greeting);
        System.out.println(userInfo);
    }

    public static void main(String[] args) throws Exception {
        test();
    }
}
