/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package win.liyufan.im;

import cn.wildfirechat.common.ErrorCode;
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
    public interface HttpCallback {
        void onSuccess(String content);
        void onFailure(int statusCode, String errorMessage);
    }

    public enum HttpPostType {
        POST_TYPE_Push,
        POST_TYPE_Grout_Event_Callback,
        POST_TYPE_Grout_Member_Event_Callback,
        POST_TYPE_User_Relation_Event_Callback,
        POST_TYPE_User_Info_Event_Callback,
        POST_TYPE_Channel_Info_Event_Callback,
        POST_TYPE_Channel_Subscriber_Event_Callback,
        POST_TYPE_Chatroom_Info_Event_Callback,
        POST_TYPE_Chatroom_Member_Event_Callback,
        POST_TYPE_Robot_Message_Callback,
        POST_TYPE_Channel_Message_Callback,
        POST_TYPE_Forward_Message_Callback,
        POST_TYPE_User_Online_Event_Callback,
        POST_TYPE_Server_Exception_Callback;
    }

    public static void httpJsonPost(String url, String jsonStr, HttpPostType postType) {
        httpJsonPost(url, jsonStr, null, postType);
    }

    public static void httpJsonPost(String url, String jsonStr, HttpCallback httpCallback, HttpPostType postType) {
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
                String logText;
                if(postType == HttpPostType.POST_TYPE_Push) {
                    logText = "Http请求到推送服务失败，请检查推送服务是否正在运行或者配置文件中推送服务地址是否正确。";
                } else if(postType == HttpPostType.POST_TYPE_Robot_Message_Callback) {
                    logText = "Http请求转发消息到机器人服务失败，请检查机器人服务是否部署且在机器人信息中的回调地址是否正确。\n如果不需要机器人及机器人服务，请关掉应用服务中自动添加机器人好友和发送机器人欢迎语的代码，用户不给机器人发送消息就不会有转发请求了。";
                } else if(postType == HttpPostType.POST_TYPE_Channel_Message_Callback ||
                    postType == HttpPostType.POST_TYPE_Channel_Subscriber_Event_Callback) {
                    logText = "Http请求到频道服务失败，请检查频道服务是否部署且在频道信息中的回调地址是否正确。";
                } else {
                    logText = "Http请求回调地址失败，请检查IM服务配置文件中对应的回调地址是否正确";
                }
                System.out.println(logText);
                e.printStackTrace();
                LOG.info(logText);
                LOG.info("POST to {} with data {} failure", url, jsonStr);
                if(httpCallback != null) {
                    httpCallback.onFailure(-1, e.getMessage());
                }
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                LOG.info("POST to {} success with response: {}", url, response.body());
                try {
                    if(httpCallback != null) {
                        int code = response.code();
                        if(code == 200) {
                            if(response.body() != null && response.body().contentLength() > 0) {
                                httpCallback.onSuccess(response.body().string());
                            } else {
                                httpCallback.onSuccess(null);
                            }
                        } else {
                            httpCallback.onFailure(code, response.message());
                        }
                    }
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
