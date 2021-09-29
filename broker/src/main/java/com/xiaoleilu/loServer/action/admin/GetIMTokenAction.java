/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.admin;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.proto.WFCMessage;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import cn.wildfirechat.pojos.InputGetToken;
import cn.wildfirechat.pojos.OutputGetIMTokenData;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import cn.wildfirechat.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import win.liyufan.im.IMTopic;

import java.util.Base64;

@Route(APIPath.User_Get_Token)
@HttpMethod("POST")
public class GetIMTokenAction extends AdminAction {
    private static final Logger LOG = LoggerFactory.getLogger(GetIMTokenAction.class);
    @Override
    public boolean isTransactionAction() {
        return false;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputGetToken input = getRequestBody(request.getNettyRequest(), InputGetToken.class);
            String userId = input.getUserId();


            WFCMessage.GetTokenRequest getTokenRequest = WFCMessage.GetTokenRequest.newBuilder().setUserId(userId).setClientId(input.getClientId()).setPlatform(input.getPlatform() == null ? 0 : input.getPlatform()).build();
            sendApiMessage(response, userId, input.getClientId(), IMTopic.GetTokenTopic, getTokenRequest.toByteArray(), result -> {
                ErrorCode errorCode1 = ErrorCode.fromCode(result[0]);
                if (errorCode1 == ErrorCode.ERROR_CODE_SUCCESS) {
                    //ba errorcode qudiao
                    byte[] data = new byte[result.length -1];
                    for (int i = 0; i < data.length; i++) {
                        data[i] = result[i+1];
                    }
                    String token = Base64.getEncoder().encodeToString(data);

                    LOG.info("get im token success {},{},{}", userId, input.getClientId(), token.substring(0, Math.min(10, token.length())));
                    
                    return new Result(errorCode1, new OutputGetIMTokenData(userId, token));
                } else {
                    return new Result(errorCode1);
                }
            }, false);
            return false;
        }
        return true;
    }
}
