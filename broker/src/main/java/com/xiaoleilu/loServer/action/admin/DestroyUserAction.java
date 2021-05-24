/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.admin;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.common.ErrorCode;
import cn.wildfirechat.pojos.InputDestroyUser;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import win.liyufan.im.IMTopic;

import java.util.Base64;

@Route(APIPath.Destroy_User)
@HttpMethod("POST")
public class DestroyUserAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return true;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputDestroyUser inputDestroyUser = getRequestBody(request.getNettyRequest(), InputDestroyUser.class);
            if (inputDestroyUser != null
                && !StringUtil.isNullOrEmpty(inputDestroyUser.getUserId())) {

                WFCMessage.IDBuf idBuf = WFCMessage.IDBuf.newBuilder().setId(inputDestroyUser.getUserId()).build();
                sendApiMessage(response, inputDestroyUser.getUserId(), IMTopic.DestroyUserTopic, idBuf.toByteArray(), result -> {
                    ErrorCode errorCode1 = ErrorCode.fromCode(result[0]);
                    return new Result(errorCode1);
                });
                return false;
            } else {
                setResponseContent(RestResult.resultOf(ErrorCode.INVALID_PARAMETER), response);
            }

        }
        return true;
    }
}
