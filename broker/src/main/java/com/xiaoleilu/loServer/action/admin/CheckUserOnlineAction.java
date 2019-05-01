/*
 * This file is part of the Wildfire Chat package.
 * (c) Heavyrain2012 <heavyrain.lee@gmail.com>
 *
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 */

package com.xiaoleilu.loServer.action.admin;

import com.xiaoleilu.loServer.annotation.HttpMethod;
import com.xiaoleilu.loServer.annotation.Route;
import com.xiaoleilu.loServer.handler.Request;
import com.xiaoleilu.loServer.handler.Response;
import com.xiaoleilu.loServer.pojos.InputGetUserInfo;
import com.xiaoleilu.loServer.pojos.OutputCreateUser;
import io.moquette.persistence.RPCCenter;
import io.moquette.persistence.TargetEntry;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.util.internal.StringUtil;
import win.liyufan.im.ErrorCode;

@Route("/admin/user/onlinestatus")
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
                RPCCenter.getInstance().sendRequest(null, null, RPCCenter.CHECK_USER_ONLINE_REQUEST, inputUserId.getUserId().getBytes(), inputUserId.getUserId(), TargetEntry.Type.TARGET_TYPE_USER, null, true);
                return true;
            } else {
                sendResponse(response, ErrorCode.INVALID_PARAMETER, null);
            }

        }
        return true;
    }
}
