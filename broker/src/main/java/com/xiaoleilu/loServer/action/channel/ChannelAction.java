/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.channel;

import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.action.Action;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.moquette.persistence.RPCCenter;
import io.moquette.spi.impl.Utils;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import org.apache.commons.codec.digest.DigestUtils;
import win.liyufan.im.ErrorCode;
import win.liyufan.im.RateLimiter;

abstract public class ChannelAction extends Action {
    private final RateLimiter mLimitCounter = new RateLimiter(10, 200);
    protected WFCMessage.ChannelInfo channelInfo;

    @Override
    public ErrorCode preAction(Request request, Response response) {
        String nonce = request.getHeader("nonce");
        String timestamp = request.getHeader("timestamp");
        String sign = request.getHeader("sign");
        String cid = request.getHeader("cid");

        if (StringUtil.isNullOrEmpty(nonce) || StringUtil.isNullOrEmpty(timestamp) || StringUtil.isNullOrEmpty(sign) || StringUtil.isNullOrEmpty(cid)) {
            return ErrorCode.INVALID_PARAMETER;
        }

        if (!mLimitCounter.isGranted(cid)) {
            return ErrorCode.ERROR_CODE_OVER_FREQUENCY;
        }

        Long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return ErrorCode.INVALID_PARAMETER;
        }

        if (System.currentTimeMillis() - ts > 2 * 60 * 60 * 1000) {
            return ErrorCode.ERROR_CODE_SIGN_EXPIRED;
        }

        channelInfo = messagesStore.getChannelInfo(cid);
        if (channelInfo == null) {
            return ErrorCode.INVALID_PARAMETER;
        }

        String str = nonce + "|" + channelInfo.getSecret() + "|" + timestamp;
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
            String content = new String(bytes);
            Gson gson = new Gson();
            T t = gson.fromJson(content, cls);
            return t;
        }
        return null;
    }
}
