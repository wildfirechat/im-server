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
import cn.wildfirechat.pojos.InputGetUserInfo;
import com.xiaoleilu.loServer.handler.Response;
import io.moquette.persistence.ServerAPIHelper;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.StringUtil;
import cn.wildfirechat.common.ErrorCode;

@Route(APIPath.User_Get_Online_Status)
@HttpMethod("POST")
public class CheckUserOnlineAction extends AdminAction {

    @Override
    public boolean isTransactionAction() {
        return false;
    }

    @Override
    public boolean action(Request request, Response response) {
        if (request.getNettyRequest() instanceof FullHttpRequest) {
            InputGetUserInfo inputUserId = getRequestBody(request.getNettyRequest(), InputGetUserInfo.class);
            if (inputUserId == null || !StringUtil.isNullOrEmpty(inputUserId.getUserId())) {
                sendApiMessage(response, inputUserId.getUserId(), ServerAPIHelper.CHECK_USER_ONLINE_REQUEST, inputUserId.getUserId().getBytes(), res -> {
                    OutputCheckUserOnline out = new Gson().fromJson(new String(res), OutputCheckUserOnline.class);
                    return new Result(ErrorCode.ERROR_CODE_SUCCESS, out);
                });
                return false;
            } else {
                sendResponse(response, ErrorCode.INVALID_PARAMETER, null);
            }
        }
        return true;
    }
}
