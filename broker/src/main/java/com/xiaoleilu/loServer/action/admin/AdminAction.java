/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.admin;

import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.action.Action;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import com.xiaoleilu.loServer.pojos.InputCreateUser;
import io.moquette.persistence.RPCCenter;
import io.moquette.spi.impl.Utils;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import org.apache.commons.codec.digest.DigestUtils;
import win.liyufan.im.ErrorCode;
import win.liyufan.im.RateLimiter;

abstract public class AdminAction extends Action {
    private static String SECRET_KEY = "123456";
    private final RateLimiter mLimitCounter = new RateLimiter(10, 500);
    public static void setSecretKey(String secretKey) {
        SECRET_KEY = secretKey;
    }

    @Override
    public ErrorCode preAction(Request request, Response response) {
        if (!mLimitCounter.isGranted("admin")) {
            return ErrorCode.ERROR_CODE_OVER_FREQUENCY;
        }
        String nonce = request.getHeader("nonce");
        String timestamp = request.getHeader("timestamp");
        String sign = request.getHeader("sign");
        if (StringUtil.isNullOrEmpty(nonce) || StringUtil.isNullOrEmpty(timestamp) || StringUtil.isNullOrEmpty(sign)) {
            return ErrorCode.INVALID_PARAMETER;
        }

        Long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return ErrorCode.INVALID_PARAMETER;
        }

        if (System.currentTimeMillis() - ts > 2 * 60 * 60 * 1000) {
//            return ErrorCode.ERROR_CODE_SIGN_EXPIRED;
        }

        String str = nonce + "|" + SECRET_KEY + "|" + timestamp;
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
}
