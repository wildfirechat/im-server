package cn.wildfirechat.sdk.utilities;

import cn.wildfirechat.sdk.model.IMResult;
import com.google.gson.Gson;
import ikidou.reflect.TypeBuilder;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;


public class AdminHttpUtils {
    private static final Logger LOG = LoggerFactory.getLogger(AdminHttpUtils.class);

    private static String adminUrl;
    private static String adminSecret;

    public static void init(String url, String secret) {
        adminUrl = url;
        adminSecret = secret;
    }

    public static <T> IMResult<T> httpJsonPost(String path, Object object, Class<T> clazz) throws Exception{
        if (isNullOrEmpty(adminUrl) || isNullOrEmpty(adminSecret)) {
            LOG.error("野火IM Server SDK必须先初始化才能使用，是不是忘记初始化了！！！！");
            throw new Exception("SDK没有初始化");
        }

        if (isNullOrEmpty(path)) {
            throw new Exception("路径缺失");
        }

        String url = adminUrl + path;
        HttpPost post = null;
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();

            int nonce = (int)(Math.random() * 100000 + 3);
            long timestamp = System.currentTimeMillis();
            String str = nonce + "|" + adminSecret + "|" + timestamp;
            String sign = DigestUtils.sha1Hex(str);


            post = new HttpPost(url);
            post.setHeader("Content-type", "application/json; charset=utf-8");
            post.setHeader("Connection", "Keep-Alive");
            post.setHeader("nonce", nonce + "");
            post.setHeader("timestamp", "" + timestamp);
            post.setHeader("sign", sign);

            String jsonStr = "";
            if (object != null) {
                jsonStr = new Gson().toJson(object);
            }
            LOG.info("http request content: {}", jsonStr);

            StringEntity entity = new StringEntity(jsonStr, Charset.forName("UTF-8"));
            entity.setContentEncoding("UTF-8");
            entity.setContentType("application/json");
            post.setEntity(entity);
            HttpResponse response = httpClient.execute(post);

            int statusCode = response.getStatusLine().getStatusCode();
            String content = null;
            if (response.getEntity().getContentLength() > 0) {
                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "utf-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                String NL = System.getProperty("line.separator");
                while ((line = in.readLine()) != null) {
                    sb.append(line).append(NL);
                }

                in.close();

                content = sb.toString();
                LOG.info("http request response content: {}", content);
            }

            if(statusCode != HttpStatus.SC_OK){
                LOG.info("Request error: " + statusCode + " error msg:" + content);
                throw new Exception("Http request error with code:" + statusCode);
            } else {
                return fromJsonObject(content, clazz);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            if(post != null) {
                post.releaseConnection();
            }
        }
    }

    private static <T> IMResult<T> fromJsonObject(String content, Class<T> clazz) {
        Type type = TypeBuilder
                .newInstance(IMResult.class)
                .addTypeParam(clazz)
                .build();
        return new Gson().fromJson(content, type);
    }

    private static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

}
