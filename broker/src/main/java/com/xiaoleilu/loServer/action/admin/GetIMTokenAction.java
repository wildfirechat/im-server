/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.admin;

import cn.wildfirechat.proto.WFCMessage;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import com.xiaoleilu.loServer.pojos.InputGetToken;
import com.xiaoleilu.loServer.pojos.OutputGetIMTokenData;
import io.moquette.persistence.RPCCenter;
import io.moquette.persistence.TargetEntry;
import io.netty.handler.codec.http.FullHttpRequest;
import win.liyufan.im.ErrorCode;
import win.liyufan.im.IMTopic;

import java.util.Base64;
import java.util.concurrent.Executor;

@Route("/admin/user/token")
@HttpMethod("POST")
public class GetIMTokenAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return false;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputGetToken input = getRequestBody(request.getNettyRequest(), InputGetToken.class);
            String userId = input.getUserId();


            WFCMessage.GetTokenRequest getTokenRequest = WFCMessage.GetTokenRequest.newBuilder().setUserId(userId).setClientId(input.getClientId()).build();
            RPCCenter.getInstance().sendRequest(userId, input.getClientId(), IMTopic.GetTokenTopic, getTokenRequest.toByteArray(), userId, TargetEntry.Type.TARGET_TYPE_USER, new RPCCenter.Callback() {
                @Override
                public void onSuccess(byte[] result) {
                    ErrorCode errorCode1 = ErrorCode.fromCode(result[0]);
                    if (errorCode1 == ErrorCode.ERROR_CODE_SUCCESS) {
                        //ba errorcode qudiao
                        byte[] data = new byte[result.length -1];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = result[i+1];
                        }
                        String token = Base64.getEncoder().encodeToString(data);

                        sendResponse(response, null, new OutputGetIMTokenData(userId, token));
                    } else {
                        sendResponse(response, errorCode1, null);
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
            }, true);
            return false;
        }
        return true;
    }
}
