/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package win.liyufan.im;

import io.netty.util.internal.StringUtil;
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
import java.io.InputStreamReader;
import java.nio.charset.Charset;


public class HttpUtils {
    private static final Logger LOG = LoggerFactory.getLogger(HttpUtils.class);

    public static boolean httpJsonPost(String url, String jsonStr){
        boolean isSuccess = false;

        LOG.info("POST to {} with data {}", url, jsonStr);
        if (StringUtil.isNullOrEmpty(url)) {
            LOG.error("http post failure with empty url");
            return false;
        }

        HttpPost post = null;
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();
//            // 设置超时时间
//            httpClient.getParams().setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 2000);
//            httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT, 2000);

            post = new HttpPost(url);
            // 构造消息头
            post.setHeader("Content-type", "application/json; charset=utf-8");
            post.setHeader("Connection", "Keep-Alive");

            // 构建消息实体
            StringEntity entity = new StringEntity(jsonStr, Charset.forName("UTF-8"));
            entity.setContentEncoding("UTF-8");
            // 发送Json格式的数据请求
            entity.setContentType("application/json");
            post.setEntity(entity);

            HttpResponse response = httpClient.execute(post);

            // 检验返回码
            int statusCode = response.getStatusLine().getStatusCode();
            if(statusCode != HttpStatus.SC_OK){
                LOG.info("请求出错: "+statusCode);
                isSuccess = false;
            }else{
                BufferedReader in = new BufferedReader(new InputStreamReader(response.getEntity()
                    .getContent(),"utf-8"));
                StringBuffer sb = new StringBuffer("");
                String line = "";
                String NL = System.getProperty("line.separator");
                while ((line = in.readLine()) != null) {
                    sb.append(line + NL);
                }

                in.close();

                String content = sb.toString();
                LOG.info("http request response content: {}", content);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
            isSuccess = false;
        }finally{
            if(post != null){
                post.releaseConnection();
            }
        }
        return isSuccess;
    }

}
