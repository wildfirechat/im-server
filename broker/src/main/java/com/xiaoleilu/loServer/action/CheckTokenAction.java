/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action;

import com.google.gson.Gson;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.moquette.persistence.MemorySessionStore;
import io.moquette.spi.security.Tokenor;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import win.liyufan.im.GitRepositoryState;
import win.liyufan.im.RateLimiter;
import win.liyufan.im.Utility;

import java.io.IOException;

@Route("/api/verify_token")
@HttpMethod("GET")
public class CheckTokenAction extends Action {
    private final RateLimiter mLimitCounter = new RateLimiter(10, 1);
    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            response.setStatus(HttpResponseStatus.OK);
            String userId = request.getParam("userId");
            String clientId = request.getParam("clientId");
            String token = request.getParam("token");

            String result = "这是个检查token有效性的接口，如果客户端无法连接成功，可以使用这个接口检查token是否正确。\n";
            result += "使用方法是在浏览器中输入http://imserverip/api/verify_token?userId=${userId}&clientId=${clientId}&token=${token}。\n";
            result += "例如：http://localhost/api/verify_token?userId=123&clientId=456&token=789。\n";
            result += "特别注意的是：必须使用正确的clientId，clientId必须是在测试手机上调用im接口获取。不用手机上获取到的clientId也都是不同的，一定不能用错！！！\n\n\n";

            if (StringUtil.isNullOrEmpty(userId)) {
                result += "错误，userId为空";
            } else if (StringUtil.isNullOrEmpty(clientId)) {
                result += "错误，clientId为空";
            } else if (StringUtil.isNullOrEmpty(token)) {
                result += "错误，token为空";
            } else if(!mLimitCounter.isGranted("verify_token")) {
                result += "接口请求超频！接口限制每10秒只能验证一次！";
            } else {
                MemorySessionStore.Session session = sessionsStore.getSession(clientId);
                if (session == null) {
                    result += "错误，session不存在。请确认token是从本服务通过getToken接口获取的，另外请确认clientId是否正确";
                } else if (session.getDeleted() == 1) {
                    result += "错误，session已经被清除，请确认当前客户没有多端登录或者主动退出";
                } else if(!session.getUsername().equals(userId)) {
                    result += "错误，当前客户端的登录用户不是" + userId + "。这一般发生在当前clientId又为其它用户获取过token";
                } else {
                    String id = Tokenor.getUserId(token.getBytes());
                    if (id == null) {
                        result += "错误，无效的token";
                    } else if(!id.equals(userId)) {
                        result += "错误，改token是用户" + id + "的";
                    } else {
                        result += "恭喜，您的信息是正确的";
                    }
                }
            }
            response.setContent(result);
        }
        return true;
    }
}
