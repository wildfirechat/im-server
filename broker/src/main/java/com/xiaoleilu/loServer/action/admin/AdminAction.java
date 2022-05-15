/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.admin;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.proto.ProtoConstants;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.action.Action;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.moquette.persistence.ServerAPIHelper;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import org.apache.commons.codec.digest.DigestUtils;
import cn.wildfirechat.common.ErrorCode;
import org.slf4j.LoggerFactory;
import win.liyufan.im.RateLimiter;
import win.liyufan.im.Utility;

import java.util.concurrent.Executor;

abstract public class AdminAction extends Action {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(AdminAction.class);
    private static String SECRET_KEY = "123456";
    private static boolean NO_CHECK_TIME = false;
    public static void setSecretKey(String secretKey) {
        SECRET_KEY = secretKey;
    }

    public static String getSecretKey() {
        return SECRET_KEY;
    }

    public static void setNoCheckTime(String noCheckTime) {
        try {
            NO_CHECK_TIME = Boolean.parseBoolean(noCheckTime);
        } catch (Exception e) {

        }
    }

    @Override
    public ErrorCode preAction(Request request, Response response) {
        if (!adminLimiter.isGranted("admin")) {
            return ErrorCode.ERROR_CODE_OVER_FREQUENCY;
        }

        if(APIPath.Health.equals(request.getUri())) {
            return ErrorCode.ERROR_CODE_SUCCESS;
        }

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
        
        if (StringUtil.isNullOrEmpty(nonce) || StringUtil.isNullOrEmpty(timestamp) || StringUtil.isNullOrEmpty(sign)) {
            return ErrorCode.ERROR_CODE_API_NOT_SIGNED;
        }

        Long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (Exception e) {
            e.printStackTrace();
            Utility.printExecption(LOG, e);
            return ErrorCode.ERROR_CODE_API_NOT_SIGNED;
        }

        if (!NO_CHECK_TIME && System.currentTimeMillis() - ts > 2 * 60 * 60 * 1000) {
            return ErrorCode.ERROR_CODE_SIGN_EXPIRED;
        }

        String str = nonce + "|" + SECRET_KEY + "|" + timestamp;
        String localSign = DigestUtils.sha1Hex(str);
        return localSign.equals(sign) ? ErrorCode.ERROR_CODE_SUCCESS : ErrorCode.ERROR_CODE_AUTH_FAILURE;
    }

    protected void sendResponse(Response response, ErrorCode errorCode, Object data) {
        if(response != null) {
            response.setStatus(HttpResponseStatus.OK);
            if (errorCode == null) {
                errorCode = ErrorCode.ERROR_CODE_SUCCESS;
            }

            RestResult result = RestResult.resultOf(errorCode, errorCode.getMsg(), data);
            response.setContent(new Gson().toJson(result));
            response.send();
        }
    }
    protected void sendApiMessage(Response response, String fromUser, String topic, byte[] message, ApiCallback callback) {
        sendApiMessage(response, fromUser, null, topic, message, callback, false);
    }

    protected void sendApiMessage(Response response, String fromUser, String topic, byte[] message, ApiCallback callback, boolean noAdmin) {
        sendApiMessage(response, fromUser, null, topic, message, callback, noAdmin);
    }

    protected void sendApiMessage(Response response, String fromUser, String clientId, String topic, byte[] message, ApiCallback callback, boolean noAdmin) {
        ServerAPIHelper.sendRequest(fromUser, clientId, topic, message, new ServerAPIHelper.Callback() {
            @Override
            public void onSuccess(byte[] result) {
                if(callback != null) {
                    Result r = callback.onResult(result);
                    sendResponse(response, r.getErrorCode(), r.getData());
                }
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
        }, noAdmin ? ProtoConstants.RequestSourceType.Request_From_User : ProtoConstants.RequestSourceType.Request_From_Admin);
    }
}
