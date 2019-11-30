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
import cn.wildfirechat.pojos.InputOutputUserInfo;
import cn.wildfirechat.pojos.OutputCreateUser;
import cn.wildfirechat.pojos.OutputGetIMTokenData;
import cn.wildfirechat.proto.WFCMessage;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.RestResult;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import io.moquette.persistence.RPCCenter;
import io.moquette.persistence.TargetEntry;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.internal.StringUtil;
import win.liyufan.im.IMTopic;
import win.liyufan.im.UUIDGenerator;

import java.util.Base64;
import java.util.concurrent.Executor;

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
                RPCCenter.getInstance().sendRequest(inputDestroyUser.getUserId(), null, IMTopic.DestroyUserTopic, idBuf.toByteArray(), inputDestroyUser.getUserId(), TargetEntry.Type.TARGET_TYPE_USER, new RPCCenter.Callback() {
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

                            sendResponse(response, null, null);
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
            } else {
                response.setStatus(HttpResponseStatus.OK);
                RestResult result = RestResult.resultOf(ErrorCode.INVALID_PARAMETER);
                response.setContent(new Gson().toJson(result));
            }

        }
        return true;
    }
}
