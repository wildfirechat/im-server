/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.robot;

import cn.wildfirechat.common.IMExceptionEvent;
import cn.wildfirechat.proto.ProtoConstants;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.action.Action;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.moquette.persistence.ServerAPIHelper;
import io.moquette.spi.impl.Utils;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import org.apache.commons.codec.digest.DigestUtils;
import cn.wildfirechat.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.liyufan.im.RateLimiter;
import win.liyufan.im.Utility;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.Executor;

abstract public class RobotAction extends Action {
    protected static final Logger LOG = LoggerFactory.getLogger(RobotAction.class);

    protected WFCMessage.Robot robot;

    protected interface Callback {
        void onSuccess(byte[] response);
    }

    @Override
    public ErrorCode preAction(Request request, Response response) {
        String nonce = request.getHeader("nonce");
        if (StringUtil.isNullOrEmpty(nonce)) {
            nonce = request.getHeader("Nonce");
        }
        String timestamp = request.getHeader("timestamp");
        if (StringUtil.isNullOrEmpty(timestamp)) {
            timestamp = request.getHeader("Timestamp");
        }
        String sign = request.getHeader("sign");
        if (StringUtil.isNullOrEmpty(sign)) {
            sign = request.getHeader("Sign");
        }
        String rid = request.getHeader("rid");
        if (StringUtil.isNullOrEmpty(rid)) {
            rid = request.getHeader("Rid");
        }

        if (StringUtil.isNullOrEmpty(nonce) || StringUtil.isNullOrEmpty(timestamp) || StringUtil.isNullOrEmpty(sign) || StringUtil.isNullOrEmpty(rid)) {
            return ErrorCode.ERROR_CODE_API_NOT_SIGNED;
        }

        if (!robotLimiter.isGranted(rid)) {
            return ErrorCode.ERROR_CODE_OVER_FREQUENCY;
        }

        Long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (Exception e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e, IMExceptionEvent.EventType.ROBOT_API_Exception);
            return ErrorCode.ERROR_CODE_API_NOT_SIGNED;
        }

        if (System.currentTimeMillis() - ts > 2 * 60 * 60 * 1000) {
            return ErrorCode.ERROR_CODE_SIGN_EXPIRED;
        }

        robot = messagesStore.getRobot(rid);
        if (robot == null) {
            return ErrorCode.ERROR_CODE_NOT_EXIST;
        }

        if (StringUtil.isNullOrEmpty(robot.getSecret())) {
            return ErrorCode.ERROR_CODE_NOT_RIGHT;
        }

        String str = nonce + "|" + robot.getSecret() + "|" + timestamp;
        String localSign = DigestUtils.sha1Hex(str);
        return localSign.equals(sign) ? ErrorCode.ERROR_CODE_SUCCESS : ErrorCode.ERROR_CODE_AUTH_FAILURE;
    }

    protected void sendResponse(Response response, ErrorCode errorCode, Object data) {
        response.setStatus(HttpResponseStatus.OK);
        if (errorCode == null) {
            errorCode = ErrorCode.ERROR_CODE_SUCCESS;
        }

        RestResult result = RestResult.resultOf(errorCode, errorCode.getMsg(), data);
        response.setContent(new Gson().toJson(result));
        response.send();
    }

    protected <T> T getRequestBody(HttpRequest request, Class<T> cls) {
        if (request instanceof FullHttpRequest) {
            FullHttpRequest fullHttpRequest = (FullHttpRequest) request;
            byte[] bytes = Utils.readBytesAndRewind(fullHttpRequest.content());
            String content = null;
            try {
                content = new String(bytes, "UTF-8");
                Gson gson = new Gson();
                T t = gson.fromJson(content, cls);
                return t;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                Utility.printExecption(LOG, e, IMExceptionEvent.EventType.ROBOT_API_Exception);
            }
        }
        return null;
    }

    protected void sendApiRequest(Response response, String topic, byte[] message, Callback callback) {
        ServerAPIHelper.sendRequest(robot.getUid(), null, topic, message, new ServerAPIHelper.Callback() {
            @Override
            public void onSuccess(byte[] response) {
                callback.onSuccess(response);
            }

            @Override
            public void onError(ErrorCode errorCode) {
                sendResponse(response, errorCode, null);
            }

            @Override
            public void onTimeout() {
                sendResponse(response, ErrorCode.ERROR_CODE_TIMEOUT, null);
            }

            @Override
            public Executor getResponseExecutor() {
                return command -> {
                    ctx.executor().execute(command);
                };
            }
        }, ProtoConstants.RequestSourceType.Request_From_Robot);
    }
}
