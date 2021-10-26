/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action;

import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.RequireAuthentication;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.moquette.server.config.IConfig;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.impl.Utils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import cn.wildfirechat.common.ErrorCode;
import win.liyufan.im.RateLimiter;

import java.nio.charset.StandardCharsets;

import static cn.wildfirechat.common.ErrorCode.ERROR_CODE_SUCCESS;
import static io.moquette.BrokerConstants.*;

/**
 * 请求处理接口<br>
 * 当用户请求某个Path，则调用相应Action的doAction方法
 * @author Looly
 *
 */

abstract public class Action {
    public static IMessagesStore messagesStore = null;
    public static ISessionsStore sessionsStore = null;

    public ChannelHandlerContext ctx;

    protected static RateLimiter adminLimiter = null;
    protected static RateLimiter robotLimiter = null;
    protected static RateLimiter channelLimiter = null;

    public static void init(IConfig config) {
        int adminRate = 10000;
        int robotRate = 1000;
        int channelRate = 1000;

        try {
            adminRate = Integer.parseInt(config.getProperty(HTTP_ADMIN_RATE_LIMIT, "10000"));
        } catch (NumberFormatException e) {

        }
        try {
            robotRate = Integer.parseInt(config.getProperty(HTTP_ROBOT_RATE_LIMIT, "1000"));
        } catch (NumberFormatException e) {

        }
        try {
            channelRate = Integer.parseInt(config.getProperty(HTTP_CHANNEL_RATE_LIMIT, "1000"));
        } catch (NumberFormatException e) {

        }
        adminLimiter = new RateLimiter(10, adminRate);
        robotLimiter = new RateLimiter(10, robotRate);
        channelLimiter = new RateLimiter(10, channelRate);
    }

    protected class Result {
        Object data;
        ErrorCode errorCode;

        public Result(ErrorCode errorCode) {
            this.errorCode = errorCode;
        }

        public Result(ErrorCode errorCode, Object data) {
            this.data = data;
            this.errorCode = errorCode;
        }

        public Object getData() {
            return data;
        }

        public ErrorCode getErrorCode() {
            return errorCode;
        }
    }

    protected interface ApiCallback {
        Result onResult(byte[] response);
    }

    public ErrorCode preAction(Request request, Response response) {
        if (getClass().getAnnotation(RequireAuthentication.class) != null) {
            //do authentication
        }

        return ERROR_CODE_SUCCESS;
    }
	public boolean doAction(Request request, Response response) {
        ErrorCode errorCode = preAction(request, response);
        boolean isSync = true;
        if (errorCode == ErrorCode.ERROR_CODE_SUCCESS) {
            //事务逻辑有缺陷，先注释掉
//            if (isTransactionAction() && !(this instanceof IMAction)) {
//                DBUtil.beginTransaction();
//                try {
//                    action(request, response);
//                    DBUtil.commit();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    DBUtil.roolback();
//                    throw e;
//                }
//            } else {
            isSync = action(request, response);
//            }
        } else {
            response.setStatus(HttpResponseStatus.OK);
            if (errorCode == null) {
                errorCode = ErrorCode.ERROR_CODE_SUCCESS;
            }

            RestResult result = RestResult.resultOf(errorCode, errorCode.getMsg(), RestResult.resultOf(errorCode));
            response.setContent(new Gson().toJson(result));
            response.send();
        }

        return isSync;
    }
    public boolean isTransactionAction() {
        return false;
    }
    abstract public boolean action(Request request, Response response);

    protected <T> T getRequestBody(HttpRequest request, Class<T> cls) {
        if (request instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) request;
            byte[] bytes = Utils.readBytesAndRewind(fullHttpRequest.content());
            String content = new String(bytes, StandardCharsets.UTF_8);//contribute by JiaRG from github
            Gson gson = new Gson();
            T t = gson.fromJson(content, cls);
            return t;
        }
        return null;
    }

    protected void setResponseContent(RestResult result, Response response) {
        response.setStatus(HttpResponseStatus.OK);
        response.setContent(new Gson().toJson(result));
    }
}
