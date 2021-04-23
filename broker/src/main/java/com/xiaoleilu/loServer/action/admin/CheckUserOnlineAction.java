/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.admin;

import cn.wildfirechat.common.APIPath;
import cn.wildfirechat.pojos.OutputCheckUserOnline;
import com.google.gson.Gson;
import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import cn.wildfirechat.pojos.InputGetUserInfo;
import io.moquette.persistence.ServerAPIHelper;
import io.moquette.persistence.TargetEntry;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.StringUtil;
import cn.wildfirechat.common.ErrorCode;

import java.util.concurrent.Executor;

@Route(APIPath.User_Get_Online_Status)
@HttpMethod("POST")
public class CheckUserOnlineAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return false;
    }

    @Override
    public boolean action(Request request) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputGetUserInfo inputUserId = getRequestBody(request.getNettyRequest(), InputGetUserInfo.class);
            if (inputUserId == null || !StringUtil.isNullOrEmpty(inputUserId.getUserId())) {
                ServerAPIHelper.sendRequest(null, null, ServerAPIHelper.CHECK_USER_ONLINE_REQUEST, inputUserId.getUserId().getBytes(), inputUserId.getUserId(), TargetEntry.Type.TARGET_TYPE_USER, new ServerAPIHelper.Callback() {
                    @Override
                    public void onSuccess(byte[] res) {
                        OutputCheckUserOnline out = new Gson().fromJson(new String(res), OutputCheckUserOnline.class);
                        sendResponse(response, null, out);
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
                sendResponse(response, ErrorCode.INVALID_PARAMETER, null);
            }
        }
        return true;
    }
}
