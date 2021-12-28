/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.channel;

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
import org.slf4j.LoggerFactory;
import win.liyufan.im.RateLimiter;
import win.liyufan.im.Utility;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

abstract public class ChannelAction extends Action {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(ChannelAction.class);
    protected WFCMessage.ChannelInfo channelInfo;

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
        String cid = request.getHeader("cid");
        if (StringUtil.isNullOrEmpty(cid)) {
            cid = request.getHeader("Cid");
        }

        if (StringUtil.isNullOrEmpty(nonce) || StringUtil.isNullOrEmpty(timestamp) || StringUtil.isNullOrEmpty(sign) || StringUtil.isNullOrEmpty(cid)) {
            return ErrorCode.ERROR_CODE_API_NOT_SIGNED;
        }

        if (!channelLimiter.isGranted(cid)) {
            return ErrorCode.ERROR_CODE_OVER_FREQUENCY;
        }

        Long ts;
        try {
            ts = Long.parseLong(timestamp);
        } catch (Exception e) {
            LOG.error("Channel timestamp:{} invalid", timestamp);
            e.printStackTrace();
            Utility.printExecption(LOG, e, IMExceptionEvent.EventType.CHANNEL_API_Exception);
            return ErrorCode.ERROR_CODE_API_NOT_SIGNED;
        }

        if (System.currentTimeMillis() - ts > 2 * 60 * 60 * 1000) {
            return ErrorCode.ERROR_CODE_SIGN_EXPIRED;
        }

        channelInfo = messagesStore.getChannelInfo(cid);
        if (channelInfo == null) {
            return ErrorCode.ERROR_CODE_CHANNEL_NO_EXIST;
        }
        
        if (StringUtil.isNullOrEmpty(channelInfo.getSecret())) {
            return ErrorCode.ERROR_CODE_CHANNEL_NO_SECRET;
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
            String content = new String(bytes, StandardCharsets.UTF_8);
            Gson gson = new Gson();
            T t = gson.fromJson(content, cls);
            return t;
        }
        return null;
    }

    protected void sendApiMessage(Response response, String topic, byte[] message, Callback callback) {
        sendApiMessage(response, channelInfo.getOwner(), topic, message, callback);
    }
    protected void sendApiMessage(Response response, String user, String topic, byte[] message, Callback callback) {
        ServerAPIHelper.sendRequest(user, null, topic, message, new ServerAPIHelper.Callback() {
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
        }, ProtoConstants.RequestSourceType.Request_From_Channel);
    }
}
