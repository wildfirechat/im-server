/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package win.liyufan.im;

import io.netty.util.internal.StringUtil;
import okhttp3.*;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.params.CoreConnectionPNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;


public class HttpUtils {
    private static final Logger LOG = LoggerFactory.getLogger(HttpUtils.class);
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    static private OkHttpClient client = new OkHttpClient();

    public static void httpJsonPost(final String url, final String jsonStr){
        //消息推送内容为 {"sender":"uCGUxUaa","senderName":"杨","convType":0,"target":"usq7v7UU","targetName":"鞋子","userId":"usq7v7UU","line":0,"cntType":400,"serverTime":1610590766485,"pushMessageType":1,"pushType":2,"pushContent":"","pushData":"","unReceivedMsg":1,"mentionedType":0,"packageName":"cn.wildfirechat.chat","deviceToken":"AFoieP9P6u6CccIkRK23gRwUJWKqSkdiqnb-6gC1kL7Wv-9XNoEYBPU7VsINU_q8_WTKfafe35qWu7ya7Z-NmgOTX9XVW3A3zd6ilh--quj6ccINXRvVnh8QmI9QQ","isHiddenDetail":false,"language":"zh"}
        //推送信息只打印前100个字符，防止敏感信息打印到日志中去。
        LOG.info("POST to {} with data {}...", url, jsonStr.substring(0, Math.min(jsonStr.length(), 100)));
        if (StringUtil.isNullOrEmpty(url)) {
            LOG.error("http post failure with empty url");
            return;
        }

        RequestBody body = RequestBody.create(JSON, jsonStr);
        Request request = new Request.Builder()
            .url(url)
            .post(body)
            .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                LOG.info("POST to {} with data {} failure", url, jsonStr);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                LOG.info("POST to {} success with response: {}", url, response.body());
                try {
                    if (response.body() != null) {
                        response.body().close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
